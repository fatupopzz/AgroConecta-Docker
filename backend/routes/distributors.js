/**
 * @file routes/distributors.js
 * @description Rutas para el módulo de distribuidores.
 * Maneja el registro de nuevos distribuidores en la plataforma AgroConecta.
 * 
 * Base path: /api/distributors
 */

const router = require('express').Router();
const pool = require('../db');

router.post('/register', async (req, res) => {
  const { nombre, telefono, email, contrasena, nombre_negocio, nit, departamento } = req.body;

  if (!nombre || !telefono || !contrasena || !nombre_negocio) {
    return res.status(400).json({ error: 'Campos requeridos faltantes' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const userResult = await client.query(
      `INSERT INTO usuario (nombre, telefono, email, contrasena_hash, tipo_usuario)
       VALUES ($1, $2, $3, crypt($4, gen_salt('bf', 10)), 'distribuidor')
       RETURNING id_usuario, nombre, telefono, email, fecha_registro`,
      [nombre, telefono, email, contrasena]
    );

    const usuario = userResult.rows[0];

    const distResult = await client.query(
      `INSERT INTO distribuidor (id_usuario, nombre_negocio, nit, departamento)
       VALUES ($1, $2, $3, $4)
       RETURNING id_distribuidor, estado_verificacion`,
      [usuario.id_usuario, nombre_negocio, nit, departamento]
    );

    await client.query('COMMIT');

    res.status(201).json({
      message: 'Distribuidor registrado. Estado: pendiente de verificación.',
      usuario,
      distribuidor: distResult.rows[0],
    });
  } catch (err) {
    await client.query('ROLLBACK');
    if (err.code === '23505') {
      return res.status(409).json({ error: 'Teléfono, email o NIT ya registrado' });
    }
    res.status(500).json({ error: 'Error interno del servidor' });
  } finally {
    client.release();
  }
});

module.exports = router;