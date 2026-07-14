const { pool } = require("../config/db");
const { PrecioNotificacionService } = require("../services/PrecioNotificacionService");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));
const precioNotificacionService = new PrecioNotificacionService();

const notifyPriceDropSafely = async (idProducto, precioAnterior, precioNuevo) => {
  try {
    await precioNotificacionService.verificarYNotificarBajaDePrecio(
      idProducto,
      precioAnterior,
      precioNuevo
    );
  } catch (error) {
    console.error("Error al generar notificaciones de baja de precio:", error);
  }
};

const getDistributorInventory = async (req, res) => {
  const idUsuario = req.user?.id;
  const tipoUsuario = req.user?.tipo;

  if (tipoUsuario !== "distribuidor") {
    return res.status(403).json({ error: "Solo distribuidores pueden ver su inventario" });
  }

  try {
    const distResult = await pool.query(
      `SELECT id_distribuidor FROM distribuidor WHERE id_usuario = $1`,
      [Number(idUsuario)]
    );

    if (distResult.rows.length === 0) {
      return res.status(404).json({ error: "Perfil de distribuidor no encontrado" });
    }

    const { id_distribuidor } = distResult.rows[0];

    const result = await pool.query(
      `SELECT i.id_inventario,
              i.id_producto,
              p.nombre AS producto,
              p.marca,
              c.nombre AS categoria,
              i.precio,
              i.stock_disponible,
              i.unidad_medida,
              i.tiempo_entrega_dias,
              i.ultima_actualizacion
       FROM inventario_distribuidor i
       JOIN producto p ON i.id_producto = p.id_producto
       LEFT JOIN categoria c ON p.id_categoria = c.id_categoria
       WHERE i.id_distribuidor = $1
       ORDER BY i.ultima_actualizacion DESC, p.nombre ASC`,
      [id_distribuidor]
    );

    return res.json({
      distribuidor: {
        id_distribuidor: Number(id_distribuidor),
        id_usuario: Number(idUsuario),
      },
      inventario: result.rows,
    });
  } catch (error) {
    console.error("Error en getDistributorInventory:", error);
    return res.status(500).json({ error: "Error al obtener inventario" });
  }
};

// POST /api/inventory
// Requiere token de distribuidor verificado
const createInventory = async (req, res) => {
  const idUsuario = req.user?.id;
  const tipoUsuario = req.user?.tipo;

  if (tipoUsuario !== "distribuidor") {
    return res.status(403).json({ error: "Solo distribuidores pueden publicar productos" });
  }

  const { id_producto, precio, stock_disponible, unidad_medida, tiempo_entrega_dias } = req.body;

  if (!id_producto || precio === undefined || stock_disponible === undefined) {
    return res.status(400).json({
      error: "Faltan campos obligatorios: id_producto, precio, stock_disponible",
    });
  }

  if (!isPositiveInteger(id_producto)) {
    return res.status(400).json({ error: "id_producto inválido" });
  }

  const precioNum = Number(precio);
  if (!Number.isFinite(precioNum) || precioNum <= 0) {
    return res.status(400).json({ error: "precio debe ser un número positivo" });
  }

  const stockNum = Number(stock_disponible);
  if (!Number.isInteger(stockNum) || stockNum < 0) {
    return res.status(400).json({ error: "stock_disponible debe ser un entero no negativo" });
  }

  try {
    // Lookup id_distribuidor desde id_usuario del JWT
    const distResult = await pool.query(
      `SELECT id_distribuidor, estado_verificacion FROM distribuidor WHERE id_usuario = $1`,
      [Number(idUsuario)]
    );

    if (distResult.rows.length === 0) {
      return res.status(404).json({ error: "Perfil de distribuidor no encontrado" });
    }

    const { id_distribuidor, estado_verificacion } = distResult.rows[0];

    if (estado_verificacion !== "verificado") {
      return res.status(403).json({ error: "Tu cuenta aún no está verificada" });
    }

    // Verificar que el producto existe
    const prodResult = await pool.query(
      `SELECT id_producto FROM producto WHERE id_producto = $1 AND activo = true`,
      [Number(id_producto)]
    );

    if (prodResult.rows.length === 0) {
      return res.status(404).json({ error: "Producto no encontrado" });
    }

    // Upsert — si ya tiene ese producto en inventario, actualiza
    const result = await pool.query(
      `INSERT INTO inventario_distribuidor
   (id_distribuidor, id_producto, precio, stock_disponible, unidad_medida,tiempo_entrega_dias)
 VALUES ($1, $2, $3, $4, $5, $6)
 ON CONFLICT (id_distribuidor, id_producto)
 DO UPDATE SET
   precio = EXCLUDED.precio,
   stock_disponible = EXCLUDED.stock_disponible,
   unidad_medida = COALESCE(EXCLUDED.unidad_medida, inventario_distribuidor.unidad_medida),
   ultima_actualizacion = NOW()
 RETURNING *`,
[id_distribuidor, Number(id_producto), precioNum, stockNum, unidad_medida?.trim() || null, tiempo_entrega_dias !== undefined ? Number(tiempo_entrega_dias) : null]
    );

    return res.status(201).json({
      message: "Inventario publicado correctamente",
      inventario: result.rows[0],
    });
  } catch (error) {
    console.error("Error en createInventory:", error);
    return res.status(500).json({ error: "Error al publicar inventario" });
  }
};

