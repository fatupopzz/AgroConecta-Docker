const { pool } = require("../config/db");

const escapeLikePattern = (value) => value.replace(/[!%_]/g, "!$&");

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

class ProductRepository {
  constructor(db = pool) { this.db = db; }

  async findRecommended(userId) {
    const profileResult = await this.db.query(
      `SELECT departamento, cultivos_principales
       FROM agricultor
       WHERE id_usuario = $1`,
      [userId],
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

    const productsResult = await this.db.query(
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
              p.calificacion_promedio, p.imagen_url, p.imagen_thumb_url,
              c.nombre AS categoria,
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

    return productsResult.rows.slice(0, 10);
  }

  async findAll({ nombre, id_categoria, page: pageNumber, limit: limitNumber }) {
    const offset = (pageNumber - 1) * limitNumber;
    const params = [];
    let whereClause = " WHERE p.activo = true";
    const normalizedName = nombre?.trim() || "";
    const orderClause = normalizedName
      ? "ORDER BY LOWER(p.nombre) ASC, p.id_producto ASC"
      : "ORDER BY RANDOM()";

    if (normalizedName) {
      params.push(`%${escapeLikePattern(normalizedName)}%`);
      whereClause += ` AND p.nombre ILIKE $${params.length} ESCAPE '!'`;
  }

  if (id_categoria) {

    params.push(Number(id_categoria));
    whereClause += ` AND p.id_categoria = $${params.length}`;
  }

  const countResult = await this.db.query(
    `SELECT COUNT(*) AS total
     FROM producto p
     JOIN categoria c ON p.id_categoria = c.id_categoria
     ${whereClause}`,
    params,
  );

  const queryParams = [...params, limitNumber, offset];
  const result = await this.db.query(
    `SELECT p.id_producto, p.nombre, p.marca, p.descripcion,
            p.calificacion_promedio, p.imagen_url, p.imagen_thumb_url,
            c.nombre AS categoria,
            MIN(i.precio) AS precio_desde,
            COUNT(DISTINCT i.id_distribuidor) AS num_distribuidores
     FROM producto p
     JOIN categoria c ON p.id_categoria = c.id_categoria
     LEFT JOIN inventario_distribuidor i ON p.id_producto = i.id_producto
     ${whereClause}
     GROUP BY p.id_producto, p.nombre, p.marca, p.descripcion,
              p.calificacion_promedio, p.imagen_url, p.imagen_thumb_url, c.nombre
      ${orderClause}
     LIMIT $${queryParams.length - 1} OFFSET $${queryParams.length}`,
    queryParams,
  );

  return {
    page: pageNumber,
    limit: limitNumber,
    total: Number(countResult.rows[0].total),
    products: result.rows,
  };
  }

  async findById(id) {
    const producto = await this.db.query(
      `SELECT p.*, c.nombre AS categoria
       FROM producto p
       JOIN categoria c ON p.id_categoria = c.id_categoria
       WHERE p.id_producto = $1 AND p.activo = true`,
      [id],
    );
    return producto.rows;
  }

  async findOffers(id) {
    const ofertas = await this.db.query(
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
    return ofertas.rows;
  }

  async findForComparison(id) {
    const producto = await this.db.query(
      `SELECT p.id_producto, p.nombre, p.marca, p.descripcion,
              p.calificacion_promedio, p.imagen_url, c.id_categoria, c.nombre AS categoria
       FROM producto p
       JOIN categoria c ON p.id_categoria = c.id_categoria
       WHERE p.id_producto = $1 AND p.activo = true`,
      [id],
    );
    return producto.rows;
  }

  async findVerifiedOffers(id) {
    const distribuidoresResult = await this.db.query(
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
    return distribuidoresResult.rows;
  }

  async categoryExists(idCategoria) {
    const categoriaExist = await this.db.query(
      "SELECT 1 FROM categoria WHERE id_categoria = $1",
      [idCategoria],
    );
    return categoriaExist.rows.length > 0;
  }

  async create({ id_categoria, nombre, marca, descripcion, composicion, dosis_recomendada, instrucciones_uso, calificacion_promedio: rating }) {
    const baseValues = [id_categoria, nombre, marca, descripcion, composicion,
      dosis_recomendada, instrucciones_uso];
    const newProduct =
      rating === undefined
        ? await this.db.query(
            `INSERT INTO producto
             (id_categoria, nombre, marca, descripcion, composicion, dosis_recomendada, instrucciones_uso)
             VALUES ($1, $2, $3, $4, $5, $6, $7)
             RETURNING *`,
            baseValues,
          )
        : await this.db.query(
            `INSERT INTO producto
             (id_categoria, nombre, marca, descripcion, composicion, dosis_recomendada, instrucciones_uso, calificacion_promedio)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
             RETURNING *`,
            [...baseValues, rating],
          );
    return newProduct.rows[0];
  }

  async update(id, changes) {
    const columns = ["id_categoria", "nombre", "marca", "descripcion", "composicion",
      "dosis_recomendada", "instrucciones_uso", "calificacion_promedio"];
    const keys = columns.filter((key) => changes[key] !== undefined);
    if (keys.length === 0) return null;
    const values = keys.map((key) => changes[key]);
    const fields = keys.map((key, index) => `${key} = $${index + 1}`);
    values.push(Number(id));
    const updatedProduct = await this.db.query(
      `UPDATE producto
       SET ${fields.join(", ")}
       WHERE id_producto = $${values.length} AND activo = true
       RETURNING *`,
      values,
    );
    return updatedProduct.rows[0] || null;
  }

  async delete(id) {
    const deletedProduct = await this.db.query(
      `UPDATE producto
       SET activo = false
       WHERE id_producto = $1 AND activo = true
       RETURNING *`,
      [id],
    );
    return deletedProduct.rows;
  }

  async comparePrices(product_id) {
    const result = await this.db.query(
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
    return result.rows;
  }
}

module.exports = ProductRepository;
