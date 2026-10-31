const { installCommonJsMock } = require("./helpers/commonJsMocks");

const pool = { query: vi.fn() };
const jwt = { verify: vi.fn() };

installCommonJsMock(require.resolve("../src/config/db"), { pool });
installCommonJsMock(require.resolve("jsonwebtoken"), jwt);

const request = require("supertest");
const app = require("../app");

describe("GET /api/distribuidores", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    jwt.verify.mockReturnValue({ id: 12, tipo: "agricultor" });
  });

  test("devuelve el promedio de las reseñas del distribuidor como número", async () => {
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

    // Las reseñas de productos no cuentan: un producto lo venden varios
    // distribuidores y su calificación no es del servicio de ninguno.
    const [sql] = pool.query.mock.calls[0];
    expect(sql).toMatch(/FROM resena_distribuidor r/);
    expect(sql).not.toMatch(/JOIN resena r ON/);
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
      distancia_km: null,
    }));
  });

  test("calcula distancia en km y ordena distribuidores por cercanía", async () => {
    pool.query.mockResolvedValueOnce({
      rows: [
        {
          id_distribuidor: 2,
          nombre_negocio: "Agro Cercano",
          promedio_resenas: "4.20",
          cantidad_resenas: 3,
          distancia_km: "3.42",
        },
        {
          id_distribuidor: 9,
          nombre_negocio: "Sin ubicación",
          promedio_resenas: null,
          cantidad_resenas: null,
          distancia_km: null,
        },
      ],
    });

    const response = await request(app)
      .get("/api/distribuidores?lat=14.6349&lng=-90.5069")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body[0]).toEqual(expect.objectContaining({
      id_distribuidor: 2,
      distancia_km: 3.42,
    }));
    expect(response.body[1].distancia_km).toBeNull();

    const [sql, params] = pool.query.mock.calls[0];
    expect(params).toEqual([14.6349, -90.5069]);
    expect(sql).toMatch(/6371 \* ACOS/);
    expect(sql).toMatch(/WHEN d\.latitud IS NULL OR d\.longitud IS NULL THEN NULL/);
    expect(sql).toMatch(/ORDER BY distancia_km ASC NULLS LAST/);
  });

  test("rechaza una ubicación de consulta incompleta", async () => {
    const response = await request(app)
      .get("/api/distribuidores?lat=14.6349")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(400);
    expect(response.body).toEqual({
      error: "latitud y longitud deben enviarse juntas",
    });
    expect(pool.query).not.toHaveBeenCalled();
  });
});

