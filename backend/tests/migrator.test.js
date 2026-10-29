const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");

const {
  DEFAULT_MIGRATIONS_DIR,
  UnversionedDatabaseError,
  baseline,
  listMigrations,
  runMigrations,
} = require("../src/db/migrator");
const { main, parseArgs } = require("../scripts/migrate");

const silent = { log: () => {}, error: () => {} };

function migrationsDir(files) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "migraciones-"));
  for (const [name, sql] of Object.entries(files)) {
    fs.writeFileSync(path.join(dir, name), sql);
  }
  return dir;
}

/**
 * Cliente de Postgres de mentira que entiende lo justo: si existe la tabla de
 * control, qué migraciones registró y las transacciones. Un registro hecho
 * dentro de una transacción que termina en ROLLBACK se descarta, igual que en
 * Postgres.
 */
function fakeClient({ hasSchema = false, versioned = false, applied = [] } = {}) {
  const state = {
    hasSchema,
    versioned,
    applied: new Set(applied),
    executed: [],
    statements: [],
    inTransaction: false,
    pendingRecords: [],
  };

  const client = {
    state,
    async query(sql, params = []) {
      state.statements.push(sql.trim().split("\n")[0]);
      if (sql.includes("to_regclass")) {
        return { rows: [{ versioned: state.versioned, has_schema: state.hasSchema }] };
      }
      if (sql.startsWith("SELECT nombre FROM schema_migrations")) {
        return { rows: [...state.applied].map((nombre) => ({ nombre })) };
      }
      if (sql.includes("pg_advisory")) return { rows: [] };
      if (sql.startsWith("CREATE TABLE IF NOT EXISTS schema_migrations")) {
        state.versioned = true;
        return { rows: [] };
      }
      if (sql.startsWith("INSERT INTO schema_migrations")) {
        if (state.inTransaction) state.pendingRecords.push(params[0]);
        else state.applied.add(params[0]);
        return { rows: [] };
      }
      if (sql === "BEGIN") {
        state.inTransaction = true;
        return { rows: [] };
      }
      if (sql === "COMMIT") {
        state.pendingRecords.forEach((name) => state.applied.add(name));
        state.pendingRecords = [];
        state.inTransaction = false;
        return { rows: [] };
      }
      if (sql === "ROLLBACK") {
        state.pendingRecords = [];
        state.inTransaction = false;
        return { rows: [] };
      }
      // Cualquier otra cosa es el contenido de una migración.
      if (sql.includes("FALLA")) throw new Error("error de sintaxis");
      state.executed.push(sql.trim());
      state.hasSchema = true;
      return { rows: [] };
    },
  };
  return client;
}

const threeMigrations = {
  "001_init.sql": "CREATE TABLE usuario (id INT);",
  "002_carrito.sql": "CREATE TABLE carrito (id INT);",
  "003_pedidos.sql": "CREATE TABLE pedido (id INT);",
};

