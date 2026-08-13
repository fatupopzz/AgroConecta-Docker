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

const RELEVANT_CROP_CATEGORIES = [
  "Semillas",
  "Fertilizantes",
  "Pesticidas",
  "Herbicidas",
];

const parseCropTerms = (crops) =>
  String(crops || "")
    .split(/[,;\n/]+/)
    .map((crop) => crop.trim().toLowerCase())
    .filter((crop) => crop.length >= 2)
    .slice(0, 20);

const getRecommendedProducts = async (req, res) => {
  try {
    if (req.user.tipo !== "agricultor") {
      return res.status(403).json({
        error: "Las recomendaciones personalizadas son solo para agricultores",
      });
    }

    const profileResult = await pool.query(
      `SELECT departamento, cultivos_principales
       FROM agricultor
       WHERE id_usuario = $1`,
      [req.user.id],
    );

    const profile = profileResult.rows[0];
    const department = profile?.departamento?.trim() || null;
    const cropTerms = parseCropTerms(profile?.cultivos_principales);
    const hasCompleteProfile = Boolean(department && cropTerms.length > 0);

    const params = hasCompleteProfile
      ? [RELEVANT_CROP_CATEGORIES, cropTerms, department]
      : [];

    const personalizationFilter = hasCompleteProfile
      ? "AND c.nombre = ANY($1::text[])"
      : "";
    const cropMatchExpression = hasCompleteProfile
      ? `EXISTS (
           SELECT 1
           FROM unnest($2::text[]) AS crop(term)
           WHERE LOWER(CONCAT_WS(' ', p.nombre, p.descripcion, p.composicion))
                 LIKE '%' || crop.term || '%'
         )`
      : "FALSE";
    const localInventoryExpression = hasCompleteProfile
      ? "COALESCE(inv.has_local_inventory, FALSE)"
      : "FALSE";

    const productsResult = await pool.query(
      `WITH sales AS (
         SELECT inventory.id_producto, COALESCE(SUM(detail.cantidad), 0) AS units_sold
         FROM detalle_pedido detail
         JOIN inventario_distribuidor inventory
           ON inventory.id_inventario = detail.id_inventario
         JOIN pedido orders ON orders.id_pedido = detail.id_pedido
         WHERE orders.estado = 'entregado'
         GROUP BY inventory.id_producto
       ), inventory_summary AS (
         SELECT inventory.id_producto,
                MIN(inventory.precio) FILTER (WHERE inventory.stock_disponible > 0) AS precio_desde,
                COUNT(DISTINCT inventory.id_distribuidor)
                  FILTER (WHERE inventory.stock_disponible > 0) AS num_distribuidores,
                ${hasCompleteProfile
                  ? "BOOL_OR(inventory.stock_disponible > 0 AND LOWER(distributor.departamento) = LOWER($3))"
                  : "FALSE"} AS has_local_inventory
         FROM inventario_distribuidor inventory
         JOIN distribuidor distributor
           ON distributor.id_distribuidor = inventory.id_distribuidor
         GROUP BY inventory.id_producto
       )
       SELECT p.id_producto, p.nombre, p.marca, p.descripcion,
              p.composicion, p.dosis_recomendada, p.instrucciones_uso,
              p.calificacion_promedio, c.nombre AS categoria,
              inv.precio_desde,
              COALESCE(inv.num_distribuidores, 0)::int AS num_distribuidores
       FROM producto p
       JOIN categoria c ON c.id_categoria = p.id_categoria
       LEFT JOIN sales ON sales.id_producto = p.id_producto
       LEFT JOIN inventory_summary inv ON inv.id_producto = p.id_producto
       WHERE p.activo = TRUE
         ${personalizationFilter}
       ORDER BY
         CASE WHEN ${cropMatchExpression} THEN 1 ELSE 0 END DESC,
         CASE WHEN ${localInventoryExpression} THEN 1 ELSE 0 END DESC,
         COALESCE(sales.units_sold, 0) DESC,
         p.calificacion_promedio DESC,
         p.id_producto ASC
       LIMIT 10`,
      params,
    );

    return res.json(productsResult.rows.slice(0, 10));
  } catch (error) {
    console.error("Error en getRecommendedProducts:", error);
    return res.status(500).json({ error: "Error al obtener productos recomendados" });
  }
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
       ORDER BY RANDOM()
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
              d.calificacion_promedio AS calificacion_distribuidor,
              d.estado_verificacion
       FROM inventario_distribuidor i
       JOIN distribuidor d ON i.id_distribuidor = d.id_distribuidor
       WHERE i.id_producto = $1
         AND i.stock_disponible > 0
       ORDER BY i.precio ASC`,
      [id],
    );

    res.json({
      ...producto.rows[0],
      unidad_medida: ofertas.rows.length > 0 ? ofertas.rows[0].unidad_medida : null,
      ofertas: ofertas.rows,
    });
  } catch (error) {
    console.error("Error en getProductById:", error);
    res.status(500).json({ error: "Error al obtener el producto" });
  }
};

const getProductComparison = async (req, res) => {
  try {
    if (!isPositiveInteger(req.params.id)) {
      return res.status(400).json({ error: "ID de producto inválido" });
    }

    const id = Number(req.params.id);

    const producto = await pool.query(
      `SELECT p.id_producto, p.nombre, p.marca, p.descripcion,
              p.calificacion_promedio, c.id_categoria, c.nombre AS categoria
       FROM producto p
       JOIN categoria c ON p.id_categoria = c.id_categoria
       WHERE p.id_producto = $1 AND p.activo = true`,
      [id],
    );

    if (producto.rows.length === 0) {
      return res.status(404).json({ error: "Producto no encontrado" });
    }

    const distribuidoresResult = await pool.query(
      `SELECT d.id_distribuidor,
              d.nombre_negocio AS nombre,
              i.precio,
              i.stock_disponible,
              i.unidad_medida,
              d.calificacion_promedio AS calificacion_distribuidor
       FROM inventario_distribuidor i
       JOIN distribuidor d ON i.id_distribuidor = d.id_distribuidor
       WHERE i.id_producto = $1
         AND d.estado_verificacion = 'verificado'
         AND i.stock_disponible > 0
       ORDER BY i.precio ASC`,
      [id],
    );

    const distribuidores = distribuidoresResult.rows;
    const precioMasBajo = distribuidores.length > 0 ? distribuidores[0].precio : null;

    const distribuidoresConBandera = distribuidores.map((distribuidor) => ({
      ...distribuidor,
      es_precio_mas_bajo:
        precioMasBajo !== null && Number(distribuidor.precio) === Number(precioMasBajo),
    }));

    return res.json({
      ...producto.rows[0],
      precio_mas_bajo: precioMasBajo,
      distribuidores: distribuidoresConBandera,
    });
  } catch (error) {
    console.error("Error en getProductComparison:", error);
    return res.status(500).json({ error: "Error al comparar precios del producto" });
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

const comparePrices = async (req, res) => {
  try {
    const { product_id } = req.query;

    if (!product_id) {
      return res.status(400).json({
        error: "product_id es requerido",
      });
    }

    const result = await pool.query(
      `SELECT 
        d.id_distribuidor,
        d.nombre_negocio,
        i.precio,
        i.stock_disponible,
        d.calificacion_promedio,
        i.tiempo_entrega_dias
       FROM inventario_distribuidor i
       JOIN distribuidor d ON i.id_distribuidor = d.id_distribuidor
       WHERE i.id_producto = $1
         AND i.stock_disponible > 0
         AND d.estado_verificacion = 'verificado'
       ORDER BY i.precio ASC`,
      [product_id]
    );

    if (result.rows.length === 0) {
      return res.json({
        message: "No hay distribuidores disponibles",
        data: [],
      });
    }

    res.json(result.rows);

  } catch (error) {
    console.error("Error en comparePrices:", error);
    res.status(500).json({ error: "Error en servidor" });
  }
};


module.exports = {
  getProducts,
  getRecommendedProducts,
  getProductById,
  getProductComparison,
  createProduct,
  updateProduct,
  deleteProduct,
  comparePrices
};
