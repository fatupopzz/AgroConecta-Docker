const { pool } = require("../config/db");

// GET /api/products
const getProducts = async (req, res) => {
  try {
    const { nombre, id_categoria, page = 1, limit = 10 } = req.query;
    const offset = (page - 1) * limit;

    let query = `
      SELECT p.id_producto, p.nombre, p.marca, p.descripcion,
             p.calificacion_promedio, c.nombre AS categoria,
             i.precio, i.stock_disponible, i.unidad_medida,
             d.nombre_negocio AS distribuidor
      FROM producto p
      JOIN categoria c ON p.id_categoria = c.id_categoria
      JOIN inventario_distribuidor i ON p.id_producto = i.id_producto
      JOIN distribuidor d ON i.id_distribuidor = d.id_distribuidor
      WHERE p.activo = true
    `;

    const params = [];

    if (nombre) {
      params.push(`%${nombre}%`);
      query += ` AND p.nombre ILIKE $${params.length}`;
    }

    if (id_categoria) {
      params.push(id_categoria);
      query += ` AND p.id_categoria = $${params.length}`;
    }

    params.push(limit);
    query += ` LIMIT $${params.length}`;

    params.push(offset);
    query += ` OFFSET $${params.length}`;

    const result = await pool.query(query, params);

    res.json({
      page: Number(page),
      limit: Number(limit),
      total: result.rowCount,
      products: result.rows,
    });
  } catch (error) {
    console.error("Error en getProducts:", error);
    res.status(500).json({ error: "Error al obtener productos" });
  }
};

// GET /api/products/:id
const getProductById = async (req, res) => {
  try {
    const { id } = req.params;

    const result = await pool.query(
      `SELECT p.*, c.nombre AS categoria,
              i.precio, i.stock_disponible, i.unidad_medida,
              d.nombre_negocio AS distribuidor,
              d.calificacion_promedio AS calificacion_distribuidor
       FROM producto p
       JOIN categoria c ON p.id_categoria = c.id_categoria
       JOIN inventario_distribuidor i ON p.id_producto = i.id_producto
       JOIN distribuidor d ON i.id_distribuidor = d.id_distribuidor
       WHERE p.id_producto = $1 AND p.activo = true`,
      [id],
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Producto no encontrado" });
    }

    res.json(result.rows[0]);
  } catch (error) {
    console.error("Error en getProductById:", error);
    res.status(500).json({ error: "Error al obtener el producto" });
  }
};

// GET /api/categories
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
