const { pool } = require("../config/db");
const {
  validateCreateRecurringOrder,
  validateUpdateRecurringOrder,
} = require("../services/recurringOrderValidation");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const serialize = (row) => ({
  id: row.id,
  id_usuario: row.id_usuario,
  frecuencia: row.frecuencia,
  productos: row.productos.items,
  id_distribuidor: row.productos.id_distribuidor,
  direccion_entrega: row.productos.direccion_entrega,
  metodo_pago: row.productos.metodo_pago,
  fecha_proximo: row.fecha_proximo,
  estado: row.estado,
  fecha_creacion: row.fecha_creacion,
  fecha_actualizacion: row.fecha_actualizacion,
});

const requireFarmer = (req, res) => {
  if (!req.user || req.user.tipo !== "agricultor" || !isPositiveInteger(req.user.id)) {
    res.status(403).json({ error: "Solo un agricultor puede gestionar pedidos recurrentes" });
    return null;
  }
  return Number(req.user.id);
};

const validateStoredConfiguration = async (configuration) => {
  const distributor = await pool.query(
    "SELECT 1 FROM distribuidor WHERE id_distribuidor = $1",
    [configuration.id_distribuidor]
  );
  if (distributor.rows.length === 0) {
    return { statusCode: 404, message: "Distribuidor no encontrado" };
  }

  const inventoryIds = configuration.items.map((item) => item.id_inventario);
  const inventory = await pool.query(
    `SELECT id_inventario
     FROM inventario_distribuidor
     WHERE id_distribuidor = $1
       AND id_inventario = ANY($2::int[])`,
    [configuration.id_distribuidor, inventoryIds]
  );
  const foundIds = new Set(inventory.rows.map((row) => Number(row.id_inventario)));
  if (inventoryIds.some((id) => !foundIds.has(id))) {
    return {
      statusCode: 400,
      message: "Todos los productos deben existir y pertenecer al distribuidor indicado",
    };
  }

  return null;
};

const createRecurringOrder = async (req, res) => {
  const userId = requireFarmer(req, res);
  if (!userId) return;

  const validation = validateCreateRecurringOrder(req.body);
  if (validation.error) {
    return res.status(validation.error.statusCode).json({ error: validation.error.message });
  }

  try {
    const farmer = await pool.query(
      "SELECT 1 FROM agricultor WHERE id_usuario = $1",
      [userId]
    );
    if (farmer.rows.length === 0) {
      return res.status(404).json({ error: "Perfil de agricultor no encontrado" });
    }

    const configurationError = await validateStoredConfiguration(validation.value.productos);
    if (configurationError) {
      return res.status(configurationError.statusCode).json({ error: configurationError.message });
    }

    const result = await pool.query(
      `INSERT INTO pedido_recurrente
         (id_usuario, frecuencia, productos, fecha_proximo, estado)
       VALUES ($1, $2, $3::jsonb, $4, 'activo')
       RETURNING *`,
      [
        userId,
        validation.value.frecuencia,
        JSON.stringify(validation.value.productos),
        validation.value.fecha_proximo,
      ]
    );

    return res.status(201).json({
      message: "Pedido recurrente creado correctamente",
      pedido_recurrente: serialize(result.rows[0]),
    });
  } catch (error) {
    console.error("Error al crear pedido recurrente:", error);
    return res.status(500).json({ error: "Error al crear el pedido recurrente" });
  }
};

const listRecurringOrders = async (req, res) => {
  const userId = requireFarmer(req, res);
  if (!userId) return;

  try {
    const result = await pool.query(
      `SELECT *
       FROM pedido_recurrente
       WHERE id_usuario = $1
       ORDER BY fecha_creacion DESC, id DESC`,
      [userId]
    );
    return res.json(result.rows.map(serialize));
  } catch (error) {
    console.error("Error al listar pedidos recurrentes:", error);
    return res.status(500).json({ error: "Error al listar pedidos recurrentes" });
  }
};

const updateRecurringOrder = async (req, res) => {
  const userId = requireFarmer(req, res);
  if (!userId) return;
  if (!isPositiveInteger(req.params.id)) {
    return res.status(400).json({ error: "ID de pedido recurrente inválido" });
  }

  try {
    const currentResult = await pool.query(
      `SELECT * FROM pedido_recurrente
       WHERE id = $1 AND id_usuario = $2`,
      [Number(req.params.id), userId]
    );
    if (currentResult.rows.length === 0) {
      return res.status(404).json({ error: "Pedido recurrente no encontrado" });
    }

    const current = currentResult.rows[0];
    if (current.estado === "cancelado") {
      return res.status(409).json({ error: "Un pedido recurrente cancelado no puede modificarse" });
    }

    const validation = validateUpdateRecurringOrder(req.body, current);
    if (validation.error) {
      return res.status(validation.error.statusCode).json({ error: validation.error.message });
    }

    const configurationError = await validateStoredConfiguration(validation.value.productos);
    if (configurationError) {
      return res.status(configurationError.statusCode).json({ error: configurationError.message });
    }

    const updated = await pool.query(
      `UPDATE pedido_recurrente
       SET frecuencia = $1,
           productos = $2::jsonb,
           fecha_proximo = $3,
           estado = $4,
           fecha_actualizacion = NOW()
       WHERE id = $5 AND id_usuario = $6
       RETURNING *`,
      [
        validation.value.frecuencia,
        JSON.stringify(validation.value.productos),
        validation.value.fecha_proximo,
        validation.value.estado,
        Number(req.params.id),
        userId,
      ]
    );

    return res.json({
      message: "Pedido recurrente actualizado correctamente",
      pedido_recurrente: serialize(updated.rows[0]),
    });
  } catch (error) {
    console.error("Error al actualizar pedido recurrente:", error);
    return res.status(500).json({ error: "Error al actualizar el pedido recurrente" });
  }
};

module.exports = {
  createRecurringOrder,
  listRecurringOrders,
  updateRecurringOrder,
};
