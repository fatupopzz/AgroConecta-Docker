const express = require("express");
const request = require("supertest");

const {
  buildRequestKey,
  cacheResponse,
  invalidateCache,
} = require("../src/middleware/cacheMiddleware");

const createApp = (method, path, middleware, handler) => {
  const app = express();
  app.use(express.json());
  app[method](path, middleware, handler);
  return app;
};

describe("cacheMiddleware", () => {
  it("normaliza los filtros para producir una clave estable", () => {
    const firstKey = buildRequestKey({
      baseUrl: "/api/products",
      path: "/",
      query: { page: "1", nombre: "maiz" },
    });
    const secondKey = buildRequestKey({
      baseUrl: "/api/products",
      path: "/",
      query: { nombre: "maiz", page: "1" },
    });

    expect(firstKey).toBe(secondKey);
  });

  it("devuelve un HIT sin ejecutar el controlador", async () => {
    const handler = vi.fn((req, res) => res.json({ source: "database" }));
    const cacheService = {
      get: vi.fn().mockResolvedValue({
        available: true,
        hit: true,
        value: { source: "redis" },
      }),
      set: vi.fn(),
    };
    const app = createApp(
      "get",
      "/products",
      cacheResponse("products", { cacheService }),
      handler,
    );

    const response = await request(app).get("/products?page=1");

    expect(response.status).toBe(200);
    expect(response.headers["x-cache"]).toBe("HIT");
    expect(response.body).toEqual({ source: "redis" });
    expect(handler).not.toHaveBeenCalled();
    expect(cacheService.set).not.toHaveBeenCalled();
  });

  it("guarda los MISS exitosos con el TTL configurado", async () => {
    const cacheService = {
      get: vi.fn().mockResolvedValue({
        available: true,
        hit: false,
        key: "agroconecta:cache:products:v1:/products:{}",
      }),
      set: vi.fn().mockResolvedValue(true),
    };
    const app = createApp(
      "get",
      "/products",
      cacheResponse("products", { cacheService, ttlSeconds: 90 }),
      (req, res) => res.json({ products: [] }),
    );

    const response = await request(app).get("/products");

    expect(response.headers["x-cache"]).toBe("MISS");
    expect(response.body).toEqual({ products: [] });
    expect(cacheService.set).toHaveBeenCalledWith(
      "agroconecta:cache:products:v1:/products:{}",
      { products: [] },
      90,
    );
  });

  it("continua hacia PostgreSQL cuando Redis no esta disponible", async () => {
    const cacheService = {
      get: vi.fn().mockRejectedValue(new Error("connection refused")),
      set: vi.fn(),
    };
    const app = createApp(
      "get",
      "/categories",
      cacheResponse("categories", { cacheService }),
      (req, res) => res.json([{ id: 1 }]),
    );

    const response = await request(app).get("/categories");

    expect(response.status).toBe(200);
    expect(response.headers["x-cache"]).toBe("BYPASS");
    expect(response.body).toEqual([{ id: 1 }]);
  });

  it("invalida namespaces solo despues de una mutacion exitosa", async () => {
    const cacheService = { invalidate: vi.fn().mockResolvedValue(true) };
    const invalidator = invalidateCache("products", "distributors");
    invalidator.cacheService = cacheService;
    const app = createApp("post", "/inventory", invalidator, (req, res) =>
      res.status(201).json({ id: 1 }),
    );

    const response = await request(app).post("/inventory").send({ stock: 10 });

    expect(response.status).toBe(201);
    expect(cacheService.invalidate).toHaveBeenCalledWith([
      "products",
      "distributors",
    ]);
  });
});
