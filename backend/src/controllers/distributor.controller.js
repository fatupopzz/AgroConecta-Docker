/**
 * @file controllers/distributor.controller.js
 * @description Controladores para el módulo de distribuidores.
 */

const { pool } = require("../config/db");

/**
 * PATCH /api/admin/distributors/:id/verify
 *
 * Actualiza el estado de verificación de un distribuidor.
 * Solo un administrador puede ejecutar esta acción.
 *
 * Estados válidos:
 *  - "verificado"  → el distribuidor aparece en el catálogo público
 *  - "suspendido"  → el distribuidor deja de aparecer en el catálogo
 *
 * @route   PATCH /api/admin/distributors/:id/verify
 * @access  Privado (administrador)
 *
 * @param  {number} id                  - ID del distribuidor (id_distribuidor)
 * @body   {string} estado_verificacion - Nuevo estado: "verificado" | "suspendido"
 *
 * @returns {200} Estado actualizado exitosamente
 * @returns {400} ID o estado no válido
 * @returns {404} Distribuidor no encontrado
 * @returns {500} Error interno del servidor
 */
const verifyDistributor = async (req, res) => {
  if (!/^[1-9]\d*$/.test(req.params.id)) {
    return res.status(400).json({ error: "ID de distribuidor inválido" });
  }
  const id = Number(req.params.id);
  const { estado_verificacion } = req.body;

  const estadosValidos = ["verificado", "suspendido"];
  if (!estado_verificacion || !estadosValidos.includes(estado_verificacion)) {
    return res.status(400).json({
      error: `Estado no válido. Use: ${estadosValidos.join(" | ")}`,
    });
  }

  try {
    const result = await pool.query(
      `UPDATE distribuidor
       SET estado_verificacion = $1,
           fecha_verificacion  = CASE WHEN $1 = 'verificado' THEN NOW() ELSE fecha_verificacion END
       WHERE id_distribuidor = $2
       RETURNING id_distribuidor, estado_verificacion, fecha_verificacion`,
      [estado_verificacion, id]
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    res.status(200).json({
      message: `Distribuidor ${estado_verificacion} exitosamente.`,
      distribuidor: result.rows[0],
    });
  } catch (error) {
    console.error("Error en verifyDistributor:", error);
    res.status(500).json({ error: "Error interno del servidor" });
  }
};

/**
 * GET /api/distributors
 *
 * Retorna la lista de distribuidores verificados únicamente.
 * Este es el catálogo público visible para los agricultores.
 * Distribuidores con estado "pendiente" o "suspendido" no aparecen.
 *
 * @route   GET /api/distributors
 * @access  Público
 *
 * @returns {200} Lista de distribuidores verificados
 * @returns {500} Error interno del servidor
 */
const getVerifiedDistributors = async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT
         d.id_distribuidor,
         d.nombre_negocio,
         d.nit,
         d.departamento,
         d.calificacion_promedio,
         d.fecha_verificacion,
         u.nombre,
         u.telefono,
         u.email
       FROM distribuidor d
       JOIN usuario u ON u.id_usuario = d.id_usuario
       WHERE d.estado_verificacion = 'verificado'
       ORDER BY d.nombre_negocio ASC`
    );

    res.status(200).json({
      total: result.rowCount,
      distribuidores: result.rows,
    });
  } catch (error) {
    console.error("Error en getVerifiedDistributors:", error);
    res.status(500).json({ error: "Error interno del servidor" });
  }
};

module.exports = { verifyDistributor, getVerifiedDistributors };