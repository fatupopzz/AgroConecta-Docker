const assert = require("node:assert/strict");
const test = require("node:test");

const dbPath = require.resolve("../src/config/db");
require.cache[dbPath] = {
  id: dbPath,
  filename: dbPath,
  loaded: true,
  exports: { pool: {} },
};

const {
  parseCropNames,
  serializeCropNames,
  withCropList,
} = require("../src/utils/cropNames");
const {
  upsertFarmerProfile,
  getFarmerProfile,
} = require("../src/controllers/farmerController");

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

test("parseCropNames normalizes aliases, separators and duplicates", () => {
  const crops = parseCropNames(" MAIZ y Café; frijol / maíz ");

  assert.deepEqual(crops, ["maíz", "café", "frijol"]);
  assert.equal(serializeCropNames(crops), "maíz, café, frijol");
});

test("parseCropNames accepts arrays and rejects invalid values", () => {
  assert.deepEqual(parseCropNames(["Tomate", "elote"]), ["tomate", "elote"]);
  assert.deepEqual(parseCropNames(""), []);
  assert.equal(parseCropNames(["maíz", 123]), null);
  assert.equal(parseCropNames({ cultivo: "maíz" }), null);
});

test("withCropList preserves the legacy field and adds a normalized list", () => {
  const profile = withCropList({
    id_agricultor: 7,
    cultivos_principales: "Maiz, TOMATE",
  });

  assert.equal(profile.cultivos_principales, "Maiz, TOMATE");
  assert.deepEqual(profile.cultivos, ["maíz", "tomate"]);
});

test("upsertFarmerProfile stores normalized crops and returns the crop list", async () => {
  const queries = [];
  const database = require("../src/config/db").pool;
  database.query = async (sql, params) => {
    queries.push({ sql, params });
    return {
      rows: [{
        id_agricultor: 7,
        id_usuario: 4,
        cultivos_principales: params[5],
      }],
    };
  };
  const res = response();

  await upsertFarmerProfile(
    {
      user: { id: 4, tipo: "agricultor" },
      body: { cultivos_principales: ["MAIZ", "café", "maíz"] },
    },
    res,
  );

  assert.equal(res.statusCode, 201);
  assert.equal(queries.length, 1);
  assert.equal(queries[0].params[5], "maíz, café");
  assert.equal(queries[0].params[6], true);
  assert.deepEqual(res.body.perfil.cultivos, ["maíz", "café"]);
});

test("upsertFarmerProfile rejects malformed crops before querying", async () => {
  const database = require("../src/config/db").pool;
  database.query = async () => assert.fail("La base de datos no debe consultarse");
  const res = response();

  await upsertFarmerProfile(
    {
      user: { id: 4, tipo: "agricultor" },
      body: { cultivos_principales: ["maíz", "tomate123"] },
    },
    res,
  );

  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /cultivos_principales/);
});

test("getFarmerProfile returns normalized crops for existing legacy data", async () => {
  const database = require("../src/config/db").pool;
  let queryNumber = 0;
  database.query = async () => {
    queryNumber += 1;
    if (queryNumber === 1) {
      return {
        rows: [{
          id_agricultor: 7,
          cultivos_principales: "Cafe; Elote",
        }],
      };
    }
    return { rows: [{ id_usuario: 4 }] };
  };
  const res = response();

  await getFarmerProfile(
    {
      user: { id: 4, tipo: "agricultor" },
      params: { id: "7" },
    },
    res,
  );

  assert.equal(res.statusCode, 200);
  assert.deepEqual(res.body.perfil.cultivos, ["café", "elote"]);
});
