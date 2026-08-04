const assert = require("node:assert/strict");
const test = require("node:test");

const dbPath = require.resolve("../src/config/db");
const controllerPath = require.resolve("../src/controllers/orderController");

const loadController = (pool) => {
  delete require.cache[controllerPath];
  require.cache[dbPath] = {
    id: dbPath,
    filename: dbPath,
    loaded: true,
    exports: { pool },
  };
  return require(controllerPath);
};

const response = () => ({
  statusCode: 200,
  body: null,
  status(code) {
    this.statusCode = code;
    return this;
  },
  json(body) {
    this.body = body;
    return this;
  },
});

const validBody = {
  id_distribuidor: 3,
  direccion_entrega: "Parcela norte, Guatemala",
  productos: [{ id_inventario: 8, cantidad: 1 }],
  metodo_pago: "efectivo",
};

test("createOrder rejects non-boolean esUrgente before opening a transaction", async () => {
  const pool = { connect: async () => assert.fail("must not connect") };
  const { createOrder } = loadController(pool);
  const res = response();

  await createOrder(
    { body: { ...validBody, esUrgente: "true" }, agricultorId: 2 },
    res
  );

  assert.equal(res.statusCode, 400);
  assert.deepEqual(res.body, { error: "esUrgente debe ser boolean" });
});

test("createOrder accepts omitted urgency fields for backwards compatibility", async () => {
  const queries = [];
  const client = {
    release() {},
    async query(sql, params) {
      queries.push({ sql, params });
      if (sql.includes("SELECT 1 FROM agricultor")) return { rows: [{}] };
      if (sql.includes("SELECT 1 FROM distribuidor")) return { rows: [{}] };
      if (sql.includes("FROM inventario_distribuidor i")) {
        return {
          rows: [{
            id_inventario: 8,
            id_distribuidor: 3,
            precio: 25,
            stock_disponible: 4,
            producto_nombre: "Control biológico",
          }],
        };
      }
      if (sql.includes("INSERT INTO pedido\n")) {
        return { rows: [{ id_pedido: 11 }] };
      }
      if (sql.includes("SELECT u.nombre")) return { rows: [{ nombre: "Ana" }] };
      if (sql.includes("FROM pedido p\n")) return { rows: [] };
      return { rows: [] };
    },
  };
  const { createOrder } = loadController({ connect: async () => client });
  const res = response();

  await createOrder({ body: validBody, agricultorId: 2 }, res);

  assert.equal(res.statusCode, 201);
  const insert = queries.find(({ sql }) => sql.includes("INSERT INTO pedido\n"));
  assert.equal(insert.params[4], false);
  assert.equal(insert.params[5], null);
});
