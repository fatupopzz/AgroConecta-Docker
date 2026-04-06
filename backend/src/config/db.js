const { Pool } = require("pg");

const dbPort = Number(process.env.DB_PORT);
if (Number.isNaN(dbPort)) {
  throw new Error(`Invalid DB_PORT value: ${process.env.DB_PORT}`);
}

const pool = new Pool({
  host: process.env.DB_HOST,
  port: dbPort,
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
});

pool.once("connect", () => {
  console.log("Conectado a PostgreSQL");
});

pool.on("error", (err) => {
  console.error("Error en el pool de PostgreSQL:", err);
  process.emit("uncaughtException", err);
});

module.exports = pool;
