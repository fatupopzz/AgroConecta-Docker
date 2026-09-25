const assert = require("node:assert/strict");
const test = require("node:test");
const request = require("supertest");

const dbPath = require.resolve("../src/config/db");
require.cache[dbPath] = {
  id: dbPath,
  filename: dbPath,
  loaded: true,
  exports: { pool: {} },
};

const {
  createFavoriteController,
} = require("../src/controllers/favoriteController");
const app = require("../app");

const response = () => ({
  statusCode: 200,
  body: null,
  sent: false,
  status(code) {
    this.statusCode = code;
    return this;
  },
  json(body) {
    this.body = body;
    return this;
  },
  send() {
    this.sent = true;
    return this;
  },
});

const farmerRequest = (overrides = {}) => ({
  user: { id: 25, tipo: "agricultor" },
  body: {},
  params: {},
  ...overrides,
});

test("favorites routes require authentication", async () => {
  const getResponse = await request(app).get("/api/favorites");
  const postResponse = await request(app)
    .post("/api/favorites")
    .send({ productId: 4 });
  const deleteResponse = await request(app).delete("/api/favorites/4");

  assert.equal(getResponse.statusCode, 401);
  assert.equal(postResponse.statusCode, 401);
  assert.equal(deleteResponse.statusCode, 401);
});

test("favorites are only available to farmers", async () => {
  const database = {
    query: async () => assert.fail("database must not be queried"),
  };
  const { getFavorites } = createFavoriteController(database);
  const res = response();

  await getFavorites(
    farmerRequest({ user: { id: 25, tipo: "distribuidor" } }),
    res,
  );

  assert.equal(res.statusCode, 403);
  assert.deepEqual(res.body, {
    error: "Los favoritos solo están disponibles para agricultores",
  });
});

test("addFavorite validates productId before querying", async () => {
  const database = {
    query: async () => assert.fail("database must not be queried"),
  };
  const { addFavorite } = createFavoriteController(database);
  const res = response();

  await addFavorite(farmerRequest({ body: { productId: "invalid" } }), res);

  assert.equal(res.statusCode, 400);
  assert.deepEqual(res.body, { error: "productId inválido" });
});

test("addFavorite persists the authenticated user and active product", async () => {
  const favorite = {
    id: 9,
    id_usuario: 25,
    id_producto: 4,
    fecha_agregado: "2026-09-25T12:00:00.000Z",
  };
  const queries = [];
  const database = {
    async query(sql, params) {
      queries.push({ sql, params });
      return { rowCount: 1, rows: [favorite] };
    },
  };
  const { addFavorite } = createFavoriteController(database);
  const res = response();

  await addFavorite(farmerRequest({ body: { productId: 4 } }), res);

  assert.equal(res.statusCode, 201);
  assert.deepEqual(res.body.favorite, favorite);
  assert.deepEqual(queries[0].params, [25, 4]);
  assert.match(queries[0].sql, /p\.activo = TRUE/);
  assert.match(queries[0].sql, /ON CONFLICT \(id_usuario, id_producto\)/);
});

test("addFavorite returns 404 for an unavailable product", async () => {
  const database = {
    query: async () => ({ rowCount: 0, rows: [] }),
  };
  const { addFavorite } = createFavoriteController(database);
  const res = response();

  await addFavorite(farmerRequest({ body: { productId: 404 } }), res);

  assert.equal(res.statusCode, 404);
  assert.deepEqual(res.body, { error: "Producto no encontrado" });
});

test("getFavorites returns product-card data newest first", async () => {
  const favorites = [
    {
      id_favorito: 9,
      fecha_agregado: "2026-09-25T12:00:00.000Z",
      id_producto: 4,
      nombre: "Semilla de maíz",
      precio_desde: "125.00",
      num_distribuidores: 2,
    },
  ];
  const queries = [];
  const database = {
    async query(sql, params) {
      queries.push({ sql, params });
      return { rows: favorites };
    },
  };
  const { getFavorites } = createFavoriteController(database);
  const res = response();

  await getFavorites(farmerRequest(), res);

  assert.equal(res.statusCode, 200);
  assert.deepEqual(res.body, favorites);
  assert.deepEqual(queries[0].params, [25]);
  assert.match(queries[0].sql, /MIN\(i\.precio\)/);
  assert.match(queries[0].sql, /ORDER BY f\.fecha_agregado DESC/);
});

test("removeFavorite deletes only the authenticated user's product", async () => {
  const queries = [];
  const database = {
    async query(sql, params) {
      queries.push({ sql, params });
      return { rowCount: 1, rows: [{ id: 9 }] };
    },
  };
  const { removeFavorite } = createFavoriteController(database);
  const res = response();

  await removeFavorite(farmerRequest({ params: { productId: "4" } }), res);

  assert.equal(res.statusCode, 204);
  assert.equal(res.sent, true);
  assert.deepEqual(queries[0].params, [25, 4]);
  assert.match(queries[0].sql, /id_usuario = \$1/);
  assert.match(queries[0].sql, /id_producto = \$2/);
});

test("removeFavorite returns 404 when the product was not saved", async () => {
  const database = {
    query: async () => ({ rowCount: 0, rows: [] }),
  };
  const { removeFavorite } = createFavoriteController(database);
  const res = response();

  await removeFavorite(farmerRequest({ params: { productId: "4" } }), res);

  assert.equal(res.statusCode, 404);
  assert.deepEqual(res.body, { error: "El producto no estaba en favoritos" });
});
