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

/**
 * PATCH /api/admin/distributors/:id/reject
 * Actualiza estado_verificacion a 'suspendido' con motivo opcional.
 * Requiere middleware verifyAdmin.
 *
 * Body opcional: { motivo: string }
 *
 * Nota: el CHECK constraint actual acepta 'pendiente' | 'verificado' | 'suspendido'.
 * Se usa 'suspendido' como estado de rechazo. Ver Batch 8 (opcional) si el equipo
 * quiere agregar 'rechazado' como valor propio.
 */
const rejectDistributor = async (req, res) => {
  const { id } = req.params;
  const { motivo } = req.body;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  const motivoNormalizado =
    motivo !== undefined && typeof motivo === "string" && motivo.trim().length > 0
      ? motivo.trim()
      : null;

  try {
    const existing = await pool.query(
      `SELECT id_distribuidor, estado_verificacion FROM distribuidor WHERE id_distribuidor = $1`,
      [Number(id)]
    );

    if (existing.rows.length === 0) {
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    if (existing.rows[0].estado_verificacion === "verificado") {
      return res.status(400).json({
        error: "No se puede rechazar un distribuidor ya verificado",
      });
    }

    const result = await pool.query(
      `UPDATE distribuidor
       SET estado_verificacion = 'suspendido',
           fecha_verificacion = NOW()
       WHERE id_distribuidor = $1
       RETURNING id_distribuidor, nombre_negocio, estado_verificacion, fecha_verificacion`,
      [Number(id)]
    );

    return res.json({
      message: "Distribuidor rechazado",
      motivo: motivoNormalizado,
      distribuidor: result.rows[0],
    });
  } catch (error) {
    console.error("Error en rejectDistributor:", error);
    return res.status(500).json({ error: "Error al rechazar distribuidor" });
  }
};

module.exports = {
  getPendingDistributors,
  verifyDistributor,
  rejectDistributor,
};
