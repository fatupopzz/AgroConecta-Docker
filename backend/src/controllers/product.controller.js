const { pool } = require("../config/db");

const getProducts = async (req, res) => {
  try {
    const { nombre, id_categoria, page = 1, limit = 10 } = req.query;

    const pageNumber = parseInt(page);
    const limitNumber = parseInt(limit);

    if (
      isNaN(pageNumber) ||
      pageNumber < 1 ||
      isNaN(limitNumber) ||
      limitNumber < 1 ||
      limitNumber > 100
    ) {
      return res
        .status(400)
        .json({ error: "Parámetros de paginación inválidos" });
    }

    const offset = (pageNumber - 1) * limitNumber;
    const params = [];
    let whereClause = " WHERE p.activo = true";

    if (nombre) {
      params.push(`%${nombre}%`);
      whereClause += ` AND p.nombre ILIKE $${params.length}`;
    }

    if (id_categoria) {
      if (!/^[1-9]\d*$/.test(id_categoria)) {
        return res.status(400).json({ error: "id_categoria inválido" });
      }
      const catId = Number(id_categoria);
      params.push(catId);
      whereClause += ` AND p.id_categoria = $${params.length}`;
    }

    const countResult = await pool.query(
      `SELECT COUNT(*) AS total
       FROM producto p
       JOIN categoria c ON p.id_categoria = c.id_categoria
       ${whereClause}`,
      params,
    );

    const queryParams = [...params, limitNumber, offset];
    const result = await pool.query(
      `SELECT p.id_producto, p.nombre, p.marca, p.descripcion,
              p.calificacion_promedio, c.nombre AS categoria,
              MIN(i.precio) AS precio_desde,
              COUNT(DISTINCT i.id_distribuidor) AS num_distribuidores
       FROM producto p
       JOIN categoria c ON p.id_categoria = c.id_categoria
       LEFT JOIN inventario_distribuidor i ON p.id_producto = i.id_producto
       ${whereClause}
       GROUP BY p.id_producto, p.nombre, p.marca, p.descripcion,
                p.calificacion_promedio, c.nombre
       ORDER BY p.id_producto ASC
       LIMIT $${queryParams.length - 1} OFFSET $${queryParams.length}`,
      queryParams,
    );

    res.json({
      page: pageNumber,
      limit: limitNumber,
      total: Number(countResult.rows[0].total),
      products: result.rows,
    });
  } catch (error) {
    console.error("Error en getProducts:", error);
    res.status(500).json({ error: "Error al obtener productos" });
  }
};

const getProductById = async (req, res) => {
  try {
    if (!/^[1-9]\d*$/.test(req.params.id)) {
      return res.status(400).json({ error: "ID de producto inválido" });
    }
    const id = Number(req.params.id);

    const producto = await pool.query(
      `SELECT p.*, c.nombre AS categoria
       FROM producto p
       JOIN categoria c ON p.id_categoria = c.id_categoria
       WHERE p.id_producto = $1 AND p.activo = true`,
      [id],
    );

    if (producto.rows.length === 0) {
      return res.status(404).json({ error: "Producto no encontrado" });
    }

    const ofertas = await pool.query(
      `SELECT i.precio, i.stock_disponible, i.unidad_medida,
              d.nombre_negocio AS distribuidor,
              d.calificacion_promedio AS calificacion_distribuidor
       FROM inventario_distribuidor i
       JOIN distribuidor d ON i.id_distribuidor = d.id_distribuidor
       WHERE i.id_producto = $1
       ORDER BY i.precio ASC`,
      [id],
    );

    res.json({
      ...producto.rows[0],
      ofertas: ofertas.rows,
    });
  } catch (error) {
    console.error("Error en getProductById:", error);
    res.status(500).json({ error: "Error al obtener el producto" });
  }
};

const getCategories = async (req, res) => {
  try {
    const result = await pool.query(
      "SELECT * FROM categoria ORDER BY nombre ASC",
    );
    res.json(result.rows);
  } catch (error) {
    console.error("Error en getCategories:", error);
    res.status(500).json({ error: "Error al obtener categorías" });
  }
};

module.exports = { getProducts, getProductById, getCategories };
