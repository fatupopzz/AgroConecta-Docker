const jwt = require('jsonwebtoken');
const { installCommonJsMock } = require('./helpers/commonJsMocks');
const pool = { query: vi.fn(), connect: vi.fn() };
const notifyNearbyInstallations = vi.fn().mockResolvedValue({ sent: 0 });
installCommonJsMock(require.resolve('../src/config/db'), { pool });
installCommonJsMock(require.resolve('../src/services/PestAlertPushService'), { notifyNearbyInstallations });
const request = require('supertest');
const app = require('../app');

const diagnosis = { id_diagnostico: 1, id_usuario: 7, cultivo: 'Tomate', enfermedad: 'enfermedad-prueba', confianza: 0.91, foto: '/uploads/hoja.jpg', latitud: '14.6', longitud: '-90.5', id_alerta: null };
const payload = { cultivo: diagnosis.cultivo, enfermedad: diagnosis.enfermedad, confianza: diagnosis.confianza, foto: diagnosis.foto, latitud: 14.6, longitud: -90.5 };
let token;
let client;
beforeEach(() => {
    vi.clearAllMocks();
    pool.query.mockReset();
    token = jwt.sign({ id: 7, tipo: 'agricultor' }, process.env.JWT_SECRET);
    client = { query: vi.fn().mockResolvedValue({ rows: [] }), release: vi.fn() };
    pool.connect.mockResolvedValue(client);
});
const authorized = (method, url) => request(app)[method](url).set('Authorization', `Bearer ${token}`);

test.each([
    ['post', '/api/diagnoses'], ['get', '/api/diagnoses/mine'],
    ['get', '/api/diagnoses/1/products'], ['post', '/api/diagnoses/1/alert'],
])('%s %s exige token válido', async (method, url) => {
    expect((await request(app)[method](url)).status).toBe(401);
    expect((await request(app)[method](url).set('Authorization', 'Bearer invalid')).status).toBe(401);
    expect(pool.query).not.toHaveBeenCalled();
    expect(pool.connect).not.toHaveBeenCalled();
});

test('guarda el resultado de la app con propietario del token y fecha del servidor', async () => {
    pool.query.mockResolvedValue({ rows: [diagnosis] });
    const response = await authorized('post', '/api/diagnoses').send({ ...payload, id_usuario: 999, fecha: '1900-01-01' });
    expect(response.status).toBe(201);
    expect(response.body.id_diagnostico).toBe(1);
    expect(pool.query.mock.calls[0][1]).toEqual([7, 'Tomate', diagnosis.enfermedad, 0.91, payload.foto, 14.6, -90.5]);
});

test.each([
    { cultivo: '' }, { enfermedad: 3 }, { confianza: -1 }, { confianza: 1.1 },
    { confianza: '0.9' }, { foto: null }, { latitud: 91 }, { longitud: -181 },
    { latitud: null }, { latitud: '' }, { latitud: false },
])('rechaza diagnóstico inválido: %j', async (change) => {
    const response = await authorized('post', '/api/diagnoses').send({ ...payload, ...change });
    expect(response.status).toBe(400);
    expect(pool.query).not.toHaveBeenCalled();
});

test('permite diagnóstico sin GPS y confianza cero', async () => {
    pool.query.mockResolvedValue({ rows: [{ ...diagnosis, latitud: null, longitud: null }] });
    expect((await authorized('post', '/api/diagnoses').send({ ...payload, confianza: 0, latitud: null, longitud: null })).status).toBe(201);
});

test('historial filtra por usuario y ordena por fecha e id', async () => {
    pool.query.mockResolvedValue({ rows: [diagnosis] });
    const response = await authorized('get', '/api/diagnoses/mine');
    expect(response.status).toBe(200);
    expect(response.body).toEqual([diagnosis]);
    expect(pool.query).toHaveBeenCalledWith(expect.stringContaining('ORDER BY fecha DESC, id_diagnostico DESC'), [7]);
});

test.each(['abc', '0', '-1', '1.5', '2147483648'])('rechaza id %s', async (id) => {
    expect((await authorized('get', `/api/diagnoses/${id}/products`)).status).toBe(400);
    expect((await authorized('post', `/api/diagnoses/${id}/alert`)).status).toBe(400);
    expect(pool.query).not.toHaveBeenCalled();
    expect(pool.connect).not.toHaveBeenCalled();
});

test('oculta diagnósticos inexistentes o de otro agricultor', async () => {
    pool.query.mockResolvedValue({ rows: [] });
    expect((await authorized('get', '/api/diagnoses/2/products')).status).toBe(404);
    expect(pool.query.mock.calls[0][1]).toEqual([2, 7]);
});

