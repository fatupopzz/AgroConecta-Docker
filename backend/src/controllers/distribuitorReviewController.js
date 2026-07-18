const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

/* =====================================
   POST /api/distribuidores/:id/reviews
===================================== */

const createDistributorReview = async (req, res) => {
  try {
    const { id } = req.params;
    const { calificacion, comentario } = req.body;

    if (!isPositiveInteger(id)) {
      return res.status(400).json({
        error: "ID de distribuidor inválido",
      });
    }

    if (
      calificacion === undefined ||
      Number(calificacion) < 1 ||
      Number(calificacion) > 5
    ) {
      return res.status(400).json({
        error: "La calificación debe estar entre 1 y 5",
      });
    }

    // id_usuario obtenido desde JWT
    const idUsuario = req.user.id;

    // Obtener agricultor asociado al usuario
    const agricultorResult = await pool.query(
      `SELECT id_agricultor
       FROM agricultor
       WHERE id_usuario = $1`,
      [idUsuario]
    );

    if (agricultorResult.rows.length === 0) {
      return res.status(404).json({
        error: "El usuario autenticado no es agricultor",
      });
    }

    const idAgricultor = agricultorResult.rows[0].id_agricultor;

    // Verificar que exista distribuidor
    const distribuidorResult = await pool.query(
      `SELECT id_distribuidor
       FROM distribuidor
       WHERE id_distribuidor = $1`,
      [id]
    );

    if (distribuidorResult.rows.length === 0) {
      return res.status(404).json({
        error: "Distribuidor no encontrado",
      });
    }

    const result = await pool.query(
      `INSERT INTO resena_distribuidor
      (id_agricultor, id_distribuidor, calificacion, comentario)
      VALUES ($1,$2,$3,$4)
      RETURNING *`,
      [
        idAgricultor,
        Number(id),
        Number(calificacion),
        comentario || null,
      ]
    );

    return res.status(201).json({
      message: "Reseña creada correctamente",
      review: result.rows[0],
    });

  } catch (error) {

    // Si ya calificó al distribuidor
    if (error.code === "23505") {
      return res.status(409).json({
        error: "Ya calificaste a este distribuidor",
      });
    }

    console.error("Error en createDistributorReview:", error);

    return res.status(500).json({
      error: "Error interno del servidor",
    });
  }
};

/* =====================================
   GET /api/distribuidores/:id/reviews
===================================== */

const getDistributorReviews = async (req, res) => {

  try {

    const { id } = req.params;

    if (!isPositiveInteger(id)) {
      return res.status(400).json({
        error: "ID de distribuidor inválido",
      });
    }

    const result = await pool.query(
      `SELECT
          r.id_resena,
          r.calificacion,
          r.comentario,
          r.fecha_resena,
          u.nombre AS agricultor
       FROM resena_distribuidor r
       JOIN agricultor a
            ON r.id_agricultor = a.id_agricultor
       JOIN usuario u
            ON a.id_usuario = u.id_usuario
       WHERE r.id_distribuidor = $1
       ORDER BY r.fecha_resena DESC`,
      [id]
    );

    return res.json({
      total: result.rowCount,
      reviews: result.rows,
    });

  } catch (error) {

    console.error("Error en getDistributorReviews:", error);

    return res.status(500).json({
      error: "Error al obtener reseñas",
    });
  }

};

/* =====================================
   Función auxiliar para promedio
===================================== */

const getDistributorRating = async (idDistribuidor) => {

  const result = await pool.query(
    `SELECT
        ROUND(AVG(calificacion),2) AS promedio,
        COUNT(*) AS total
     FROM resena_distribuidor
     WHERE id_distribuidor = $1`,
    [idDistribuidor]
  );

  return {
    promedio: result.rows[0].promedio || 0,
    total: Number(result.rows[0].total),
  };

};

module.exports = {
  createDistributorReview,
  getDistributorReviews,
  getDistributorRating,
};