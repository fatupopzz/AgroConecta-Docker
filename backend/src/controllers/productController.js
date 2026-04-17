const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const normalizeOptionalText = (value) => {
  if (value === undefined) return undefined;
  if (value === null) return null;
  if (typeof value !== "string") return null;

  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
};

const parseRating = (value) => {
  if (value === undefined) return { valid: true, parsed: undefined };

  const parsed = Number(value);
  const hasMaxTwoDecimals = /^\d+(\.\d{1,2})?$/.test(String(value));

  if (!Number.isFinite(parsed) || parsed < 0 || parsed > 5 || !hasMaxTwoDecimals) {
    return { valid: false };
  }

  return { valid: true, parsed };
};

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


const createProduct = async (req, res) => {
  const {
    id_categoria,
    nombre,
    marca,
    descripcion,
    composicion,
    dosis_recomendada,
    instrucciones_uso,
    calificacion_promedio,
  } = req.body;

  if (!id_categoria || !nombre) {
    return res.status(400).json({
      error: "Faltan campos obligatorios: id_categoria y nombre",
    });
  }

  if (!isPositiveInteger(id_categoria)) {
    return res.status(400).json({ error: "id_categoria inválido" });
  }

  if (typeof nombre !== "string" || nombre.trim().length < 2) {
    return res.status(400).json({ error: "Nombre inválido" });
  }

  const ratingValidation = parseRating(calificacion_promedio);
  if (!ratingValidation.valid) {
    return res.status(400).json({
      error:
        "calificacion_promedio inválida (debe ser un número entre 0 y 5 con máximo 2 decimales)",
    });
  }

  try {
    const categoriaExist = await pool.query(
      "SELECT 1 FROM categoria WHERE id_categoria = $1",
      [Number(id_categoria)],
    );

    if (categoriaExist.rows.length === 0) {
      return res.status(400).json({ error: "La categoría no existe" });
    }

    const baseValues = [
      Number(id_categoria),
      nombre.trim(),
      normalizeOptionalText(marca),
      normalizeOptionalText(descripcion),
      normalizeOptionalText(composicion),
      normalizeOptionalText(dosis_recomendada),
      normalizeOptionalText(instrucciones_uso),
    ];

    const newProduct =
      ratingValidation.parsed === undefined
        ? await pool.query(
            `INSERT INTO producto
             (id_categoria, nombre, marca, descripcion, composicion, dosis_recomendada, instrucciones_uso)
             VALUES ($1, $2, $3, $4, $5, $6, $7)
             RETURNING *`,
            baseValues,
          )
        : await pool.query(
            `INSERT INTO producto
             (id_categoria, nombre, marca, descripcion, composicion, dosis_recomendada, instrucciones_uso, calificacion_promedio)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
             RETURNING *`,
            [...baseValues, ratingValidation.parsed],
          );

    res.status(201).json({
      message: "Producto creado correctamente",
      producto: newProduct.rows[0],
    });

  } catch (error) {
    console.error("Error en createProduct:", error);
    return res.status(500).json({ error: "Error al crear producto" });
  }
};


