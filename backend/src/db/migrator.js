const fs = require("node:fs/promises");
const path = require("node:path");

const DEFAULT_MIGRATIONS_DIR = path.join(__dirname, "..", "..", "sql");

// Solo cuentan los archivos numerados; así un borrador o un script de pruebas
// que se deje en la carpeta no se aplica por accidente.
const MIGRATION_FILE = /^(\d{3})_[a-z0-9_]+\.sql$/;

// Número arbitrario pero fijo: dos procesos que migran a la vez (por ejemplo,
// el arranque del backend y alguien corriendo el script) se esperan en vez de
// aplicar la misma migración dos veces.
const MIGRATION_LOCK_ID = 7_240_118;

const CREATE_TABLE_SQL = `CREATE TABLE IF NOT EXISTS schema_migrations (
  nombre      TEXT PRIMARY KEY,
  aplicada_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
)`;

const RECORD_SQL = `INSERT INTO schema_migrations (nombre) VALUES ($1)
ON CONFLICT (nombre) DO NOTHING`;

/**
 * La base ya tiene tablas pero nunca registró migraciones: es anterior al
 * versionado (como la VM). Correr todo desde 001 ahí volvería a ejecutar
 * scripts sin guardas, así que primero hay que marcar lo que ya tiene.
 */
class UnversionedDatabaseError extends Error {
  constructor() {
    super(
      "La base de datos tiene tablas pero no tiene schema_migrations. " +
        "Marca primero las migraciones que ya tiene aplicadas con " +
        "`node scripts/migrate.js --baseline <archivo>` (ver README)."
    );
    this.name = "UnversionedDatabaseError";
  }
}

async function listMigrations(dir = DEFAULT_MIGRATIONS_DIR) {
  const files = (await fs.readdir(dir)).filter((file) => MIGRATION_FILE.test(file)).sort();

  const seen = new Map();
  for (const file of files) {
    const number = file.match(MIGRATION_FILE)[1];
    if (seen.has(number)) {
      throw new Error(`Hay dos migraciones con el número ${number}: ${seen.get(number)} y ${file}`);
    }
    seen.set(number, file);
  }

  return files;
}

async function readState(client) {
  const { rows } = await client.query(
    `SELECT to_regclass('public.schema_migrations') IS NOT NULL AS versioned,
            to_regclass('public.usuario') IS NOT NULL AS has_schema`
  );
  const { versioned, has_schema: hasSchema } = rows[0];

  if (!versioned) {
    return { versioned, hasSchema, applied: new Set() };
  }

  const applied = await client.query("SELECT nombre FROM schema_migrations");
  return { versioned, hasSchema, applied: new Set(applied.rows.map((row) => row.nombre)) };
}

// Algunos scripts traen su propio BEGIN/COMMIT. Envolverlos en otra
// transacción haría que su COMMIT cerrara la de afuera antes de registrarlos.
function managesOwnTransaction(sql) {
  return /^\s*BEGIN\s*;/im.test(sql) && /^\s*COMMIT\s*;/im.test(sql);
}

async function applyMigration(client, dir, file) {
  const sql = await fs.readFile(path.join(dir, file), "utf8");

  if (managesOwnTransaction(sql)) {
    await client.query(sql);
    await client.query(RECORD_SQL, [file]);
    return;
  }

  await client.query("BEGIN");
  try {
    await client.query(sql);
    await client.query(RECORD_SQL, [file]);
    await client.query("COMMIT");
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  }
}

async function withLock(client, work) {
  await client.query("SELECT pg_advisory_lock($1)", [MIGRATION_LOCK_ID]);
  try {
    return await work();
  } finally {
    await client.query("SELECT pg_advisory_unlock($1)", [MIGRATION_LOCK_ID]);
  }
}

/**
 * Aplica, en orden, las migraciones que la base todavía no tiene. Con
 * `dryRun` solo informa cuáles aplicaría, sin tocar la base.
 */
async function runMigrations({ client, dir = DEFAULT_MIGRATIONS_DIR, dryRun = false, logger = console } = {}) {
  const files = await listMigrations(dir);

  return withLock(client, async () => {
    const state = await readState(client);
    if (!state.versioned && state.hasSchema) {
      throw new UnversionedDatabaseError();
    }

    const pending = files.filter((file) => !state.applied.has(file));
    if (pending.length === 0) {
      logger.log("Migraciones: la base está al día.");
      return { pending, applied: [] };
    }

    if (dryRun) {
      logger.log(`Migraciones pendientes (${pending.length}), sin aplicar por --dry-run:`);
      pending.forEach((file) => logger.log(`  - ${file}`));
      return { pending, applied: [] };
    }

    await client.query(CREATE_TABLE_SQL);
    const applied = [];
    for (const file of pending) {
      logger.log(`Aplicando ${file}…`);
      try {
        await applyMigration(client, dir, file);
      } catch (error) {
        error.message = `Falló la migración ${file}: ${error.message}`;
        throw error;
      }
      applied.push(file);
    }
    logger.log(`Migraciones aplicadas: ${applied.length}.`);
    return { pending, applied };
  });
}

/**
 * Marca como aplicadas, sin correrlas, todas las migraciones hasta `until`
 * inclusive. Es para bases anteriores al versionado que ya tienen esos
 * cambios hechos a mano.
 */
async function baseline({ client, until, dir = DEFAULT_MIGRATIONS_DIR, dryRun = false, logger = console }) {
  const files = await listMigrations(dir);
  const target = files.find((file) => file === until || file.startsWith(`${until}_`));
  if (!target) {
    throw new Error(`No existe la migración "${until}". Usa el nombre del archivo o su número (por ejemplo, 019).`);
  }

  const toMark = files.slice(0, files.indexOf(target) + 1);

  return withLock(client, async () => {
    const state = await readState(client);
    const missing = toMark.filter((file) => !state.applied.has(file));

    if (dryRun) {
      logger.log(`Se marcarían como aplicadas (${missing.length}), sin correrlas:`);
      missing.forEach((file) => logger.log(`  - ${file}`));
      return { marked: [] };
    }

    await client.query(CREATE_TABLE_SQL);
    for (const file of missing) {
      await client.query(RECORD_SQL, [file]);
    }
    logger.log(`Marcadas como aplicadas: ${missing.length} (hasta ${target}).`);
    return { marked: missing };
  });
}

async function migrationStatus({ client, dir = DEFAULT_MIGRATIONS_DIR }) {
  const files = await listMigrations(dir);
  const state = await readState(client);
  return {
    versioned: state.versioned,
    hasSchema: state.hasSchema,
    applied: files.filter((file) => state.applied.has(file)),
    pending: files.filter((file) => !state.applied.has(file)),
  };
}

module.exports = {
  DEFAULT_MIGRATIONS_DIR,
  UnversionedDatabaseError,
  baseline,
  listMigrations,
  migrationStatus,
  runMigrations,
};
