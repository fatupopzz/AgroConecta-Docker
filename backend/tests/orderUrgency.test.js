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

const runSuccessfulOrder = async (body) => {
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

  await createOrder({ body, agricultorId: 2 }, res);

  return { queries, res };
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
  const { queries, res } = await runSuccessfulOrder(validBody);

  assert.equal(res.statusCode, 201);
  const insert = queries.find(({ sql }) => sql.includes("INSERT INTO pedido\n"));
  assert.equal(insert.params[4], false);
  assert.equal(insert.params[5], null);
});

test("createOrder creates a differentiated urgent notification", async () => {
  const { queries, res } = await runSuccessfulOrder({
    ...validBody,
    esUrgente: true,
    tipoPlaga: "Pulgón",
  });

  assert.equal(res.statusCode, 201);
  const notification = queries.find(({ sql }) =>
    sql.includes("INSERT INTO notificacion")
  );
  assert.equal(notification.params[2], "pedido_urgente");

  const content = JSON.parse(notification.params[3]);
  assert.equal(content.esUrgente, true);
  assert.equal(content.tipoPlaga, "Pulgón");
  assert.match(content.mensaje, /urgente/i);
});

test("getOrdersByDistributor returns urgency fields ordered first", async () => {
  const queries = [];
  const pool = {
    async query(sql) {
      queries.push(sql);
      if (sql.includes("SELECT 1 FROM distribuidor")) return { rows: [{}] };
      return { rows: [{ id: 11, es_urgente: true, tipo_plaga: "Trips" }] };
    },
  };
  const { getOrdersByDistributor } = loadController(pool);
  const res = response();

  await getOrdersByDistributor({ params: { id: "3" } }, res);

  assert.equal(res.statusCode, 200);
  assert.equal(res.body[0].es_urgente, true);
  assert.match(queries[1], /p\.es_urgente/);
  assert.match(queries[1], /ORDER BY p\.es_urgente DESC/);
});
