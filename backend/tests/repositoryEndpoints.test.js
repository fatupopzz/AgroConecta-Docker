const { installCommonJsMock } = require("./helpers/commonJsMocks");
const pool = { query: vi.fn() };
const jwt = { verify: vi.fn() };
const cache = { get: vi.fn(), set: vi.fn(), invalidate: vi.fn() };
installCommonJsMock(require.resolve("../src/config/db"), { pool });
installCommonJsMock(require.resolve("jsonwebtoken"), jwt);
installCommonJsMock(require.resolve("../src/services/cacheService"), cache);

const request = require("supertest");
const app = require("../app");

describe("contratos HTTP despues de extraer los repositorios", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    jwt.verify.mockReturnValue({ id: 1, tipo: "administrador" });
    cache.get.mockResolvedValue({ hit: false, available: false });
    cache.invalidate.mockResolvedValue(true);
  });

  test.each([
    ["post", "/api/products", { id_categoria: 2, nombre: "Semilla" }, 201, "Producto creado correctamente"],
    ["put", "/api/products/7", { nombre: "Semilla" }, 200, "Producto actualizado correctamente"],
    ["delete", "/api/products/7", {}, 200, "Producto eliminado correctamente"],
  ])("%s mantiene respuesta e invalidacion de products", async (method, url, body, status, message) => {
    const producto = { id_producto: 7, nombre: "Semilla" };
    if (method === "post") pool.query.mockResolvedValueOnce({ rows: [{}] });
    pool.query.mockResolvedValueOnce({ rows: [producto], rowCount: 1 });
    const response = await request(app)[method](url).set("Authorization", "Bearer test").send(body);
    expect(response.statusCode).toBe(status);
    expect(response.body).toEqual({ message, producto });
    expect(cache.invalidate).toHaveBeenCalledExactlyOnceWith(["products"]);
  });

  test("una actualizacion inexistente conserva el 404 y no invalida cache", async () => {
    pool.query.mockResolvedValue({ rows: [], rowCount: 0 });
    const response = await request(app).put("/api/products/7")
      .set("Authorization", "Bearer test").send({ nombre: "Semilla" });
    expect(response.statusCode).toBe(404);
    expect(response.body).toEqual({ error: "Producto no encontrado" });
    expect(cache.invalidate).not.toHaveBeenCalled();
  });

  test.each([
    ["/api/products/7", { nombre: "Semilla" }],
    ["/api/users/7", { nombre: "Juan" }],
  ])("%s propaga errores del repositorio al manejador KAN-105", async (url, body) => {
    pool.query.mockRejectedValue(Object.assign(new Error("duplicate"), { code: "23505" }));
    const response = await request(app).put(url).set("Authorization", "Bearer test").send(body);
    expect(response.statusCode).toBe(409);
    expect(response.body).toEqual({ error: "El registro ya existe" });
    expect(cache.invalidate).not.toHaveBeenCalled();
  });

  test("la validacion de producto ocurre antes de consultar el repositorio", async () => {
    const response = await request(app).put("/api/products/7")
      .set("Authorization", "Bearer test").send({ calificacion_promedio: 6 });
    expect(response.statusCode).toBe(400);
    expect(pool.query).not.toHaveBeenCalled();
    expect(cache.invalidate).not.toHaveBeenCalled();
  });
});
