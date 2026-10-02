const express = require("express");
const request = require("supertest");
const AppError = require("../src/errors/AppError");
const asyncHandler = require("../src/middleware/asyncHandler");
const { errorHandler, notFoundHandler } = require("../src/middleware/errorHandler");

const buildApp = (handler) => {
  const app = express();
  app.use(express.json());
  app.post("/test", asyncHandler(handler));
  app.get("/test", asyncHandler(handler));
  app.use(notFoundHandler);
  app.use(errorHandler);
  return app;
};

const postgresError = (code) => Object.assign(new Error(`pg ${code}`), { code });

describe("middleware central de errores", () => {
  let consoleError;

  beforeEach(() => {
    consoleError = jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    consoleError.mockRestore();
  });

  test("responde con el status y mensaje de un AppError", async () => {
    const app = buildApp(async () => {
      throw new AppError(404, "Producto no encontrado");
    });

    const response = await request(app).get("/test");

    expect(response.statusCode).toBe(404);
    expect(response.body).toEqual({ error: "Producto no encontrado" });
    expect(consoleError).not.toHaveBeenCalled();
  });

  test("incluye details cuando el AppError los trae", async () => {
    const app = buildApp(async () => {
      throw new AppError(400, "Datos inválidos", { campo: "precio" });
    });

    const response = await request(app).get("/test");

    expect(response.statusCode).toBe(400);
    expect(response.body).toEqual({
      error: "Datos inválidos",
      details: { campo: "precio" },
    });
  });

  test("oculta el detalle de errores inesperados y los registra", async () => {
    const app = buildApp(async () => {
      throw new Error("password authentication failed for user agroconecta");
    });

    const response = await request(app).get("/test");

    expect(response.statusCode).toBe(500);
    expect(response.body).toEqual({ error: "Error interno del servidor" });
    expect(consoleError).toHaveBeenCalledWith(
      "Error en GET /test:",
      expect.any(Error),
    );
  });

  test("captura errores lanzados de forma síncrona", async () => {
    const app = buildApp(() => {
      throw new Error("fallo síncrono");
    });

    const response = await request(app).get("/test");

    expect(response.statusCode).toBe(500);
    expect(response.body).toEqual({ error: "Error interno del servidor" });
  });

  test.each([
    ["23505", 409, "El registro ya existe"],
    ["23503", 409, "El registro hace referencia a datos inexistentes o en uso"],
    ["22P02", 400, "Formato de dato inválido"],
    ["23514", 400, "Los datos no cumplen las restricciones"],
  ])("traduce el error de PostgreSQL %s a %i", async (code, status, message) => {
    const app = buildApp(async () => {
      throw postgresError(code);
    });

    const response = await request(app).get("/test");

    expect(response.statusCode).toBe(status);
    expect(response.body).toEqual({ error: message });
  });

  test("responde 400 cuando el JSON del cuerpo es inválido", async () => {
    const app = buildApp(async (req, res) => res.json(req.body));

    const response = await request(app)
      .post("/test")
      .set("Content-Type", "application/json")
      .send('{"nombre": ');

    expect(response.statusCode).toBe(400);
    expect(response.body).toEqual({ error: "JSON inválido" });
  });

  test("responde 404 en JSON para rutas inexistentes", async () => {
    const app = buildApp(async (req, res) => res.json({ ok: true }));

    const response = await request(app).get("/no-existe");

    expect(response.statusCode).toBe(404);
    expect(response.body).toEqual({ error: "Ruta no encontrada" });
  });
});

describe("app principal", () => {
  test("usa el middleware central para rutas inexistentes", async () => {
    jest.resetModules();
    jest.doMock("../src/config/db", () => ({ pool: { query: jest.fn() } }));
    const app = require("../app");

    const response = await request(app).get("/no-existe");

    expect(response.statusCode).toBe(404);
    expect(response.body).toEqual({ error: "Ruta no encontrada" });
  });
});
