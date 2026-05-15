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
    const promedio = total > 0
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
  const { id_agricultor, id_pedido, calificacion, comentario } = req.body;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de producto inválido" });
  }
  if (!isPositiveInteger(id_agricultor)) {
    return res.status(400).json({ error: "id_agricultor inválido" });
  }
  if (!isPositiveInteger(id_pedido)) {
    return res.status(400).json({ error: "id_pedido inválido" });
  }

  const cal = Number(calificacion);
  if (!Number.isInteger(cal) || cal < 1 || cal > 5) {
    return res.status(400).json({ error: "calificacion debe ser entre 1 y 5" });
  }

  try {
    // Verificar que el pedido pertenece al agricultor y contiene el producto
    const pedidoCheck = await pool.query(
      `SELECT p.id_pedido
       FROM pedido p
       JOIN detalle_pedido dp ON p.id_pedido = dp.id_pedido
       JOIN inventario_distribuidor i ON dp.id_inventario = i.id_inventario
       WHERE p.id_pedido = $1
         AND p.id_agricultor = $2
         AND i.id_producto = $3
         AND p.estado = 'entregado'`,
      [Number(id_pedido), Number(id_agricultor), Number(id)]
    );

    if (pedidoCheck.rows.length === 0) {
      return res.status(403).json({
        error: "Solo puedes reseñar productos de pedidos entregados",
      });
    }

    const result = await pool.query(
      `INSERT INTO resena (id_agricultor, id_producto, id_pedido, calificacion, comentario)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id_resena, calificacion, comentario, fecha_resena`,
      [
        Number(id_agricultor),
        Number(id),
        Number(id_pedido),
        cal,
        comentario?.trim() || null,
      ]
    );

    // Actualizar calificacion_promedio en producto
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
      return res.status(400).json({
        error: "Ya existe una reseña para este producto en este pedido",
      });
    }
    console.error("Error en createResena:", error);
    return res.status(500).json({ error: "Error al crear reseña" });
  }
};

module.exports = { getResenasByProducto, createResena };
