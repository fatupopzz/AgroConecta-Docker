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

  test("devuelve el promedio de reseñas de productos del distribuidor como número", async () => {
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
    expect(sql).not.toMatch(/resena_distribuidor/);
    expect(sql).toMatch(/FROM inventario_distribuidor i/);
    expect(sql).toMatch(/JOIN resena r ON r\.id_producto = i\.id_producto/);
    expect(sql).toMatch(/AVG\(r\.calificacion\)/);
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

describe("coordenadas de distribuidores", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jwt.verify.mockReturnValue({ id: 12, tipo: "administrador" });
  });

  test("guarda coordenadas válidas al crear un distribuidor", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_usuario: 20 }] })
      .mockResolvedValueOnce({
        rows: [{
          id_distribuidor: 5,
          nombre_negocio: "Agro Norte",
          latitud: "14.7000000",
          longitud: "-90.5000000",
        }],
      });

    const response = await request(app)
      .post("/api/distribuidores")
      .set("Authorization", "Bearer token-valido")
      .send({
        id_usuario: 20,
        nombre_negocio: "Agro Norte",
        latitud: 14.7,
        longitud: -90.5,
      });

    expect(response.statusCode).toBe(201);
    expect(pool.query.mock.calls[1][1]).toEqual([
      20,
      "Agro Norte",
      null,
      null,
      null,
      14.7,
      -90.5,
    ]);
  });

  test("rechaza coordenadas fuera del rango geográfico", async () => {
    const response = await request(app)
      .put("/api/distribuidores/5")
      .set("Authorization", "Bearer token-valido")
      .send({ latitud: 91, longitud: -90.5 });

    expect(response.statusCode).toBe(400);
    expect(response.body).toEqual({ error: "Coordenadas inválidas" });
    expect(pool.query).not.toHaveBeenCalled();
  });
});
