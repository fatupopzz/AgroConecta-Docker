/**
 * @file routes/distributors.js
 * @description Rutas para el módulo de distribuidores.
 * Maneja el registro de nuevos distribuidores en la plataforma AgroConecta.
 *
 * Base path: /api/distributors
 */

const router = require("express").Router();
const { pool } = require("../config/db");

/**
 * POST /api/distributors/register
 *
 * Registra un nuevo distribuidor en la plataforma.
 * Crea un registro en las tablas `usuario` y `distribuidor` dentro de una
 * transacción atómica — si cualquiera de los dos INSERT falla, se hace ROLLBACK.
 *
 * La contraseña se hashea con bcrypt usando pgcrypto (crypt + gen_salt).
 * El estado inicial del distribuidor siempre es "pendiente" hasta que un
 * administrador lo verifique mediante PATCH /api/admin/distributors/:id/verify.
 *
 * @route   POST /api/distributors/register
 * @access  Público
 *
 * @body {string} nombre        - Nombre completo del distribuidor (requerido)
 * @body {string} telefono      - Teléfono único (requerido)
 * @body {string} contrasena    - Contraseña en texto plano, se hashea en DB (requerido)
 * @body {string} nombre_negocio - Nombre del negocio o empresa (requerido)
 * @body {string} [email]       - Correo electrónico (opcional, debe ser único)
 * @body {string} [nit]         - NIT del negocio (opcional, debe ser único)
 * @body {string} [departamento] - Departamento de Guatemala donde opera (opcional)
 *
 * @returns {201} Distribuidor creado exitosamente
 * @returns {400} Faltan campos requeridos
 * @returns {409} Teléfono, email o NIT ya registrado (violación de UNIQUE)
 * @returns {500} Error interno del servidor
 */
router.post("/register", async (req, res) => {
  const {
    nombre,
    telefono,
    email,
    contrasena,
    nombre_negocio,
    nit,
    departamento,
  } = req.body;

  if (!nombre || !telefono || !contrasena || !nombre_negocio) {
    return res.status(400).json({ error: "Campos requeridos faltantes" });
  }

  const client = await pool.connect();
  try {
    await client.query("BEGIN");

    const userResult = await client.query(
      `INSERT INTO usuario (nombre, telefono, email, contrasena_hash, tipo_usuario)
       VALUES ($1, $2, $3, crypt($4, gen_salt('bf', 10)), 'distribuidor')
       RETURNING id_usuario, nombre, telefono, email, fecha_registro`,
      [nombre, telefono, email, contrasena],
    );

    const usuario = userResult.rows[0];

    const distResult = await client.query(
      `INSERT INTO distribuidor (id_usuario, nombre_negocio, nit, departamento)
       VALUES ($1, $2, $3, $4)
       RETURNING id_distribuidor, estado_verificacion`,
      [usuario.id_usuario, nombre_negocio, nit, departamento],
    );

    await client.query("COMMIT");

    res.status(201).json({
      message: "Distribuidor registrado. Estado: pendiente de verificación.",
      usuario,
      distribuidor: distResult.rows[0],
    });
  } catch (err) {
    try {
      await client.query("ROLLBACK");
    } catch (rollbackErr) {
      console.error("Error al ejecutar ROLLBACK:", rollbackErr);
    }
    if (err.code === "23505") {
      return res
        .status(409)
        .json({ error: "Teléfono, email o NIT ya registrado" });
    }
    res.status(500).json({ error: "Error interno del servidor" });
  } finally {
    client.release();
  }
});

module.exports = router;

