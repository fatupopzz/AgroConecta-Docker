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

describe("GET /api/productos/recomendados", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jwt.verify.mockReturnValue({ id: 25, tipo: "agricultor" });
  });

  test("requiere un JWT", async () => {
    const response = await request(app).get("/api/productos/recomendados");

    expect(response.statusCode).toBe(403);
    expect(response.body).toEqual({ error: "Token requerido" });
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("rechaza perfiles que no son de agricultor", async () => {
    jwt.verify.mockReturnValue({ id: 25, tipo: "distribuidor" });

    const response = await request(app)
      .get("/api/productos/recomendados")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(403);
    expect(response.body).toEqual({
      error: "Las recomendaciones personalizadas son solo para agricultores",
    });
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("personaliza por cultivos, categorías relevantes y departamento", async () => {
    const products = [
      {
        id_producto: 4,
        nombre: "Semilla de maíz mejorada",
        categoria: "Semillas",
        precio_desde: "125.00",
      },
    ];
    pool.query
      .mockResolvedValueOnce({
        rows: [
          {
            departamento: "Alta Verapaz",
            cultivos_principales: "Maíz, Frijol",
          },
        ],
      })
      .mockResolvedValueOnce({ rows: products });

    const response = await request(app)
      .get("/api/productos/recomendados")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual(products);
    expect(pool.query).toHaveBeenCalledTimes(2);
    expect(pool.query.mock.calls[0][1]).toEqual([25]);

    const [recommendationSql, recommendationParams] = pool.query.mock.calls[1];
    expect(recommendationParams).toEqual([
      ["Semillas", "Fertilizantes", "Pesticidas", "Herbicidas"],
      ["maíz", "frijol"],
      "Alta Verapaz",
    ]);
    expect(recommendationSql).toMatch(/c\.nombre = ANY\(\$1::text\[\]\)/);
    expect(recommendationSql).toMatch(/unnest\(\$2::text\[\]\)/);
    expect(recommendationSql).toMatch(/LOWER\(\$3\)/);
    expect(recommendationSql).toMatch(/LIMIT 10/);
  });

  test.each([
    [{ departamento: null, cultivos_principales: "Maíz" }],
    [{ departamento: "Guatemala", cultivos_principales: "" }],
    [undefined],
  ])("usa productos populares cuando el perfil está incompleto", async (profile) => {
    const popularProducts = Array.from({ length: 12 }, (_, index) => ({
      id_producto: index + 1,
      nombre: `Producto ${index + 1}`,
    }));
    pool.query
      .mockResolvedValueOnce({ rows: profile ? [profile] : [] })
      .mockResolvedValueOnce({ rows: popularProducts });

    const response = await request(app)
      .get("/api/productos/recomendados")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body).toHaveLength(10);
    const [recommendationSql, recommendationParams] = pool.query.mock.calls[1];
    expect(recommendationParams).toEqual([]);
    expect(recommendationSql).not.toMatch(/c\.nombre = ANY/);
    expect(recommendationSql).toMatch(/sales\.units_sold/);
    expect(recommendationSql).toMatch(/p\.calificacion_promedio DESC/);
  });

  test("retorna 500 si falla la consulta", async () => {
    pool.query.mockRejectedValueOnce(new Error("database unavailable"));
    const consoleError = jest.spyOn(console, "error").mockImplementation(() => {});

    const response = await request(app)
      .get("/api/productos/recomendados")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(500);
    expect(response.body).toEqual({
      error: "Error al obtener productos recomendados",
    });
    consoleError.mockRestore();
  });
});
