const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));
const isNonNegativeInteger = (value) => /^\d+$/.test(String(value));

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
      if (!isPositiveInteger(id_categoria)) {
        return res.status(400).json({ error: "id_categoria inválido" });
      }
      params.push(Number(id_categoria));
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
    if (!isPositiveInteger(req.params.id)) {
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
      `SELECT i.id_inventario, i.precio, i.stock_disponible, i.unidad_medida,
              d.id_distribuidor, d.nombre_negocio AS distribuidor,
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

const createProduct = async (req, res) => {
  const {
    id_distribuidor,
    id_categoria,
    nombre,
    precio,
    stock_disponible,
    unidad_medida,
    marca,
    descripcion,
    composicion,
    dosis_recomendada,
    instrucciones_uso,
  } = req.body;

  if (
    !id_distribuidor ||
    !id_categoria ||
    !nombre ||
    precio === undefined ||
    stock_disponible === undefined ||
    !unidad_medida
  ) {
    return res.status(400).json({
      error:
        "Campos requeridos: id_distribuidor, id_categoria, nombre, precio, stock_disponible y unidad_medida",
    });
  }

  if (!isPositiveInteger(id_distribuidor)) {
    return res.status(400).json({ error: "id_distribuidor inválido" });
  }

  if (!isPositiveInteger(id_categoria)) {
    return res.status(400).json({ error: "id_categoria inválido" });
  }

  if (typeof nombre !== "string" || nombre.trim().length < 2) {
    return res.status(400).json({ error: "Nombre de producto inválido" });
  }

  const precioNumber = Number(precio);
  if (!Number.isFinite(precioNumber) || precioNumber <= 0) {
    return res.status(400).json({ error: "Precio inválido" });
  }

  if (!isNonNegativeInteger(stock_disponible)) {
    return res.status(400).json({ error: "Stock inválido" });
  }

  if (typeof unidad_medida !== "string" || unidad_medida.trim().length < 1) {
    return res.status(400).json({ error: "Unidad de medida inválida" });
  }

  let client;

  try {
    client = await pool.connect();
    await client.query("BEGIN");

    const distributorResult = await client.query(
      `SELECT id_distribuidor, estado_verificacion, nombre_negocio
       FROM distribuidor
       WHERE id_distribuidor = $1`,
      [Number(id_distribuidor)],
    );

    if (distributorResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    if (distributorResult.rows[0].estado_verificacion !== "verificado") {
      await client.query("ROLLBACK");
      return res.status(403).json({
        error: "Solo distribuidores verificados pueden publicar productos",
      });
    }

    const categoryResult = await client.query(
      `SELECT id_categoria, nombre
       FROM categoria
       WHERE id_categoria = $1`,
      [Number(id_categoria)],
    );

    if (categoryResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Categoría no encontrada" });
    }

    const productResult = await client.query(
      `INSERT INTO producto
        (id_categoria, nombre, marca, descripcion, composicion, dosis_recomendada, instrucciones_uso)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING id_producto, id_categoria, nombre, marca, descripcion, composicion,
                 dosis_recomendada, instrucciones_uso, activo, calificacion_promedio`,
      [
        Number(id_categoria),
        nombre.trim(),
        marca || null,
        descripcion || null,
        composicion || null,
        dosis_recomendada || null,
        instrucciones_uso || null,
      ],
    );

    const producto = productResult.rows[0];

    const inventoryResult = await client.query(
      `INSERT INTO inventario_distribuidor
        (id_distribuidor, id_producto, precio, stock_disponible, unidad_medida)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id_inventario, id_distribuidor, id_producto, precio, stock_disponible,
                 unidad_medida, ultima_actualizacion`,
      [
        Number(id_distribuidor),
        producto.id_producto,
        precioNumber,
        Number(stock_disponible),
        unidad_medida.trim(),
      ],
    );

    await client.query("COMMIT");

    return res.status(201).json({
      message: "Producto publicado correctamente",
      producto,
      inventario: inventoryResult.rows[0],
    });
  } catch (error) {
    if (client) {
      try {
        await client.query("ROLLBACK");
      } catch (rollbackError) {
        console.error("Error en ROLLBACK createProduct:", rollbackError);
      }
    }

    console.error("Error en createProduct:", error);
    return res.status(500).json({ error: "Error al publicar producto" });
  } finally {
    if (client) {
      client.release();
    }
  }
};

const updateInventory = async (req, res) => {
  const { id } = req.params;
  const { precio, stock_disponible } = req.body;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de inventario inválido" });
  }

  if (precio === undefined && stock_disponible === undefined) {
    return res.status(400).json({
      error: "Debe enviar al menos precio o stock_disponible para actualizar",
    });
  }

  const precioNumber = precio !== undefined ? Number(precio) : undefined;

  if (precio !== undefined) {
    if (!Number.isFinite(precioNumber) || precioNumber <= 0) {
      return res.status(400).json({ error: "Precio inválido" });
    }
  }

  if (stock_disponible !== undefined) {
    if (!isNonNegativeInteger(stock_disponible)) {
      return res.status(400).json({ error: "Stock inválido" });
    }
  }

  try {
    const result = await pool.query(
      `UPDATE inventario_distribuidor
       SET precio = COALESCE($2, precio),
           stock_disponible = COALESCE($3, stock_disponible),
           ultima_actualizacion = NOW()
       WHERE id_inventario = $1
       RETURNING id_inventario, id_distribuidor, id_producto, precio,
                 stock_disponible, unidad_medida, ultima_actualizacion`,
      [
        Number(id),
        precio !== undefined ? precioNumber : null,
        stock_disponible !== undefined ? Number(stock_disponible) : null,
      ],
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Inventario no encontrado" });
    }

    return res.json({
      message: "Inventario actualizado correctamente",
      inventario: result.rows[0],
    });
  } catch (error) {
    console.error("Error en updateInventory:", error);
    return res.status(500).json({ error: "Error al actualizar inventario" });
  }
};

const getDistributorProducts = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de distribuidor inválido" });
  }

  try {
    const distributorResult = await pool.query(
      `SELECT id_distribuidor, nombre_negocio, departamento
       FROM distribuidor
       WHERE id_distribuidor = $1
         AND estado_verificacion = 'verificado'`,
      [Number(id)],
    );

    if (distributorResult.rows.length === 0) {
      return res.status(404).json({
        error: "Distribuidor no encontrado o no verificado",
      });
    }

    const productsResult = await pool.query(
      `SELECT i.id_inventario,
              p.id_producto,
              p.nombre,
              p.marca,
              p.descripcion,
              c.nombre AS categoria,
              i.precio,
              i.stock_disponible,
              i.unidad_medida,
              i.ultima_actualizacion
       FROM inventario_distribuidor i
       JOIN producto p ON i.id_producto = p.id_producto
       LEFT JOIN categoria c ON p.id_categoria = c.id_categoria
       WHERE i.id_distribuidor = $1
         AND p.activo = true
       ORDER BY i.id_inventario ASC`,
      [Number(id)],
    );

    return res.json({
      distribuidor: distributorResult.rows[0],
      total_productos: productsResult.rows.length,
      products: productsResult.rows,
    });
  } catch (error) {
    console.error("Error en getDistributorProducts:", error);
    return res
      .status(500)
      .json({ error: "Error al obtener productos del distribuidor" });
  }
};

module.exports = {
  getProducts,
  getProductById,
  getCategories,
  createProduct,
  updateInventory,
  getDistributorProducts,
};