describe("coordenadas de distribuidores", () => {
  beforeEach(() => {
    vi.clearAllMocks();
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

describe("calificación y reseñas de un distribuidor", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    jwt.verify.mockReturnValue({ id: 12, tipo: "agricultor" });
  });

  test("la calificación sale de resena_distribuidor y actualiza la columna guardada", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_distribuidor: 4 }] })
      .mockResolvedValueOnce({
        rows: [{
          calificacion_promedio: "4.5",
          total_resenas: 2,
          cinco_estrellas: 1,
          cuatro_estrellas: 1,
          tres_estrellas: 0,
          dos_estrellas: 0,
          una_estrella: 0,
        }],
      })
      .mockResolvedValueOnce({ rows: [] });

    const response = await request(app)
      .get("/api/distribuidores/4/rating")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual({
      id_distribuidor: 4,
      calificacion_promedio: 4.5,
      total_resenas: 2,
      distribucion: { 5: 1, 4: 1, 3: 0, 2: 0, 1: 0 },
    });
    const [ratingSql, ratingParams] = pool.query.mock.calls[1];
    expect(ratingSql).toMatch(/FROM resena_distribuidor r/);
    expect(ratingSql).not.toMatch(/inventario_distribuidor/);
    expect(ratingParams).toEqual([4]);
  });

  test("lista las reseñas del distribuidor sin pasar por sus productos", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_distribuidor: 4 }] })
      .mockResolvedValueOnce({
        rows: [{
          id_resena: 9,
          calificacion: 5,
          comentario: "Entregó a tiempo",
          fecha_resena: "2026-10-20T10:00:00.000Z",
          id_producto: null,
          producto_nombre: null,
          agricultor_nombre: "María",
        }],
      })
      .mockResolvedValueOnce({ rows: [{ total: 1 }] });

    const response = await request(app)
      .get("/api/distribuidores/4/reviews?page=1&limit=20")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body).toEqual(expect.objectContaining({ page: 1, limit: 20, total: 1, total_pages: 1 }));
    expect(response.body.reviews[0]).toEqual(expect.objectContaining({
      id_resena: 9,
      agricultor_nombre: "María",
      id_producto: null,
    }));
    const [reviewsSql, reviewsParams] = pool.query.mock.calls[1];
    expect(reviewsSql).toMatch(/FROM resena_distribuidor r/);
    expect(reviewsSql).not.toMatch(/JOIN producto/);
    expect(reviewsParams).toEqual([4, 20, 0]);
  });

  test("guardar una reseña recalcula la calificación del distribuidor", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_agricultor: 30 }] })
      .mockResolvedValueOnce({ rows: [{ id_distribuidor: 4 }] })
      .mockResolvedValueOnce({ rows: [{ id_resena: 11, calificacion: 4 }] })
      .mockResolvedValueOnce({ rows: [] });

    const response = await request(app)
      .post("/api/distribuidoresReview/4/reviews")
      .set("Authorization", "Bearer token-valido")
      .send({ calificacion: 4, comentario: "Buen trato" });

    expect(response.statusCode).toBe(201);
    expect(pool.query.mock.calls[2][1]).toEqual([30, 4, 4, "Buen trato"]);
    const [updateSql, updateParams] = pool.query.mock.calls[3];
    expect(updateSql).toMatch(/UPDATE distribuidor/);
    expect(updateSql).toMatch(/FROM resena_distribuidor/);
    expect(updateParams).toEqual([4]);
  });

  test("rechaza calificaciones con decimales", async () => {
    const response = await request(app)
      .post("/api/distribuidoresReview/4/reviews")
      .set("Authorization", "Bearer token-valido")
      .send({ calificacion: 4.5 });

    expect(response.statusCode).toBe(400);
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("avisa si el agricultor ya calificó a ese distribuidor", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id_agricultor: 30 }] })
      .mockResolvedValueOnce({ rows: [{ id_distribuidor: 4 }] })
      .mockRejectedValueOnce(Object.assign(new Error("duplicada"), { code: "23505" }));

    const response = await request(app)
      .post("/api/distribuidoresReview/4/reviews")
      .set("Authorization", "Bearer token-valido")
      .send({ calificacion: 5 });

    expect(response.statusCode).toBe(409);
    expect(response.body).toEqual({ error: "Ya calificaste a este distribuidor" });
  });
});

describe("GET /api/distribuidores/:id/productos", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    jwt.verify.mockReturnValue({ id: 12, tipo: "agricultor" });
  });

  test("devuelve en una consulta lo que vende, con los campos del catálogo", async () => {
    pool.query.mockResolvedValueOnce({
      rows: [{
        id_producto: 1,
        nombre: "Urea 46%",
        categoria: "Fertilizantes",
        imagen_thumb_url: "/uploads/productos/urea_thumb.webp",
        precio_desde: "185.00",
        num_distribuidores: 2,
      }],
    });

    const response = await request(app)
      .get("/api/distribuidores/4/productos")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(200);
    expect(response.body[0]).toEqual(expect.objectContaining({
      id_producto: 1,
      categoria: "Fertilizantes",
      precio_desde: "185.00",
    }));
    expect(pool.query).toHaveBeenCalledTimes(1);
    const [sql, params] = pool.query.mock.calls[0];
    expect(params).toEqual([4]);
    expect(sql).toMatch(/JOIN categoria c/);
    expect(sql).toMatch(/p\.activo = true/);
    expect(sql).toMatch(/i\.stock_disponible > 0/);
  });

  test("rechaza un ID inválido sin consultar la base", async () => {
    const response = await request(app)
      .get("/api/distribuidores/abc/productos")
      .set("Authorization", "Bearer token-valido");

    expect(response.statusCode).toBe(400);
    expect(pool.query).not.toHaveBeenCalled();
  });
});
