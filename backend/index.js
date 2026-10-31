require("dotenv").config();

const app = require("./app");
const { shutdownPool, pool } = require("./src/config/db");
const { runMigrations, UnversionedDatabaseError } = require("./src/db/migrator");
const { connectRedis, shutdownRedis } = require("./src/config/redis");
const {
  startRecurringOrderScheduler,
  stopRecurringOrderScheduler,
} = require("./src/services/recurringOrderScheduler");
const {
  startPlantingReminderScheduler,
  stopPlantingReminderScheduler,
} = require("./src/services/plantingReminderScheduler");

const PORT = process.env.PORT || 8080;

// El esquema se versiona con backend/sql/NNN_*.sql y la tabla schema_migrations.
// Al arrancar se aplican solo las migraciones pendientes, así ninguna se queda
// olvidada al desplegar. Si la base es anterior al versionado se avisa y se
// sigue: el backend no debe caerse por eso, pero hay que correr --baseline.
const runStartupMigrations = async () => {
  const client = await pool.connect();
  try {
    await runMigrations({ client });
  } catch (error) {
    if (!(error instanceof UnversionedDatabaseError)) throw error;
    console.warn(`Migraciones omitidas: ${error.message}`);
  } finally {
    client.release();
  }
};

const startServer = async () => {
    try {
        await runStartupMigrations();
        await connectRedis();

        app.listen(PORT, () => {
            console.log(`Backend AgroConecta escuchando en puerto ${PORT}`);
        });
        startRecurringOrderScheduler();
        startPlantingReminderScheduler();

    } catch (error) {
        console.error(error);
        process.exit(1);
    }
};

startServer();

process.on("SIGTERM", async () => {
    stopRecurringOrderScheduler();
    stopPlantingReminderScheduler();
    await shutdownRedis();
    await shutdownPool();
    process.exit(0);
});
