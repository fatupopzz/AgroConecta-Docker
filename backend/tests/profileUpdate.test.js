const { installCommonJsMock } = require("./helpers/commonJsMocks");

const pool = { connect: vi.fn(), query: vi.fn() };
const jwt = { verify: vi.fn() };
const bcrypt = { hash: vi.fn(), compare: vi.fn() };

installCommonJsMock(require.resolve("../src/config/db"), { pool });
installCommonJsMock(require.resolve("jsonwebtoken"), jwt);
installCommonJsMock(require.resolve("bcrypt"), bcrypt);

const request = require("supertest");
const app = require("../app");

describe("PUT /api/auth/me", () => {
  let client;

  beforeEach(() => {
    vi.clearAllMocks();
    client = { query: vi.fn(), release: vi.fn() };
    pool.connect.mockResolvedValue(client);
  });

  test("actualiza únicamente datos personales del distribuidor", async () => {
    jwt.verify.mockReturnValue({ id: 12, tipo: "distribuidor" });
    client.query
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({
        rows: [{
          id_usuario: 12,
          nombre: "Juan",
          apellido: "Rivas",
          telefono: "55551234",
          email: "juan@example.com",
          tipo_usuario: "distribuidor",
        }],
      })
      .mockResolvedValueOnce({
        rows: [{
          id_distribuidor: 4,
          nombre_negocio: "Agro Juan",
          nit: "1234",
          departamento: "Guatemala",
          direccion: "Zona 1",
          estado_verificacion: "verificado",
          calificacion_promedio: "4.50",
        }],
      })
      .mockResolvedValueOnce({});

    const response = await request(app)
      .put("/api/auth/me")
      .set("Authorization", "Bearer token-valido")
      .send({
        nombre: "Juan",
        apellido: "Rivas",
        telefono: "55551234",
        email: "juan@example.com",
        departamento: "Guatemala",
        nombre_negocio: "Agro Juan",
        nit: "1234",
        direccion: "Zona 1",
      });

    expect(response.statusCode).toBe(200);
    expect(response.body.perfil.estado_verificacion).toBe("verificado");
    const distributorSql = client.query.mock.calls[2][0];
    const updateClause = distributorSql.match(/SET([\s\S]*?)WHERE/)[1];
    expect(updateClause).not.toMatch(/estado_verificacion/);
    expect(updateClause).not.toMatch(/calificacion_promedio/);
    expect(client.release).toHaveBeenCalled();
  });

  test("rechaza campos sensibles aunque pertenezcan al usuario", async () => {
    jwt.verify.mockReturnValue({ id: 12, tipo: "distribuidor" });

    const response = await request(app)
      .put("/api/auth/me")
      .set("Authorization", "Bearer token-valido")
      .send({
        nombre: "Juan",
        telefono: "55551234",
        estado_verificacion: "verificado",
      });

    expect(response.statusCode).toBe(400);
    expect(response.body.error).toMatch(/estado_verificacion/);
    expect(pool.connect).not.toHaveBeenCalled();
  });
});
