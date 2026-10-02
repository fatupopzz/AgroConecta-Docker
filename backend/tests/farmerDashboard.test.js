jest.mock("../src/config/db", () => ({
  pool: {
    query: jest.fn(),
  },
}));

jest.mock("jsonwebtoken", () => ({
  verify: jest.fn(),
}));

jest.mock("bcrypt", () => ({
  hash: jest.fn(),
  compare: jest.fn(),
}));

const request = require("supertest");
const jwt = require("jsonwebtoken");
const app = require("../app");
const { pool } = require("../src/config/db");

describe("GET /api/users/dashboard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jwt.verify.mockReturnValue({ id: 12, tipo: "agricultor" });
  });

  test("requiere autenticacion", async () => {
    const response = await request(app).get("/api/users/dashboard");

    expect(response.statusCode).toBe(401);
    expect(response.body).toEqual({ error: "Token requerido" });
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("solo permite agricultores", async () => {
    jwt.verify.mockReturnValue({ id: 4, tipo: "distribuidor" });

    const response = await request(app)
      .get("/api/users/dashboard")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(403);
    expect(response.body).toEqual({
      error: "Solo un agricultor puede consultar este dashboard",
    });
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("retorna metricas vacias para un agricultor sin pedidos", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_agricultor: 9 }] })
      .mockResolvedValueOnce({
        rows: [{
          total_pedidos: 0,
          total_gastado_historico: "0",
          total_gastado_mes_actual: "0",
        }],
      })
      .mockResolvedValueOnce({
        rows: [
          { mes: "2026-08", total: "0" },
          { mes: "2026-09", total: "0" },
        ],
      })
      .mockResolvedValueOnce({ rows: [] })
      .mockResolvedValueOnce({ rows: [] });

    const response = await request(app)
      .get("/api/users/dashboard")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual({
      totalGastadoHistorico: 0,
      totalGastadoMesActual: 0,
      cantidadPedidos: 0,
      gastosPorMes: [
        { mes: "2026-08", total: 0 },
        { mes: "2026-09", total: 0 },
      ],
      productosMasComprados: [],
      ultimoPedido: null,
    });
  });

  test("retorna totales, serie mensual, top 5 y ultimo pedido", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_agricultor: 9 }] })
      .mockResolvedValueOnce({
        rows: [{
          total_pedidos: 7,
          total_gastado_historico: "1575.50",
          total_gastado_mes_actual: "325.25",
        }],
      })
      .mockResolvedValueOnce({
        rows: [
          { mes: "2026-08", total: "1250.25" },
          { mes: "2026-09", total: "325.25" },
        ],
      })
      .mockResolvedValueOnce({
        rows: [
          { id_producto: 2, nombre: "Fertilizante", cantidad: 10, total_gastado: "900.00" },
          { id_producto: 5, nombre: "Semilla", cantidad: 6, total_gastado: "675.50" },
        ],
      })
      .mockResolvedValueOnce({
        rows: [{
          id_pedido: 44,
          fecha_pedido: "2026-09-25T15:00:00.000Z",
          estado: "en_ruta",
          total_pedido: "325.25",
          distribuidor_nombre: "Agro Centro",
        }],
      });

    const response = await request(app)
      .get("/api/users/dashboard")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual({
      totalGastadoHistorico: 1575.5,
      totalGastadoMesActual: 325.25,
      cantidadPedidos: 7,
      gastosPorMes: [
        { mes: "2026-08", total: 1250.25 },
        { mes: "2026-09", total: 325.25 },
      ],
      productosMasComprados: [
        { idProducto: 2, nombre: "Fertilizante", cantidad: 10, totalGastado: 900 },
        { idProducto: 5, nombre: "Semilla", cantidad: 6, totalGastado: 675.5 },
      ],
      ultimoPedido: {
        id: 44,
        fechaPedido: "2026-09-25T15:00:00.000Z",
        estado: "en_ruta",
        totalPedido: 325.25,
        distribuidorNombre: "Agro Centro",
      },
    });
    expect(pool.query).toHaveBeenCalledTimes(5);
    expect(pool.query.mock.calls[1][1]).toEqual([9, "cancelado"]);
    expect(pool.query.mock.calls[2][1]).toEqual([9, "cancelado"]);
    expect(pool.query.mock.calls[3][1]).toEqual([9, "cancelado"]);
    expect(pool.query.mock.calls[4][1]).toEqual([9]);
  });

  test("retorna 404 si falta el perfil de agricultor", async () => {
    pool.query.mockResolvedValueOnce({ rows: [] });

    const response = await request(app)
      .get("/api/users/dashboard")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(404);
    expect(response.body).toEqual({ error: "Perfil de agricultor no encontrado" });
  });

  test("retorna un error controlado si falla una agregacion", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_agricultor: 9 }] })
      .mockRejectedValueOnce(new Error("database unavailable"));
    const consoleError = jest.spyOn(console, "error").mockImplementation(() => {});

    const response = await request(app)
      .get("/api/users/dashboard")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(500);
    expect(response.body).toEqual({
      error: "Error interno del servidor",
    });
    consoleError.mockRestore();
  });
});
