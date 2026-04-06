const { Pool } = require("pg");

const dbHost = process.env.DB_HOST?.trim();
const dbName = process.env.DB_NAME?.trim();
const dbUser = process.env.DB_USER?.trim();
const dbPassword = process.env.DB_PASSWORD?.trim();
const dbPortValue = process.env.DB_PORT?.trim() || "5432";

const missingEnvVars = [
  ["DB_HOST", dbHost],
  ["DB_NAME", dbName],
  ["DB_USER", dbUser],
  ["DB_PASSWORD", dbPassword],
].filter(([, value]) => !value);

if (missingEnvVars.length > 0) {
  throw new Error(
    `Missing required database environment variables: ${missingEnvVars
      .map(([name]) => name)
      .join(", ")}`,
  );
}

const dbPort = Number(dbPortValue);
if (Number.isNaN(dbPort)) {
  throw new Error(`Invalid DB_PORT value: ${dbPortValue}`);
}

const pool = new Pool({
  host: dbHost,
  port: dbPort,
  database: dbName,
  user: dbUser,
  password: dbPassword,
});

pool.once("connect", () => {
  console.log("Conectado a PostgreSQL");
});

pool.on("error", (err) => {
  console.error("Error en el pool de PostgreSQL:", err);
});

const shutdownPool = async () => {
  await pool.end();
  console.log("Pool de PostgreSQL cerrado.");
};

module.exports = { pool, shutdownPool };
