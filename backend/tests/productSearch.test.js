const { installCommonJsMock } = require("./helpers/commonJsMocks");

const pool = { query: vi.fn() };
const jwt = { verify: vi.fn() };
const bcrypt = { hash: vi.fn(), compare: vi.fn() };

installCommonJsMock(require.resolve("../src/config/db"), { pool });
installCommonJsMock(require.resolve("jsonwebtoken"), jwt);
installCommonJsMock(require.resolve("bcrypt"), bcrypt);

const request = require("supertest");
const app = require("../app");

describe("GET /api/products con búsqueda", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("filtra por nombre y categoría conservando la paginación", async () => {
    const products = [
      {
        id_producto: 12,
        nombre: "Abono foliar orgánico",
        categoria: "Fertilizantes",
        precio_desde: "85.00",
        num_distribuidores: "2",
      },
    ];
    pool.query
      .mockResolvedValueOnce({ rows: [{ total: "1" }] })
      .mockResolvedValueOnce({ rows: products });

    const response = await request(app)
      .get("/api/products")
      .query({
        nombre: "  abono foliar  ",
        id_categoria: 4,
        page: 2,
        limit: 10,
      });

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual({
      page: 2,
      limit: 10,
      total: 1,
      products,
    });
    expect(pool.query).toHaveBeenCalledTimes(2);

    const [countSql, countParams] = pool.query.mock.calls[0];
    expect(countSql).toMatch(/p\.nombre ILIKE \$1/);
    expect(countSql).toMatch(/p\.id_categoria = \$2/);
    expect(countParams).toEqual(["%abono foliar%", 4]);

    const [productsSql, productsParams] = pool.query.mock.calls[1];
    expect(productsSql).toMatch(/p\.nombre ILIKE \$1/);
    expect(productsSql).toMatch(/p\.id_categoria = \$2/);
    expect(productsSql).toMatch(
      /ORDER BY LOWER\(p\.nombre\) ASC, p\.id_producto ASC/,
    );
    expect(productsSql).not.toMatch(/ORDER BY RANDOM\(\)/);
    expect(productsParams).toEqual(["%abono foliar%", 4, 10, 10]);
  });

  test("rechaza nombres compuestos que no sean texto", async () => {
    const response = await request(app).get(
      "/api/products?nombre=abono&nombre=foliar",
    );

    expect(response.statusCode).toBe(400);
    expect(response.body).toEqual({ error: "nombre debe ser texto" });
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("busca porcentajes y guiones bajos como caracteres literales", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ total: "0" }] })
      .mockResolvedValueOnce({ rows: [] });

    const response = await request(app)
      .get("/api/products")
      .query({ nombre: "50%_mezcla!" });

    expect(response.statusCode).toBe(200);
    expect(pool.query).toHaveBeenCalledTimes(2);

    const [countSql, countParams] = pool.query.mock.calls[0];
    expect(countSql).toMatch(/p\.nombre ILIKE \$1 ESCAPE '!'/);
    expect(countParams).toEqual(["%50!%!_mezcla!!%"]);

    const [productsSql, productsParams] = pool.query.mock.calls[1];
    expect(productsSql).toMatch(/p\.nombre ILIKE \$1 ESCAPE '!'/);
    expect(productsParams).toEqual(["%50!%!_mezcla!!%", 10, 0]);
  });
});
