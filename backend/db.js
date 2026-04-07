/**
 * @file db.js
 * @description Conexión a la base de datos PostgreSQL mediante un pool de conexiones.
 * Las credenciales se leen desde variables de entorno definidas en docker-compose.yml.
 * 
 * Variables requeridas:
 *  - DB_HOST      Host de la base de datos (ej: "db" en Docker)
 *  - DB_PORT      Puerto PostgreSQL (default 5432)
 *  - DB_NAME      Nombre de la base de datos
 *  - DB_USER      Usuario de PostgreSQL
 *  - DB_PASSWORD  Contraseña de PostgreSQL
 */

const { Pool } = require('pg');

const pool = new Pool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
});

module.exports = pool;