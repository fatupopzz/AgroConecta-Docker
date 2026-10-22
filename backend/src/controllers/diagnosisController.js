const { pool } = require('../config/db');
const AppError = require('../errors/AppError');
const { notifyNearbyInstallations } = require('../services/PestAlertPushService');

const GENERAL_RECOMMENDATION = 'No hay productos específicos disponibles. Confirme el diagnóstico con un técnico agrícola y aplique manejo integrado del cultivo antes de elegir un tratamiento.';
const text = (value, max) => typeof value === 'string' && value.trim().length > 0 && value.trim().length <= max;
const coordinate = (value, max) => typeof value === 'number' && Number.isFinite(value) && Math.abs(value) <= max;

function diagnosisId(req) {
    if (!/^[1-9]\d*$/.test(req.params.id) || !Number.isSafeInteger(Number(req.params.id)) || Number(req.params.id) > 2147483647) {
        throw new AppError(400, 'El id del diagnóstico no es válido');
    }
    return Number(req.params.id);
}

async function ownedDiagnosis(req, database = pool, lock = false) {
    const result = await database.query(
        `SELECT * FROM diagnostico WHERE id_diagnostico = $1 AND id_usuario = $2${lock ? ' FOR UPDATE' : ''}`,
        [diagnosisId(req), req.user.id],
    );
    if (!result.rows.length) throw new AppError(404, 'Diagnóstico no encontrado');
    return result.rows[0];
}

async function createDiagnosis(req, res) {
    const { cultivo, enfermedad, confianza, foto, latitud = null, longitud = null } = req.body || {};
    if (!text(cultivo, 100) || !text(enfermedad, 150) || !text(foto, 2048) ||
        typeof confianza !== 'number' || !Number.isFinite(confianza) || confianza < 0 || confianza > 1) {
        throw new AppError(400, 'Envíe cultivo, enfermedad, confianza entre 0 y 1 y foto (URL o ruta)');
    }
    if ((latitud !== null || longitud !== null) && (!coordinate(latitud, 90) || !coordinate(longitud, 180))) {
        throw new AppError(400, 'Envíe ambas coordenadas numéricas válidas');
    }
    const result = await pool.query(
        `INSERT INTO diagnostico (id_usuario, cultivo, enfermedad, confianza, foto, latitud, longitud)
         VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *`,
        [req.user.id, cultivo.trim(), enfermedad.trim(), confianza, foto.trim(), latitud, longitud],
    );
    return res.status(201).json(result.rows[0]);
}

async function getMyDiagnoses(req, res) {
    const result = await pool.query(
        'SELECT * FROM diagnostico WHERE id_usuario = $1 ORDER BY fecha DESC, id_diagnostico DESC',
        [req.user.id],
    );
    return res.json(result.rows);
}

async function getDiagnosisProducts(req, res) {
    const diagnosis = await ownedDiagnosis(req);
    const mappings = await pool.query(
        `SELECT * FROM enfermedad_tratamiento
         WHERE LOWER(enfermedad) = LOWER($1) AND LOWER(cultivo) = LOWER($2)
         ORDER BY id_tratamiento`,
        [diagnosis.enfermedad, diagnosis.cultivo],
    );
    let products = [];
    if (mappings.rows.length) {
        const result = await pool.query(
            `SELECT p.*, c.nombre AS categoria FROM producto p
             JOIN categoria c ON c.id_categoria = p.id_categoria
             WHERE p.activo = TRUE AND EXISTS (
                 SELECT 1 FROM enfermedad_tratamiento t
                 CROSS JOIN LATERAL unnest(t.ingredientes_activos) AS ingrediente(nombre)
                 WHERE LOWER(t.enfermedad) = LOWER($1) AND LOWER(t.cultivo) = LOWER($2)
                   AND t.id_categoria = p.id_categoria
                   AND BTRIM(ingrediente.nombre) <> ''
                   AND STRPOS(
                       ' ' || regexp_replace(LOWER(COALESCE(p.composicion, '')), '[^[:alnum:]]+', ' ', 'g') || ' ',
                       ' ' || regexp_replace(LOWER(BTRIM(ingrediente.nombre)), '[^[:alnum:]]+', ' ', 'g') || ' '
                   ) > 0
             ) ORDER BY p.calificacion_promedio DESC NULLS LAST, p.id_producto`,
            [diagnosis.enfermedad, diagnosis.cultivo],
        );
        products = result.rows;
    }
    return res.json({
        diagnostico_id: diagnosis.id_diagnostico,
        enfermedad: diagnosis.enfermedad,
        productos: products,
        ingredientes_activos: [...new Set(mappings.rows.flatMap((row) => row.ingredientes_activos))],
        recomendacion_general: products.length
            ? mappings.rows.map((row) => row.recomendacion_general).join(' ')
            : GENERAL_RECOMMENDATION,
    });
}

async function publishDiagnosisAlert(req, res) {
    diagnosisId(req);
    const { severidad = 'media' } = req.body || {};
    if (!['baja', 'media', 'alta', 'critica'].includes(severidad)) throw new AppError(400, 'Severidad inválida');
    const client = await pool.connect();
    let alert;
    let created = false;
    try {
        await client.query('BEGIN');
        // Serializa publicaciones simultáneas del mismo diagnóstico.
        const diagnosis = await ownedDiagnosis(req, client, true);
        if (diagnosis.id_alerta) {
            const existing = await client.query('SELECT * FROM alerta_plaga WHERE id_alerta = $1', [diagnosis.id_alerta]);
            alert = existing.rows[0];
        } else {
            if (diagnosis.latitud == null || diagnosis.longitud == null) throw new AppError(400, 'El diagnóstico necesita ubicación para publicar una alerta');
            const result = await client.query(
                `INSERT INTO alerta_plaga (id_usuario, tipo_plaga, cultivo_afectado, descripcion, latitud, longitud, severidad)
                 VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *`,
                [req.user.id, diagnosis.enfermedad, diagnosis.cultivo,
                    `Diagnóstico reportado por la app; confianza: ${diagnosis.confianza}.`,
                    diagnosis.latitud, diagnosis.longitud, severidad],
            );
            alert = result.rows[0];
            await client.query('UPDATE diagnostico SET id_alerta = $1 WHERE id_diagnostico = $2', [alert.id_alerta, diagnosis.id_diagnostico]);
            created = true;
        }
        await client.query('COMMIT');
    } catch (error) {
        await client.query('ROLLBACK');
        throw error;
    } finally {
        client.release();
    }
    if (created) {
        void notifyNearbyInstallations({ alert, reporterUserId: req.user.id }).catch((error) => {
            console.error('Error al notificar alerta de diagnóstico:', error);
        });
    }
    return res.status(created ? 201 : 200).json({ alerta: alert });
}

module.exports = { createDiagnosis, getMyDiagnoses, getDiagnosisProducts, publishDiagnosisAlert };
