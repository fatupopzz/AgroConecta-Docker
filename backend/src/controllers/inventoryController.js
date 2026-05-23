const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

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

module.exports = { createInventory };
