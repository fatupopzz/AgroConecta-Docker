const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

// GET /api/cart/:id_agricultor
const getCart = async (req, res) => {
  const { id_agricultor } = req.params;
  if (!isPositiveInteger(id_agricultor)) {
    return res.status(400).json({ error: "id_agricultor inválido" });
  }
  try {
    const carrito = await pool.query(
      `SELECT c.id_carrito, c.fecha_actualizacion,
              json_agg(json_build_object(
                'id_item', ic.id_item,
                'id_inventario', ic.id_inventario,
                'cantidad', ic.cantidad,
                'precio_unitario', i.precio,
                'subtotal', ic.cantidad * i.precio,
                'producto', p.nombre,
                'marca', p.marca,
                'distribuidor', d.nombre_negocio,
                'stock_disponible', i.stock_disponible,
                'unidad_medida', i.unidad_medida
              )) AS items
       FROM carrito c
       JOIN item_carrito ic ON c.id_carrito = ic.id_carrito
       JOIN inventario_distribuidor i ON ic.id_inventario = i.id_inventario
       JOIN producto p ON i.id_producto = p.id_producto
       JOIN distribuidor d ON i.id_distribuidor = d.id_distribuidor
       WHERE c.id_agricultor = $1
       GROUP BY c.id_carrito, c.fecha_actualizacion`,
      [Number(id_agricultor)],
    );

    if (carrito.rows.length === 0) {
      return res.json({ id_carrito: null, items: [], total: 0 });
    }

    const items = carrito.rows[0].items;
    const total = items.reduce((sum, item) => sum + Number(item.subtotal), 0);

    return res.json({ ...carrito.rows[0], total });
  } catch (error) {
    console.error("Error en getCart:", error);
    return res.status(500).json({ error: "Error al obtener carrito" });
  }
};

// POST /api/cart/:id_agricultor/items
const addItem = async (req, res) => {
  const { id_agricultor } = req.params;
  const { id_inventario, cantidad } = req.body;

  if (!isPositiveInteger(id_agricultor)) {
    return res.status(400).json({ error: "id_agricultor inválido" });
  }
  if (!isPositiveInteger(id_inventario)) {
    return res.status(400).json({ error: "id_inventario inválido" });
  }
  if (!isPositiveInteger(cantidad)) {
    return res.status(400).json({ error: "cantidad inválida" });
  }

  let client;
  try {
    client = await pool.connect();
    await client.query("BEGIN");

    const invResult = await client.query(
      `SELECT id_inventario, stock_disponible FROM inventario_distribuidor WHERE id_inventario = $1`,
      [Number(id_inventario)],
    );
    if (invResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Inventario no encontrado" });
    }
    if (invResult.rows[0].stock_disponible < Number(cantidad)) {
      await client.query("ROLLBACK");
      return res.status(400).json({ error: "Stock insuficiente" });
    }

    const carritoResult = await client.query(
      `INSERT INTO carrito (id_agricultor) VALUES ($1)
       ON CONFLICT (id_agricultor) DO UPDATE SET fecha_actualizacion = NOW()
       RETURNING id_carrito`,
      [Number(id_agricultor)],
    );
    const id_carrito = carritoResult.rows[0].id_carrito;

    const itemResult = await client.query(
      `INSERT INTO item_carrito (id_carrito, id_inventario, cantidad)
       VALUES ($1, $2, $3)
       ON CONFLICT (id_carrito, id_inventario) DO UPDATE SET cantidad = $3
       RETURNING *`,
      [id_carrito, Number(id_inventario), Number(cantidad)],
    );

    await client.query("COMMIT");
    return res
      .status(201)
      .json({ message: "Item agregado al carrito", item: itemResult.rows[0] });
  } catch (error) {
    if (client) await client.query("ROLLBACK");
    console.error("Error en addItem:", error);
    return res.status(500).json({ error: "Error al agregar item" });
  } finally {
    if (client) client.release();
  }
};

// PATCH /api/cart/:id_agricultor/items/:id_item
const updateItem = async (req, res) => {
  const { id_item, id_agricultor } = req.params;
  const { cantidad } = req.body;

  if (!isPositiveInteger(id_item)) {
    return res.status(400).json({ error: "id_item inválido" });
  }
  if (!isPositiveInteger(cantidad)) {
    return res.status(400).json({ error: "cantidad inválida" });
  }

  let client;
  try {
    client = await pool.connect();
    await client.query("BEGIN");

    const invCheck = await client.query(
      `SELECT i.stock_disponible FROM item_carrito ic
       JOIN inventario_distribuidor i ON ic.id_inventario = i.id_inventario
       WHERE ic.id_item = $1`,
      [Number(id_item)],
    );
    if (invCheck.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Item no encontrado" });
    }
    if (invCheck.rows[0].stock_disponible < Number(cantidad)) {
      await client.query("ROLLBACK");
      return res.status(400).json({ error: "Stock insuficiente" });
    }

    const result = await client.query(
      `UPDATE item_carrito SET cantidad = $2 WHERE id_item = $1 RETURNING *`,
      [Number(id_item), Number(cantidad)],
    );

    await client.query(
      `UPDATE carrito SET fecha_actualizacion = NOW() WHERE id_carrito = $1`,
      [result.rows[0].id_carrito],
    );

    await client.query("COMMIT");
    return res.json({ message: "Item actualizado", item: result.rows[0] });
  } catch (error) {
    if (client) await client.query("ROLLBACK");
    console.error("Error en updateItem:", error);
    return res.status(500).json({ error: "Error al actualizar item" });
  } finally {
    if (client) client.release();
  }
};

// DELETE /api/cart/:id_agricultor/items/:id_item
const removeItem = async (req, res) => {
  const { id_item } = req.params;
  if (!isPositiveInteger(id_item)) {
    return res.status(400).json({ error: "id_item inválido" });
  }
  let client;
  try {
    client = await pool.connect();
    await client.query("BEGIN");

    const item = await client.query(
      `SELECT id_carrito FROM item_carrito WHERE id_item = $1`,
      [Number(id_item)],
    );
    if (item.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Item no encontrado" });
    }

    await client.query(`DELETE FROM item_carrito WHERE id_item = $1`, [
      Number(id_item),
    ]);
    await client.query(
      `UPDATE carrito SET fecha_actualizacion = NOW() WHERE id_carrito = $1`,
      [item.rows[0].id_carrito],
    );

    await client.query("COMMIT");
    return res.json({ message: "Item eliminado" });
  } catch (error) {
    if (client) await client.query("ROLLBACK");
    console.error("Error en removeItem:", error);
    return res.status(500).json({ error: "Error al eliminar item" });
  } finally {
    if (client) client.release();
  }
}; // DELETE /api/cart/:id_agricultor
const clearCart = async (req, res) => {
  const { id_agricultor } = req.params;
  if (!isPositiveInteger(id_agricultor)) {
    return res.status(400).json({ error: "id_agricultor inválido" });
  }
  try {
    await pool.query(`DELETE FROM carrito WHERE id_agricultor = $1`, [
      Number(id_agricultor),
    ]);
    return res.json({ message: "Carrito vaciado" });
  } catch (error) {
    console.error("Error en clearCart:", error);
    return res.status(500).json({ error: "Error al vaciar carrito" });
  }
};

module.exports = { getCart, addItem, updateItem, removeItem, clearCart };
