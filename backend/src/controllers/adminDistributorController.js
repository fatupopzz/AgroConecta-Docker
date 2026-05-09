const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

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

/**
 * PATCH /api/admin/distributors/:id/verify
 * Actualiza estado_verificacion a 'verificado' y registra fecha_verificacion.
 * Requiere middleware verifyAdmin.
 */
const verifyDistributor = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      `UPDATE distribuidor
       SET estado_verificacion = 'verificado',
           fecha_verificacion = NOW()
       WHERE id_distribuidor = $1
       RETURNING id_distribuidor, nombre_negocio, estado_verificacion, fecha_verificacion`,
      [Number(id)]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    return res.json({
      message: "Distribuidor verificado correctamente",
      distribuidor: result.rows[0],
    });
  } catch (error) {
    console.error("Error en verifyDistributor:", error);
    return res.status(500).json({ error: "Error al verificar distribuidor" });
  }
};

module.exports = { getPendingDistributors, verifyDistributor };