test.each([false, true])('sin productos devuelve lista vacía y recomendación (mapeo=%s)', async (mapped) => {
    pool.query.mockResolvedValueOnce({ rows: [diagnosis] }).mockResolvedValueOnce({ rows: mapped ? [{ ingredientes_activos: ['ingrediente-prueba'] }] : [] }).mockResolvedValueOnce({ rows: [] });
    const response = await authorized('get', '/api/diagnoses/1/products');
    expect(response.status).toBe(200);
    expect(response.body.productos).toEqual([]);
    expect(response.body.recomendacion_general).toContain('No hay productos específicos');
});

test('busca productos activos por enfermedad, cultivo, categoría e ingrediente', async () => {
    const product = { id_producto: 10, composicion: 'ingrediente-prueba' };
    pool.query.mockResolvedValueOnce({ rows: [diagnosis] }).mockResolvedValueOnce({ rows: [{ ingredientes_activos: ['ingrediente-prueba'], recomendacion_general: 'Texto aprobado' }] }).mockResolvedValueOnce({ rows: [product] });
    const response = await authorized('get', '/api/diagnoses/1/products');
    expect(response.status).toBe(200);
    expect(response.body.productos).toEqual([product]);
    expect(pool.query.mock.calls[2][0]).toContain('t.id_categoria = p.id_categoria');
    expect(pool.query.mock.calls[2][0]).toContain('p.activo = TRUE');
    expect(pool.query.mock.calls[2][1]).toEqual([diagnosis.enfermedad, 'Tomate']);
});

function publication({ row = diagnosis, failUpdate = false } = {}) {
    const alert = { id_alerta: 29, tipo_plaga: row.enfermedad };
    client.query.mockImplementation(async (sql) => {
        if (sql.includes('SELECT * FROM diagnostico')) return { rows: row ? [row] : [] };
        if (sql.includes('INSERT INTO alerta_plaga') || sql.includes('SELECT * FROM alerta_plaga')) return { rows: [alert] };
        if (sql.includes('UPDATE diagnostico') && failUpdate) throw new Error('fallo de escritura');
        return { rows: [] };
    });
    return alert;
}

test('publica con HU-029, vincula y notifica después del commit', async () => {
    const alert = publication();
    const response = await authorized('post', '/api/diagnoses/1/alert').send({ severidad: 'alta' });
    expect(response.status).toBe(201);
    expect(response.body.alerta).toEqual(alert);
    expect(client.query).toHaveBeenCalledWith(expect.stringContaining('FOR UPDATE'), [1, 7]);
    expect(client.query).toHaveBeenCalledWith(expect.stringContaining('INSERT INTO alerta_plaga'), [7, diagnosis.enfermedad, 'Tomate', expect.any(String), '14.6', '-90.5', 'alta']);
    expect(client.query).toHaveBeenCalledWith('COMMIT');
    expect(notifyNearbyInstallations).toHaveBeenCalledWith({ alert, reporterUserId: 7 });
    expect(client.release).toHaveBeenCalledOnce();
});

test('republicar devuelve la misma alerta sin insertar ni notificar', async () => {
    publication({ row: { ...diagnosis, id_alerta: 29 } });
    expect((await authorized('post', '/api/diagnoses/1/alert')).status).toBe(200);
    expect(client.query.mock.calls.some(([sql]) => sql.includes('INSERT'))).toBe(false);
    expect(notifyNearbyInstallations).not.toHaveBeenCalled();
});

test('sin GPS rechaza publicación y revierte transacción', async () => {
    publication({ row: { ...diagnosis, latitud: null, longitud: null } });
    expect((await authorized('post', '/api/diagnoses/1/alert')).status).toBe(400);
    expect(client.query).toHaveBeenCalledWith('ROLLBACK');
    expect(client.release).toHaveBeenCalledOnce();
});

test('otro usuario no puede publicar el diagnóstico', async () => {
    client.query.mockResolvedValue({ rows: [] });
    expect((await authorized('post', '/api/diagnoses/1/alert')).status).toBe(404);
    expect(client.query).toHaveBeenCalledWith('ROLLBACK');
});

test('severidad inválida no abre transacción', async () => {
    expect((await authorized('post', '/api/diagnoses/1/alert').send({ severidad: 'desconocida' })).status).toBe(400);
    expect(pool.connect).not.toHaveBeenCalled();
});

test('fallo al vincular revierte alerta y no envía push', async () => {
    publication({ failUpdate: true });
    const log = vi.spyOn(console, 'error').mockImplementation(() => {});
    try {
        expect((await authorized('post', '/api/diagnoses/1/alert')).status).toBe(500);
        expect(client.query).toHaveBeenCalledWith('ROLLBACK');
        expect(client.query).not.toHaveBeenCalledWith('COMMIT');
        expect(client.release).toHaveBeenCalledOnce();
        expect(notifyNearbyInstallations).not.toHaveBeenCalled();
    } finally { log.mockRestore(); }
});
