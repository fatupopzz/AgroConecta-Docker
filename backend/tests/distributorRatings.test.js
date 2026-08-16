jest.mock("../src/config/db", () => ({
  pool: {
    query: jest.fn(),
  },
}));

jest.mock("jsonwebtoken", () => ({
  verify: jest.fn(),
}));

const request = require("supertest");
const jwt = require("jsonwebtoken");
const app = require("../app");
const { pool } = require("../src/config/db");

describe("GET /api/distribuidores", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jwt.verify.mockReturnValue({ id: 12, tipo: "agricultor" });
  });

  test("devuelve el promedio actual de resena_distribuidor como número", async () => {
    pool.query.mockResolvedValueOnce({
      rows: [
        {
          id_distribuidor: 1,
          nombre_negocio: "Agro Distribuciones SA",
          estado_verificacion: "verificado",
          calificacion_promedio: "0.00",
          promedio_resenas: "4.50",
          cantidad_resenas: 2,
        },
        {
          id_distribuidor: 3,
          nombre_negocio: "AgroInsumos Guatemala",
          estado_verificacion: "verificado",
          calificacion_promedio: "1.00",
          promedio_resenas: "4.00",
          cantidad_resenas: 1,
        },
      ],
    });

    const response = await request(app)
      .get("/api/distribuidores")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual([
      expect.objectContaining({
        id_distribuidor: 1,
        calificacion_promedio: 4.5,
        cantidad_resenas: 2,
      }),
      expect.objectContaining({
        id_distribuidor: 3,
        calificacion_promedio: 4,
        cantidad_resenas: 1,
      }),
    ]);

    const [sql] = pool.query.mock.calls[0];
    expect(sql).toMatch(/FROM resena_distribuidor/);
    expect(sql).toMatch(/AVG\(calificacion\)/);
    expect(sql).toMatch(/LEFT JOIN/);
  });

  test("normaliza a cero un distribuidor sin reseñas", async () => {
    pool.query.mockResolvedValueOnce({
      rows: [
        {
          id_distribuidor: 8,
          nombre_negocio: "Distribuidor sin reseñas",
          estado_verificacion: "verificado",
          calificacion_promedio: null,
          promedio_resenas: null,
          cantidad_resenas: null,
        },
      ],
    });

    const response = await request(app)
      .get("/api/distribuidores")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body[0]).toEqual(expect.objectContaining({
      calificacion_promedio: 0,
      cantidad_resenas: 0,
    }));
  });
});
