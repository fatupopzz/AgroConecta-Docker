#!/usr/bin/env node
// Aplica las migraciones pendientes de backend/sql/.
//
//   node scripts/migrate.js                     aplica las pendientes
//   node scripts/migrate.js --dry-run           solo muestra cuáles aplicaría
//   node scripts/migrate.js --status            aplicadas y pendientes
//   node scripts/migrate.js --baseline 019      marca hasta 019 como aplicadas
//                                               sin correrlas (bases anteriores
//                                               al versionado, como la VM)
//
// Usa las mismas variables de entorno que el backend (DB_HOST, DB_NAME...).

const {
  baseline,
  migrationStatus,
  runMigrations,
} = require("../src/db/migrator");

function parseArgs(argv) {
  const args = { dryRun: false, status: false, baseline: null };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--dry-run") args.dryRun = true;
    else if (arg === "--status") args.status = true;
    else if (arg === "--baseline") {
      args.baseline = argv[i + 1];
      i += 1;
      if (!args.baseline) throw new Error("--baseline necesita el archivo o número hasta donde marcar.");
    } else {
      throw new Error(`Opción desconocida: ${arg}`);
    }
  }
  return args;
}

async function main(argv = process.argv.slice(2), { pool, logger = console } = {}) {
  const args = parseArgs(argv);
  const client = await pool.connect();
  try {
    if (args.status) {
      const status = await migrationStatus({ client });
      if (!status.versioned) {
        logger.log(status.hasSchema
          ? "La base no está versionada todavía (falta --baseline)."
          : "La base está vacía.");
      }
      logger.log(`Aplicadas (${status.applied.length}):`);
      status.applied.forEach((file) => logger.log(`  ✓ ${file}`));
      logger.log(`Pendientes (${status.pending.length}):`);
      status.pending.forEach((file) => logger.log(`  · ${file}`));
      return 0;
    }
    if (args.baseline) {
      await baseline({ client, until: args.baseline, dryRun: args.dryRun, logger });
      return 0;
    }
    await runMigrations({ client, dryRun: args.dryRun, logger });
    return 0;
  } catch (error) {
    logger.error(error.message || String(error));
    return 1;
  } finally {
    client.release();
  }
}

if (require.main === module) {
  require("dotenv").config();
  const { pool, shutdownPool } = require("../src/config/db");
  main(process.argv.slice(2), { pool })
    .then(async (code) => {
      await shutdownPool();
      process.exit(code);
    })
    .catch(async (error) => {
      console.error(error);
      await shutdownPool();
      process.exit(1);
    });
}

module.exports = { main, parseArgs };
