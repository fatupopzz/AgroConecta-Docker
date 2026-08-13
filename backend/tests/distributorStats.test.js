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

const distributorToken = {
  id: 10,
  email: "distribuidor@agroconecta.gt",
  tipo: "distribuidor",
};

describe("GET /api/distribuidores/:id/stats", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jwt.verify.mockReturnValue(distributorToken);
  });

  test("rechaza solicitudes sin JWT", async () => {
    const response = await request(app).get("/api/distribuidores/7/stats");

    expect(response.statusCode).toBe(403);
    expect(response.body).toEqual({ error: "Token requerido" });
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("rechaza un ID de distribuidor inválido", async () => {
    const response = await request(app)
      .get("/api/distribuidores/no-valido/stats")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(400);
    expect(response.body).toEqual({ error: "ID de distribuidor inválido" });
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("rechaza usuarios que no son distribuidores", async () => {
    jwt.verify.mockReturnValue({ id: 20, tipo: "agricultor" });

    const response = await request(app)
      .get("/api/distribuidores/7/stats")
      .set("Authorization", "Bearer token-agricultor");

    expect(response.statusCode).toBe(403);
    expect(response.body).toEqual({
      error: "Solo puedes consultar las estadísticas de tu propio distribuidor",
    });
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("rechaza administradores porque las estadísticas son privadas", async () => {
    jwt.verify.mockReturnValue({ id: 1, tipo: "administrador" });

    const response = await request(app)
      .get("/api/distribuidores/7/stats")
      .set("Authorization", "Bearer token-administrador");

    expect(response.statusCode).toBe(403);
    expect(response.body).toEqual({
      error: "Solo puedes consultar las estadísticas de tu propio distribuidor",
    });
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("rechaza el acceso a las estadísticas de otro distribuidor", async () => {
    pool.query.mockResolvedValueOnce({
      rows: [{ id_distribuidor: 8 }],
    });

    const response = await request(app)
      .get("/api/distribuidores/7/stats")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(403);
    expect(response.body).toEqual({
      error: "Solo puedes consultar las estadísticas de tu propio distribuidor",
    });
    expect(pool.query).toHaveBeenCalledTimes(1);
  });

  test("retorna métricas vacías para un distribuidor sin pedidos", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_distribuidor: 7 }] })
      .mockResolvedValueOnce({
        rows: [{ total_pedidos: 0, ingresos_totales: "0" }],
      })
      .mockResolvedValueOnce({ rows: [] })
      .mockResolvedValueOnce({ rows: [] });

    const response = await request(app)
      .get("/api/distribuidores/7/stats")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual({
      totalPedidos: 0,
      ingresosTotales: 0,
      productosMasVendidos: [],
      pedidosPorEstado: [
        { estado: "confirmado", cantidad: 0 },
        { estado: "preparando", cantidad: 0 },
        { estado: "en_ruta", cantidad: 0 },
        { estado: "entregado", cantidad: 0 },
        { estado: "cancelado", cantidad: 0 },
      ],
    });
  });

  test("retorna totales, top 5 y pedidos agrupados por estado", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_distribuidor: 7 }] })
      .mockResolvedValueOnce({
        rows: [{ total_pedidos: 4, ingresos_totales: "925.50" }],
      })
      .mockResolvedValueOnce({
        rows: [
          { nombre: "Fertilizante A", cantidad: 8, ingresos: "600.00" },
          { nombre: "Semilla B", cantidad: 5, ingresos: "325.50" },
        ],
      })
      .mockResolvedValueOnce({
        rows: [
          { estado: "confirmado", cantidad: 1 },
          { estado: "entregado", cantidad: 3 },
        ],
      });

    const response = await request(app)
      .get("/api/distribuidores/7/stats")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual({
      totalPedidos: 4,
      ingresosTotales: 925.5,
      productosMasVendidos: [
        { nombre: "Fertilizante A", cantidad: 8, ingresos: 600 },
        { nombre: "Semilla B", cantidad: 5, ingresos: 325.5 },
      ],
      pedidosPorEstado: [
        { estado: "confirmado", cantidad: 1 },
        { estado: "preparando", cantidad: 0 },
        { estado: "en_ruta", cantidad: 0 },
        { estado: "entregado", cantidad: 3 },
        { estado: "cancelado", cantidad: 0 },
      ],
    });
    expect(pool.query).toHaveBeenCalledTimes(4);
    expect(pool.query.mock.calls[1][1]).toEqual([7, "entregado"]);
    expect(pool.query.mock.calls[2][1]).toEqual([7, "entregado"]);
    expect(pool.query.mock.calls[3][1]).toEqual([7]);
  });

  test("retorna 500 cuando falla una consulta de estadísticas", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_distribuidor: 7 }] })
      .mockRejectedValueOnce(new Error("database unavailable"))
      .mockResolvedValueOnce({ rows: [] })
      .mockResolvedValueOnce({ rows: [] });

    const consoleError = jest.spyOn(console, "error").mockImplementation(() => {});

    const response = await request(app)
      .get("/api/distribuidores/7/stats")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(500);
    expect(response.body).toEqual({
      error: "Error al obtener estadísticas del distribuidor",
    });

    consoleError.mockRestore();
  });
});
