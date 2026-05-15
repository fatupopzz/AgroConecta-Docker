const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

// GET /api/products/:id/reviews
const getResenasByProducto = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de producto inválido" });
  }

  try {
    const result = await pool.query(
      `SELECT r.id_resena, r.calificacion, r.comentario, r.fecha_resena,
              u.nombre AS agricultor_nombre
       FROM resena r
       JOIN agricultor a ON r.id_agricultor = a.id_agricultor
       JOIN usuario u ON a.id_usuario = u.id_usuario
       WHERE r.id_producto = $1
       ORDER BY r.fecha_resena DESC`,
      [Number(id)]
    );

    const total = result.rows.length;
    const promedio =
      total > 0
        ? result.rows.reduce((sum, r) => sum + r.calificacion, 0) / total
        : 0;

    return res.json({
      promedio: Number(promedio.toFixed(2)),
      total,
      resenas: result.rows,
    });
  } catch (error) {
    console.error("Error en getResenasByProducto:", error);
    return res.status(500).json({ error: "Error al obtener reseñas" });
  }
};

// POST /api/products/:id/reviews
const createResena = async (req, res) => {
  const { id } = req.params;
  const { calificacion, comentario } = req.body;  // ya no necesitamos id_pedido
  const idUsuario = req.user?.id;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de producto inválido" });
  }
  if (!idUsuario) {
    return res.status(401).json({ error: "Usuario autenticado inválido" });
  }

  const cal = Number(calificacion);
  if (!Number.isInteger(cal) || cal < 1 || cal > 5) {
    return res.status(400).json({ error: "calificacion debe ser entre 1 y 5" });
  }

  try {
    // Verificar que sea agricultor
    const agricultorResult = await pool.query(
      `SELECT id_agricultor FROM agricultor WHERE id_usuario = $1`,
      [Number(idUsuario)]
    );

    if (agricultorResult.rows.length === 0) {
      return res.status(403).json({ error: "Solo agricultores pueden dejar reseñas" });
    }

    const id_agricultor = agricultorResult.rows[0].id_agricultor;

    // Insertar reseña (sin id_pedido)
    const result = await pool.query(
      `INSERT INTO resena (id_agricultor, id_producto, calificacion, comentario)
       VALUES ($1, $2, $3, $4)
       RETURNING id_resena, calificacion, comentario, fecha_resena`,
      [id_agricultor, Number(id), cal, comentario?.trim() || null]
    );

    // Actualizar promedio del producto
    await pool.query(
      `UPDATE producto
       SET calificacion_promedio = (
         SELECT AVG(calificacion) FROM resena WHERE id_producto = $1
       )
       WHERE id_producto = $1`,
      [Number(id)]
    );

    return res.status(201).json({
      message: "Reseña creada correctamente",
      resena: result.rows[0],
    });
  } catch (error) {
    if (error.code === "23505") {
      return res.status(409).json({
        error: "Ya dejaste una reseña para este producto",
      });
    }
    console.error("Error en createResena:", error);
    return res.status(500).json({ error: "Error al crear reseña" });
  }
};

module.exports = { getResenasByProducto, createResena };
