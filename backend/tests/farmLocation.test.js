const { installCommonJsMock } = require("./helpers/commonJsMocks");

const client = { query: vi.fn(), release: vi.fn() };
const pool = { query: vi.fn(), connect: vi.fn(async () => client) };
const jwt = { verify: vi.fn() };

installCommonJsMock(require.resolve("../src/config/db"), { pool });
installCommonJsMock(require.resolve("jsonwebtoken"), jwt);

const request = require("supertest");
const app = require("../app");

const auth = (req) => req.set("Authorization", "Bearer token-valido");

describe("ubicación de la finca", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    pool.connect.mockImplementation(async () => client);
    jwt.verify.mockReturnValue({ id: 12, tipo: "agricultor" });
  });

  test("devuelve la dirección predeterminada como ubicación de la finca", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_agricultor: 30 }] })
      .mockResolvedValueOnce({
        rows: [{
          departamento: "Chimaltenango",
          municipio: "Tecpán Guatemala",
          descripcion: "Aldea Xenimajuyú, frente a la escuela",
          latitud: "14.7612300",
          longitud: "-91.0012300",
        }],
      });

    const response = await auth(request(app).get("/api/agricultores/me/ubicacion"));

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual({
      departamento: "Chimaltenango",
      municipio: "Tecpán Guatemala",
      referencia: "Aldea Xenimajuyú, frente a la escuela",
      latitud: 14.76123,
      longitud: -91.00123,
    });
    const [sql, params] = pool.query.mock.calls[1];
    expect(sql).toMatch(/es_predeterminada = TRUE/);
    expect(params).toEqual([30]);
  });

  test("responde 404 si todavía no la guardó", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_agricultor: 30 }] })
      .mockResolvedValueOnce({ rows: [] });

    const response = await auth(request(app).get("/api/agricultores/me/ubicacion"));

    expect(response.statusCode).toBe(404);
  });

  test("un distribuidor no tiene ubicación de finca", async () => {
    pool.query.mockResolvedValueOnce({ rows: [] });

    const response = await auth(request(app).get("/api/agricultores/me/ubicacion"));

    expect(response.statusCode).toBe(403);
  });

  test("la primera vez crea la dirección predeterminada y sincroniza el perfil", async () => {
    const saved = {
      departamento: "Chimaltenango",
      municipio: "Tecpán Guatemala",
      descripcion: "Frente a la escuela",
      latitud: "14.7612300",
      longitud: "-91.0012300",
    };
    client.query
      .mockResolvedValueOnce({ rows: [{ id_agricultor: 30 }] }) // agricultor
      .mockResolvedValueOnce({ rows: [] }) // BEGIN
      .mockResolvedValueOnce({ rows: [] }) // UPDATE sin fila previa
      .mockResolvedValueOnce({ rows: [saved] }) // INSERT
      .mockResolvedValueOnce({ rows: [] }) // UPDATE agricultor
      .mockResolvedValueOnce({ rows: [] }); // COMMIT

    const response = await auth(request(app).put("/api/agricultores/me/ubicacion")).send({
      departamento: " Chimaltenango ",
      municipio: "Tecpán Guatemala",
      referencia: "Frente a la escuela",
      latitud: 14.76123,
      longitud: -91.00123,
    });

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual(expect.objectContaining({ municipio: "Tecpán Guatemala", latitud: 14.76123 }));
    const statements = client.query.mock.calls.map(([sql]) => sql.trim().split("\n")[0]);
    expect(statements).toContain("BEGIN");
    expect(statements).toContain("COMMIT");
    const insert = client.query.mock.calls.find(([sql]) => sql.includes("INSERT INTO direccion_entrega"));
    expect(insert[1]).toEqual([30, "Chimaltenango", "Tecpán Guatemala", "Frente a la escuela", 14.76123, -91.00123, "Mi finca"]);
    const profile = client.query.mock.calls.find(([sql]) => sql.startsWith("UPDATE agricultor"));
    expect(profile[1]).toEqual([30, "Chimaltenango", "Tecpán Guatemala"]);
    expect(client.release).toHaveBeenCalled();
  });

  test("si ya existe la actualiza sin crear otra", async () => {
    client.query
      .mockResolvedValueOnce({ rows: [{ id_agricultor: 30 }] })
      .mockResolvedValueOnce({ rows: [] })
      .mockResolvedValueOnce({
        rows: [{ departamento: "Sololá", municipio: "Panajachel", descripcion: null, latitud: null, longitud: null }],
      })
      .mockResolvedValueOnce({ rows: [] })
      .mockResolvedValueOnce({ rows: [] });

    const response = await auth(request(app).put("/api/agricultores/me/ubicacion")).send({
      departamento: "Sololá",
      municipio: "Panajachel",
    });

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual({
      departamento: "Sololá",
      municipio: "Panajachel",
      referencia: null,
      latitud: null,
      longitud: null,
    });
    expect(client.query.mock.calls.some(([sql]) => sql.includes("INSERT INTO direccion_entrega"))).toBe(false);
  });

  test("sin GPS se puede guardar solo el municipio", async () => {
    client.query.mockResolvedValue({ rows: [{ id_agricultor: 30, departamento: "Sololá", municipio: "Panajachel" }] });

    const response = await auth(request(app).put("/api/agricultores/me/ubicacion")).send({
      departamento: "Sololá",
      municipio: "Panajachel",
      referencia: "Cerca del muelle",
    });

    expect(response.statusCode).toBe(200);
  });

  test.each([
    [{ municipio: "Panajachel" }, /obligatorios/],
    [{ departamento: "Sololá", municipio: "Panajachel", latitud: 14.7 }, /juntas/],
    [{ departamento: "Sololá", municipio: "Panajachel", latitud: 95, longitud: -91 }, /inválidas/],
    [{ departamento: "Sololá", municipio: "Panajachel", referencia: "x".repeat(256) }, /255/],
  ])("rechaza datos inválidos sin tocar la base: %o", async (body, message) => {
    const response = await auth(request(app).put("/api/agricultores/me/ubicacion")).send(body);

    expect(response.statusCode).toBe(400);
    expect(response.body.error).toMatch(message);
    expect(pool.connect).not.toHaveBeenCalled();
  });
});
