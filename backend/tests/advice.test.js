const assert = require("node:assert/strict");
const test = require("node:test");

const dbPath = require.resolve("../src/config/db");
const middlewarePath = require.resolve("../src/middleware/adviceAuthorizationMiddleware");
const controllerPath = require.resolve("../src/controllers/adviceController");

const loadModule = (modulePath, pool) => {
  delete require.cache[modulePath];
  require.cache[dbPath] = {
    id: dbPath,
    filename: dbPath,
    loaded: true,
    exports: { pool },
  };
  return require(modulePath);
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

const orderParticipant = {
  id_pedido: 25,
  agricultor_usuario_id: 7,
  distribuidor_usuario_id: 14,
  estado_verificacion: "verificado",
};

test("only an order participant can read its advice", async () => {
  const pool = { query: async () => ({ rows: [orderParticipant] }) };
  const { canAccessOrderAdvice } = loadModule(middlewarePath, pool);
  const res = response();
  let nextCalled = false;

  await canAccessOrderAdvice(
    { params: { id: "25" }, user: { id: 99, tipo: "agricultor" } },
    res,
    () => { nextCalled = true; }
  );

  assert.equal(res.statusCode, 403);
  assert.equal(nextCalled, false);
});

test("a verified distributor can send advice", async () => {
  const pool = { query: async () => ({ rows: [orderParticipant] }) };
  const { canSendOrderAdvice } = loadModule(middlewarePath, pool);
  const res = response();
  let nextCalled = false;

  await canSendOrderAdvice(
    { params: { id: "25" }, user: { id: 14, tipo: "distribuidor" } },
    res,
    () => { nextCalled = true; }
  );

  assert.equal(nextCalled, true);
  assert.equal(res.statusCode, 200);
});

test("a distributor that is not verified cannot send advice", async () => {
  const pool = {
    query: async () => ({
      rows: [{ ...orderParticipant, estado_verificacion: "pendiente" }],
    }),
  };
  const { canSendOrderAdvice } = loadModule(middlewarePath, pool);
  const res = response();

  await canSendOrderAdvice(
    { params: { id: "25" }, user: { id: 14, tipo: "distribuidor" } },
    res,
    () => assert.fail("next must not be called")
  );

  assert.equal(res.statusCode, 403);
  assert.match(res.body.error, /verificado/i);
});

test("sendAdviceMessage gets the sender from the authenticated token", async () => {
  let queryParams;
  const pool = {
    query: async (_sql, params) => {
      queryParams = params;
      return { rows: [{ id_mensaje: 1, mensaje: "Recomendación técnica" }] };
    },
  };
  const { sendAdviceMessage } = loadModule(controllerPath, pool);
  const res = response();

  await sendAdviceMessage(
    {
      params: { id: "25" },
      user: { id: 14, tipo: "distribuidor" },
      body: { mensaje: "  Recomendación técnica  ", id_usuario_remitente: 99 },
    },
    res
  );

  assert.equal(res.statusCode, 201);
  assert.deepEqual(queryParams, [25, 14, "Recomendación técnica"]);
});

test("sendAdviceMessage rejects empty messages", async () => {
  const pool = { query: async () => assert.fail("query must not be called") };
  const { sendAdviceMessage } = loadModule(controllerPath, pool);
  const res = response();

  await sendAdviceMessage(
    { params: { id: "25" }, user: { id: 7 }, body: { mensaje: "   " } },
    res
  );

  assert.equal(res.statusCode, 400);
});
