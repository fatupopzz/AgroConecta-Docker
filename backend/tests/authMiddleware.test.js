const assert = require("node:assert/strict");
const test = require("node:test");
const jwt = require("jsonwebtoken");
const verifyToken = require("../src/middleware/authMiddleware");

const restoreSecret = (previousSecret) => {
  if (previousSecret === undefined) {
    delete process.env.JWT_SECRET;
  } else {
    process.env.JWT_SECRET = previousSecret;
  }
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

test("auth middleware returns 401 when Authorization is missing", () => {
  const res = response();
  let nextCalled = false;

  verifyToken({ headers: {} }, res, () => { nextCalled = true; });

  assert.equal(res.statusCode, 401);
  assert.deepEqual(res.body, { error: "Token requerido" });
  assert.equal(nextCalled, false);
});

test("auth middleware returns 401 when token is invalid", () => {
  const previousSecret = process.env.JWT_SECRET;
  process.env.JWT_SECRET = "err-02-test-secret";
  const res = response();

  verifyToken(
    { headers: { authorization: "Bearer token-invalido" } },
    res,
    () => assert.fail("next must not be called")
  );

  restoreSecret(previousSecret);
  assert.equal(res.statusCode, 401);
  assert.deepEqual(res.body, { error: "Token inválido" });
});

test("auth middleware accepts a valid token", () => {
  const previousSecret = process.env.JWT_SECRET;
  process.env.JWT_SECRET = "err-02-test-secret";
  const token = jwt.sign(
    { id: 25, tipo: "agricultor" },
    process.env.JWT_SECRET
  );
  const req = { headers: { authorization: `Bearer ${token}` } };
  const res = response();
  let nextCalled = false;

  verifyToken(req, res, () => { nextCalled = true; });

  restoreSecret(previousSecret);
  assert.equal(nextCalled, true);
  assert.equal(req.user.id, 25);
  assert.equal(req.user.tipo, "agricultor");
});

test("rotating JWT_SECRET rejects old tokens and accepts newly issued tokens", () => {
  const previousSecret = process.env.JWT_SECRET;
  const oldSecret = "old-secret-used-only-for-this-test";
  const rotatedSecret = "rotated-secret-used-only-for-this-test";
  const oldToken = jwt.sign({ id: 25, tipo: "agricultor" }, oldSecret);
  process.env.JWT_SECRET = rotatedSecret;

  const rejectedResponse = response();
  verifyToken(
    { headers: { authorization: `Bearer ${oldToken}` } },
    rejectedResponse,
    () => assert.fail("an old token must not be accepted after rotation")
  );

  const newToken = jwt.sign({ id: 25, tipo: "agricultor" }, rotatedSecret);
  const acceptedRequest = {
    headers: { authorization: `Bearer ${newToken}` },
  };
  let nextCalled = false;
  verifyToken(acceptedRequest, response(), () => { nextCalled = true; });

  restoreSecret(previousSecret);
  assert.equal(rejectedResponse.statusCode, 401);
  assert.equal(nextCalled, true);
});
