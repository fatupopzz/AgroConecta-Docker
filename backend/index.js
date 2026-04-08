const express = require('express');
require('dotenv').config();
const app = express();
const PORT = process.env.PORT || 8080;

const { Pool } = require("pg");

const pool = new Pool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  port: process.env.DB_PORT,
});

app.get('/', (req, res) => {
  res.json({ status: 'AgroConecta Backend corriendo', version: '1.0.0' });
});

app.get("/health", (req, res) => {
  res.send("OK");
});

app.get("/test-db", async (req, res) => {
  try {
    const result = await pool.query("SELECT NOW()");
    res.json(result.rows);
  } catch (err) {
    console.error("ERROR DB:", err.message);
    res.status(500).send(err.message);
  }
});


app.listen(PORT, () => {
  console.log(`Backend AgroConecta escuchando en puerto ${PORT}`);
});
