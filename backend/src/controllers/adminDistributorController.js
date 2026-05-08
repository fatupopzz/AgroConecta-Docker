const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

/**
 * GET /api/admin/distributors/pending
 * Lista todos los distribuidores con estado_verificacion = 'pendiente'.
 * Requiere middleware verifyAdmin.
 */
const getPendingDistributors = async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT d.id_distribuidor, d.nombre_negocio, d.nit, d.departamento,
              d.estado_verificacion, d.calificacion_promedio, d.fecha_verificacion,
              u.nombre, u.telefono, u.email, u.fecha_registro
       FROM distribuidor d
       JOIN usuario u ON d.id_usuario = u.id_usuario
       WHERE d.estado_verificacion = 'pendiente'
       ORDER BY u.fecha_registro ASC`
    );

    return res.json(result.rows);
  } catch (error) {
    console.error("Error en getPendingDistributors:", error);
    return res.status(500).json({ error: "Error al obtener distribuidores pendientes" });
  }
};

module.exports = { getPendingDistributors };
