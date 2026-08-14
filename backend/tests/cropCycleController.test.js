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

const bcryptPath = require.resolve("bcrypt");
require.cache[bcryptPath] = {
  id: bcryptPath,
  filename: bcryptPath,
  loaded: true,
  exports: {},
};

const {
  createGetCropCycles,
  getGuatemalaMonth,
  normalizeCropName,
  resolveCropPhases,
} = require("../src/controllers/cropCycleController");
const app = require("../app");

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

const phase = (id, name, startMonth, endMonth) => ({
  id_ciclo: id,
  cultivo: "café",
  fase: name,
  mes_inicio: startMonth,
  mes_fin: endMonth,
  descripcion: `Descripción de ${name}`,
  productos_recomendados: [`Producto para ${name}`],
});

test("normalizeCropName accepts case, spaces and omitted accents", () => {
  assert.equal(normalizeCropName("  MAIZ "), "maíz");
  assert.equal(normalizeCropName("Café"), "café");
  assert.equal(normalizeCropName("tomate"), "tomate");
  assert.equal(normalizeCropName("cultivo 123"), null);
});

test("getGuatemalaMonth uses Guatemala time at a UTC month boundary", () => {
  const utcSeptember = new Date("2026-09-01T05:30:00.000Z");

  assert.equal(getGuatemalaMonth(utcSeptember), 8);
});

test("resolveCropPhases returns the active phase and nearest next phase", () => {
  const phases = [
    phase(1, "Pre-siembra", 1, 4),
    phase(2, "Siembra", 5, 6),
    phase(3, "Desarrollo", 7, 9),
    phase(4, "Cosecha", 10, 12),
  ];

  const result = resolveCropPhases(phases, 8);

  assert.deepEqual(result.activePhases.map(({ fase }) => fase), ["Desarrollo"]);
  assert.equal(result.nextPhase.fase, "Cosecha");
});

test("resolveCropPhases supports phases that cross the end of the year", () => {
  const phases = [
    phase(1, "Cosecha", 11, 3),
    phase(2, "Poda", 4, 5),
    phase(3, "Floración", 6, 7),
    phase(4, "Desarrollo", 8, 10),
  ];

  const january = resolveCropPhases(phases, 1);
  const november = resolveCropPhases(phases, 11);

  assert.equal(january.activePhases[0].fase, "Cosecha");
  assert.equal(january.nextPhase.fase, "Poda");
  assert.equal(november.activePhases[0].fase, "Cosecha");
  assert.equal(november.nextPhase.fase, "Poda");
});

test("getCropCycles returns current and next phases for an existing crop", async () => {
  const queries = [];
  const phases = [
    phase(1, "Cosecha", 11, 3),
    phase(2, "Poda", 4, 5),
    phase(3, "Floración", 6, 7),
    phase(4, "Desarrollo", 8, 10),
  ];
  const database = {
    async query(sql, params) {
      queries.push({ sql, params });
      return { rows: phases };
    },
  };
  const getCropCycles = createGetCropCycles({
    database,
    now: () => new Date("2026-08-15T12:00:00.000Z"),
  });
  const res = response();

  await getCropCycles({ params: { cultivo: "CAFE" } }, res);

  assert.equal(res.statusCode, 200);
  assert.equal(queries.length, 1);
  assert.deepEqual(queries[0].params, ["café"]);
  assert.match(queries[0].sql, /FROM ciclo_cultivo/);
  assert.equal(res.body.cultivo, "café");
  assert.equal(res.body.mes_actual, 8);
  assert.equal(res.body.fase_actual.fase, "Desarrollo");
  assert.deepEqual(res.body.fases_activas, [res.body.fase_actual]);
  assert.equal(res.body.proxima_fase.fase, "Cosecha");
});

test("getCropCycles rejects invalid crop names without querying the database", async () => {
  const database = {
    async query() {
      assert.fail("La base de datos no debe consultarse");
    },
  };
  const getCropCycles = createGetCropCycles({ database });
  const res = response();

  await getCropCycles({ params: { cultivo: "123" } }, res);

  assert.equal(res.statusCode, 400);
  assert.deepEqual(res.body, { error: "Cultivo inválido" });
});

test("getCropCycles returns 404 when the crop has no configured phases", async () => {
  const database = { query: async () => ({ rows: [] }) };
  const getCropCycles = createGetCropCycles({ database });
  const res = response();

  await getCropCycles({ params: { cultivo: "aguacate" } }, res);

  assert.equal(res.statusCode, 404);
  assert.match(res.body.error, /no se encontraron ciclos/i);
});

test("GET /api/ciclos/:cultivo is registered in the application", async () => {
  const result = await request(app).get("/api/ciclos/123");

  assert.equal(result.status, 400);
  assert.deepEqual(result.body, { error: "Cultivo inválido" });
});