// PUT /api/inventory/:id
// Requiere token de distribuidor verificado
const updateInventory = async (req, res) => {
  const idUsuario = req.user?.id;
  const tipoUsuario = req.user?.tipo;

  if (tipoUsuario !== "distribuidor") {
    return res.status(403).json({ error: "Solo distribuidores pueden actualizar inventario" });
  }

  if (!isPositiveInteger(req.params.id)) {
    return res.status(400).json({ error: "id_inventario inválido" });
  }

  const { precio, stock_disponible, unidad_medida, tiempo_entrega_dias } = req.body;
  const fields = [];
  const values = [];

  if (precio !== undefined) {
    const precioNum = Number(precio);
    if (!Number.isFinite(precioNum) || precioNum <= 0) {
      return res.status(400).json({ error: "precio debe ser un número positivo" });
    }

    values.push(precioNum);
    fields.push(`precio = $${values.length}`);
  }

  if (stock_disponible !== undefined) {
    const stockNum = Number(stock_disponible);
    if (!Number.isInteger(stockNum) || stockNum < 0) {
      return res.status(400).json({ error: "stock_disponible debe ser un entero no negativo" });
    }

    values.push(stockNum);
    fields.push(`stock_disponible = $${values.length}`);
  }

  if (unidad_medida !== undefined) {
    if (unidad_medida !== null && typeof unidad_medida !== "string") {
      return res.status(400).json({ error: "unidad_medida debe ser texto" });
    }

    values.push(unidad_medida?.trim() || null);
    fields.push(`unidad_medida = $${values.length}`);
  }

  if (tiempo_entrega_dias !== undefined) {
    if (tiempo_entrega_dias === null || tiempo_entrega_dias === "") {
      values.push(null);
    } else {
      const tiempoEntregaNum = Number(tiempo_entrega_dias);
      if (!Number.isInteger(tiempoEntregaNum) || tiempoEntregaNum < 0) {
        return res.status(400).json({
          error: "tiempo_entrega_dias debe ser un entero no negativo",
        });
      }
      values.push(tiempoEntregaNum);
    }

    fields.push(`tiempo_entrega_dias = $${values.length}`);
  }

  if (fields.length === 0) {
    return res.status(400).json({ error: "No hay campos válidos para actualizar" });
  }

  try {
    const distResult = await pool.query(
      `SELECT id_distribuidor, estado_verificacion
       FROM distribuidor
       WHERE id_usuario = $1`,
      [Number(idUsuario)]
    );

    if (distResult.rows.length === 0) {
      return res.status(404).json({ error: "Perfil de distribuidor no encontrado" });
    }

    const { id_distribuidor, estado_verificacion } = distResult.rows[0];

    if (estado_verificacion !== "verificado") {
      return res.status(403).json({ error: "Tu cuenta aún no está verificada" });
    }

    const previousInventory = await pool.query(
      `SELECT id_inventario,
              id_distribuidor,
              id_producto,
              precio
       FROM inventario_distribuidor
       WHERE id_inventario = $1
         AND id_distribuidor = $2`,
      [Number(req.params.id), id_distribuidor]
    );

    if (previousInventory.rows.length === 0) {
      return res.status(404).json({ error: "Inventario no encontrado" });
    }

    values.push(Number(req.params.id), id_distribuidor);

    const result = await pool.query(
      `UPDATE inventario_distribuidor
       SET ${fields.join(", ")},
           ultima_actualizacion = NOW()
       WHERE id_inventario = $${values.length - 1}
         AND id_distribuidor = $${values.length}
       RETURNING *`,
      values
    );

    const previousPrice = Number(previousInventory.rows[0].precio);
    const newPrice = Number(result.rows[0].precio);

    if (newPrice < previousPrice) {
      await notifyPriceDropSafely(
        Number(result.rows[0].id_producto),
        previousPrice,
        newPrice
      );
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

module.exports = { getDistributorInventory, createInventory, updateInventory };