const updateProduct = async (req, res) => {
  try {
    const { id } = req.params;

    if (!isPositiveInteger(id)) {
      return res.status(400).json({ error: "ID de producto inválido" });
    }

    const {
      id_categoria,
      nombre,
      marca,
      descripcion,
      composicion,
      dosis_recomendada,
      instrucciones_uso,
      calificacion_promedio,
    } = req.body;

    const fields = [];
    const values = [];

    if (id_categoria !== undefined) {
      if (!isPositiveInteger(id_categoria)) {
        return res.status(400).json({ error: "id_categoria inválido" });
      }

      const categoriaExist = await pool.query(
        "SELECT 1 FROM categoria WHERE id_categoria = $1",
        [Number(id_categoria)],
      );

      if (categoriaExist.rows.length === 0) {
        return res.status(400).json({ error: "La categoría no existe" });
      }

      values.push(Number(id_categoria));
      fields.push(`id_categoria = $${values.length}`);
    }

    if (nombre !== undefined) {
      if (typeof nombre !== "string" || nombre.trim().length < 2) {
        return res.status(400).json({ error: "Nombre inválido" });
      }

      values.push(nombre.trim());
      fields.push(`nombre = $${values.length}`);
    }

    const normalizedMarca = normalizeOptionalText(marca);
    if (marca !== undefined && normalizedMarca === null && marca !== null) {
      return res.status(400).json({ error: "marca debe ser texto" });
    }
    if (marca !== undefined) {
      values.push(normalizedMarca);
      fields.push(`marca = $${values.length}`);
    }

    const normalizedDescripcion = normalizeOptionalText(descripcion);
    if (
      descripcion !== undefined &&
      normalizedDescripcion === null &&
      descripcion !== null
    ) {
      return res.status(400).json({ error: "descripcion debe ser texto" });
    }
    if (descripcion !== undefined) {
      values.push(normalizedDescripcion);
      fields.push(`descripcion = $${values.length}`);
    }

    const normalizedComposicion = normalizeOptionalText(composicion);
    if (
      composicion !== undefined &&
      normalizedComposicion === null &&
      composicion !== null
    ) {
      return res.status(400).json({ error: "composicion debe ser texto" });
    }
    if (composicion !== undefined) {
      values.push(normalizedComposicion);
      fields.push(`composicion = $${values.length}`);
    }

    const normalizedDosis = normalizeOptionalText(dosis_recomendada);
    if (
      dosis_recomendada !== undefined &&
      normalizedDosis === null &&
      dosis_recomendada !== null
    ) {
      return res
        .status(400)
        .json({ error: "dosis_recomendada debe ser texto" });
    }
    if (dosis_recomendada !== undefined) {
      values.push(normalizedDosis);
      fields.push(`dosis_recomendada = $${values.length}`);
    }

    const normalizedInstrucciones = normalizeOptionalText(instrucciones_uso);
    if (
      instrucciones_uso !== undefined &&
      normalizedInstrucciones === null &&
      instrucciones_uso !== null
    ) {
      return res
        .status(400)
        .json({ error: "instrucciones_uso debe ser texto" });
    }
    if (instrucciones_uso !== undefined) {
      values.push(normalizedInstrucciones);
      fields.push(`instrucciones_uso = $${values.length}`);
    }

    if (calificacion_promedio !== undefined) {
      const ratingValidation = parseRating(calificacion_promedio);
      if (!ratingValidation.valid) {
        return res.status(400).json({
          error:
            "calificacion_promedio inválida (debe ser un número entre 0 y 5 con máximo 2 decimales)",
        });
      }

      values.push(ratingValidation.parsed);
      fields.push(`calificacion_promedio = $${values.length}`);
    }

    if (fields.length === 0) {
      return res.status(400).json({
        error: "No hay campos válidos para actualizar",
      });
    }

    values.push(Number(id));

    const updatedProduct = await pool.query(
      `UPDATE producto
       SET ${fields.join(", ")}
       WHERE id_producto = $${values.length} AND activo = true
       RETURNING *`,
      values,
    );

    if (updatedProduct.rowCount === 0) {
      return res.status(404).json({ error: "Producto no encontrado" });
    }

    res.json({
      message: "Producto actualizado correctamente",
      producto: updatedProduct.rows[0],
    });
  } catch (error) {
    console.error("Error en updateProduct:", error);
    return res.status(500).json({ error: "Error al actualizar producto" });
  }
};

const deleteProduct = async (req, res) => {
  try {
    const { id } = req.params;

    if (!isPositiveInteger(id)) {
      return res.status(400).json({ error: "ID de producto inválido" });
    }

    const deletedProduct = await pool.query(
      `UPDATE producto
       SET activo = false
       WHERE id_producto = $1 AND activo = true
       RETURNING *`,
      [Number(id)],
    );

    if (deletedProduct.rowCount === 0) {
      return res.status(404).json({ error: "Producto no encontrado" });
    }

    res.status(200).json({
      message: "Producto eliminado correctamente",
      producto: deletedProduct.rows[0],
    });
  } catch (err) {
    console.error("Error en deleteProduct:", err);
    return res.status(500).json({ error: "Error al eliminar producto" });
  }
};


module.exports = {
  getProducts,
  getProductById,
  createProduct,
  updateProduct,
  deleteProduct,
};