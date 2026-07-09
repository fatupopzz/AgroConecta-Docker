const assert = require("node:assert/strict");
const test = require("node:test");

const dbPath = require.resolve("../src/config/db");
const controllerPath = require.resolve("../src/controllers/adminMetricsController");

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

const createResponse = () => ({
  statusCode: 200,
  body: null,
  status(code) {
    this.statusCode = code;
    return this;
  },
  json(payload) {
    this.body = payload;
    return this;
  },
});

test("getAdminMetrics formats aggregate query results", async () => {
  const queryResults = [
    {
      rows: [
        { type: "administrador", total: 1 },
        { type: "agricultor", total: 8 },
        { type: "distribuidor", total: 3 },
      ],
    },
    {
      rows: [
        { status: "confirmado", total: 2 },
        { status: "entregado", total: 5 },
      ],
    },
    { rows: [{ total: 14 }] },
    {
      rows: [
        { state: "pending", total: 2 },
        { state: "verified", total: 3 },
      ],
    },
  ];
  const queries = [];
  const pool = {
    query: async (sql) => {
      queries.push(sql);
      return queryResults[queries.length - 1];
    },
  };
  const { getAdminMetrics } = loadController(pool);
  const res = createResponse();

  await getAdminMetrics({}, res);

  assert.equal(res.statusCode, 200);
  assert.deepEqual(res.body, {
    users: {
      total: 12,
      byType: [
        { type: "administrador", count: 1 },
        { type: "agricultor", count: 8 },
        { type: "distribuidor", count: 3 },
      ],
    },
    orders: {
      total: 7,
      byStatus: [
        { status: "confirmado", count: 2 },
        { status: "entregado", count: 5 },
      ],
    },
    products: {
      total: 14,
    },
    distributors: {
      verified: 3,
      pending: 2,
    },
  });
  assert.equal(queries.length, 4);
  assert.match(queries[0], /FROM usuario/);
  assert.match(queries[1], /FROM pedido/);
  assert.match(queries[2], /FROM producto WHERE activo = true/);
  assert.match(queries[3], /FROM distribuidor/);
});

test("getAdminMetrics returns zero distributor counters when states are missing", async () => {
  const queryResults = [
    { rows: [{ type: "agricultor", total: 1 }] },
    { rows: [] },
    { rows: [{ total: 0 }] },
    { rows: [] },
  ];
  const pool = {
    query: async () => queryResults.shift(),
  };
  const { getAdminMetrics } = loadController(pool);
  const res = createResponse();

  await getAdminMetrics({}, res);

  assert.deepEqual(res.body.distributors, {
    verified: 0,
    pending: 0,
  });
});

test("getAdminMetrics returns 500 when an aggregate query fails", async () => {
  const pool = {
    query: async () => {
      throw new Error("database unavailable");
    },
  };
  const { getAdminMetrics } = loadController(pool);
  const res = createResponse();
  const originalConsoleError = console.error;
  console.error = () => {};

  try {
    await getAdminMetrics({}, res);
  } finally {
    console.error = originalConsoleError;
  }

  assert.equal(res.statusCode, 500);
  assert.deepEqual(res.body, {
    error: "Error al obtener metricas administrativas",
  });
});