describe("migrador de esquema", () => {
  test("en una base vacía aplica todas en orden y las registra", async () => {
    const dir = migrationsDir(threeMigrations);
    const client = fakeClient();

    const result = await runMigrations({ client, dir, logger: silent });

    expect(result.applied).toEqual(["001_init.sql", "002_carrito.sql", "003_pedidos.sql"]);
    expect(client.state.executed).toEqual([
      "CREATE TABLE usuario (id INT);",
      "CREATE TABLE carrito (id INT);",
      "CREATE TABLE pedido (id INT);",
    ]);
    expect([...client.state.applied]).toEqual(result.applied);
  });

  test("correrlo dos veces no aplica nada la segunda vez", async () => {
    const dir = migrationsDir(threeMigrations);
    const client = fakeClient();

    await runMigrations({ client, dir, logger: silent });
    const executedAfterFirstRun = client.state.executed.length;
    const second = await runMigrations({ client, dir, logger: silent });

    expect(second.applied).toEqual([]);
    expect(second.pending).toEqual([]);
    expect(client.state.executed).toHaveLength(executedAfterFirstRun);
  });

  test("con --dry-run informa las pendientes sin tocar la base", async () => {
    const dir = migrationsDir(threeMigrations);
    const client = fakeClient({ versioned: true, hasSchema: true, applied: ["001_init.sql"] });

    const result = await runMigrations({ client, dir, dryRun: true, logger: silent });

    expect(result.pending).toEqual(["002_carrito.sql", "003_pedidos.sql"]);
    expect(result.applied).toEqual([]);
    expect(client.state.executed).toEqual([]);
    expect([...client.state.applied]).toEqual(["001_init.sql"]);
  });

  test("se niega a migrar una base anterior al versionado", async () => {
    const dir = migrationsDir(threeMigrations);
    const client = fakeClient({ hasSchema: true, versioned: false });

    await expect(runMigrations({ client, dir, logger: silent })).rejects.toBeInstanceOf(UnversionedDatabaseError);
    expect(client.state.executed).toEqual([]);
  });

  test("--baseline marca sin correr y después solo se aplica lo nuevo", async () => {
    const dir = migrationsDir(threeMigrations);
    const client = fakeClient({ hasSchema: true, versioned: false });

    const marked = await baseline({ client, until: "002", dir, logger: silent });
    expect(marked.marked).toEqual(["001_init.sql", "002_carrito.sql"]);
    expect(client.state.executed).toEqual([]);

    const result = await runMigrations({ client, dir, logger: silent });
    expect(result.applied).toEqual(["003_pedidos.sql"]);
    expect(client.state.executed).toEqual(["CREATE TABLE pedido (id INT);"]);
  });

  test("--baseline con un número que no existe avisa en vez de marcar todo", async () => {
    const dir = migrationsDir(threeMigrations);
    const client = fakeClient({ hasSchema: true });

    await expect(baseline({ client, until: "099", dir, logger: silent })).rejects.toThrow(/No existe la migración/);
    expect([...client.state.applied]).toEqual([]);
  });

  test("si una falla se deshace, no se registra y no sigue con las demás", async () => {
    const dir = migrationsDir({
      "001_init.sql": "CREATE TABLE usuario (id INT);",
      "002_rota.sql": "FALLA;",
      "003_pedidos.sql": "CREATE TABLE pedido (id INT);",
    });
    const client = fakeClient();

    await expect(runMigrations({ client, dir, logger: silent })).rejects.toThrow(/002_rota\.sql/);
    expect([...client.state.applied]).toEqual(["001_init.sql"]);
    expect(client.state.statements).toContain("ROLLBACK");
    expect(client.state.executed).not.toContain("CREATE TABLE pedido (id INT);");
  });

  test("no envuelve en otra transacción los scripts que traen la suya", async () => {
    const dir = migrationsDir({
      "001_propia.sql": "BEGIN;\nCREATE TABLE siembra (id INT);\nCOMMIT;",
    });
    const client = fakeClient();

    await runMigrations({ client, dir, logger: silent });

    expect(client.state.statements).not.toContain("BEGIN");
    expect([...client.state.applied]).toEqual(["001_propia.sql"]);
  });

  test("solo cuenta archivos numerados y rechaza números repetidos", async () => {
    const ignored = migrationsDir({ ...threeMigrations, "borrador.sql": "x", "seed.sql": "y" });
    expect(await listMigrations(ignored)).toEqual(Object.keys(threeMigrations));

    const duplicated = migrationsDir({ "002_a.sql": "x", "002_b.sql": "y" });
    await expect(listMigrations(duplicated)).rejects.toThrow(/dos migraciones con el número 002/);
  });
});

describe("migraciones del repositorio", () => {
  test("están numeradas de forma consecutiva desde 001", async () => {
    const files = await listMigrations(DEFAULT_MIGRATIONS_DIR);
    const numbers = files.map((file) => Number(file.slice(0, 3)));
    expect(numbers).toEqual(numbers.map((_, index) => index + 1));
  });

  test("001_init.sql se registra solo para las bases que crea Docker", () => {
    const init = fs.readFileSync(path.join(DEFAULT_MIGRATIONS_DIR, "001_init.sql"), "utf8");
    expect(init).toMatch(/CREATE TABLE IF NOT EXISTS schema_migrations/);
    expect(init).toMatch(/INSERT INTO schema_migrations \(nombre\) VALUES \('001_init\.sql'\)/);
  });

  test("002_cart.sql ya no falla si las tablas existen", () => {
    const cart = fs.readFileSync(path.join(DEFAULT_MIGRATIONS_DIR, "002_cart.sql"), "utf8");
    expect(cart).not.toMatch(/CREATE TABLE (?!IF NOT EXISTS)/);
  });
});

describe("scripts/migrate.js", () => {
  test("entiende las opciones y rechaza las desconocidas", () => {
    expect(parseArgs(["--dry-run"])).toEqual({ dryRun: true, status: false, baseline: null });
    expect(parseArgs(["--baseline", "019"])).toEqual({ dryRun: false, status: false, baseline: "019" });
    expect(() => parseArgs(["--borrar-todo"])).toThrow(/Opción desconocida/);
    expect(() => parseArgs(["--baseline"])).toThrow(/necesita/);
  });

  test("devuelve código 1 y explica qué hacer en una base sin versionar", async () => {
    const client = { ...fakeClient({ hasSchema: true }), release: vi.fn() };
    const pool = { connect: vi.fn().mockResolvedValue(client) };
    const logger = { log: vi.fn(), error: vi.fn() };

    const code = await main([], { pool, logger });

    expect(code).toBe(1);
    expect(logger.error).toHaveBeenCalledWith(expect.stringMatching(/--baseline/));
    expect(client.release).toHaveBeenCalled();
  });
});
