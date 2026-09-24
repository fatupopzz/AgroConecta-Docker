const assert = require("node:assert/strict");
const test = require("node:test");

process.env.DB_HOST ||= "localhost";
process.env.DB_NAME ||= "agroconecta";
process.env.DB_USER ||= "agroconecta_user";
process.env.DB_PASSWORD ||= "agroconecta_pass";

const { notifyNearbyInstallations } = require("../src/services/PestAlertPushService");
const { pool } = require("../src/config/db");
const {
  getPestAlertById,
  registerPestAlertToken,
} = require("../src/controllers/pestAlertController");
const alert = {
  id_alerta: 29,
  tipo_plaga: "pulgon",
  cultivo_afectado: "Frijol",
  latitud: "14.6349",
  longitud: "-90.5069",
};

test("push skips database work when Firebase is not configured", async () => {
  const database = { query: () => assert.fail("database should not be queried") };
  const result = await notifyNearbyInstallations(
    { alert, reporterUserId: 7 },
    { database, config: null },
  );
  assert.equal(result.skipped, "firebase_not_configured");
});

test("push targets nearby FCM registration tokens", async () => {
  const queries = [];
  const database = {
    query: async (sql, params) => {
      queries.push({ sql, params });
      return { rows: [{ fcm_registration_token: "fcm-token-ok" }] };
    },
  };
  const messages = [];
  const sender = async (_config, token, message) => {
    messages.push({ token, message });
    return {
      ok: message.token === "fcm-token-ok",
      status: 200,
    };
  };

  const result = await notifyNearbyInstallations(
    { alert, reporterUserId: 7 },
    {
      database,
      config: { projectId: "agroconecta", account: {} },
      tokenProvider: async () => "access-token",
      sender,
    },
  );

  assert.deepEqual(result, { sent: 1, failed: 0 });
  assert.deepEqual(queries[0].params, [14.6349, -90.5069, 7, 25]);
  assert.equal(messages[0].message.token, "fcm-token-ok");
  assert.equal(messages[0].message.fid, undefined);
  assert.equal(messages[0].message.data.alert_id, "29");
});

test("registration endpoint associates the FCM token and GPS with the authenticated user", async (t) => {
  const originalQuery = pool.query;
  let query;
  pool.query = async (sql, params) => {
    query = { sql, params };
    return { rows: [] };
  };
  t.after(() => { pool.query = originalQuery; });
  const response = {
    statusCode: null,
    status(code) { this.statusCode = code; return this; },
    json(body) { this.body = body; return this; },
  };

  await registerPestAlertToken({
    user: { id: 7 },
    body: { token: "fcm-token-29", latitud: 14.6349, longitud: -90.5069 },
  }, response);

  assert.equal(response.statusCode, 200);
  assert.match(query.sql, /ON CONFLICT \(fcm_registration_token\)/);
  assert.deepEqual(query.params, [7, "fcm-token-29", 14.6349, -90.5069]);
});

test("detail endpoint returns the alert referenced by a notification", async (t) => {
  const originalQuery = pool.query;
  let query;
  pool.query = async (sql, params) => {
    query = { sql, params };
    return { rows: [alert] };
  };
  t.after(() => { pool.query = originalQuery; });
  const response = {
    statusCode: null,
    status(code) { this.statusCode = code; return this; },
    json(body) { this.body = body; return this; },
  };

  await getPestAlertById({ params: { id: "29" } }, response);

  assert.equal(response.statusCode, 200);
  assert.deepEqual(query.params, [29]);
  assert.equal(response.body.id_alerta, 29);
});
