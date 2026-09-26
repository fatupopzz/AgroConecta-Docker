const { normalizeOrderState } = require("../constants/orderStates");

const loadFarmerHistory = async (db, userId) => {
  const farmerResult = await db.query(
    `SELECT a.id_agricultor, u.nombre, u.apellido, u.email, u.telefono,
            a.departamento, a.municipio, a.tipo_agricultor,
            a.tamano_terreno_ha, a.cultivos_principales
     FROM agricultor a
     JOIN usuario u ON u.id_usuario = a.id_usuario
     WHERE a.id_usuario = $1 AND u.tipo_usuario = 'agricultor'`,
    [userId]
  );
  if (farmerResult.rows.length === 0) return null;
  const farmer = farmerResult.rows[0];

  const result = await db.query(
    `SELECT p.id_pedido, TO_CHAR(p.fecha_pedido, 'DD/MM/YYYY') AS fecha_pedido,
            p.estado, p.total_pedido,
            dp.id_detalle, dp.cantidad, pr.nombre AS producto_nombre
     FROM pedido p
     LEFT JOIN detalle_pedido dp ON dp.id_pedido = p.id_pedido
     LEFT JOIN inventario_distribuidor i ON i.id_inventario = dp.id_inventario
     LEFT JOIN producto pr ON pr.id_producto = i.id_producto
     WHERE p.id_agricultor = $1
     ORDER BY p.fecha_pedido DESC, p.id_pedido DESC, dp.id_detalle ASC`,
    [farmer.id_agricultor]
  );

  const orders = [];
  const byId = new Map();
  for (const row of result.rows) {
    let order = byId.get(row.id_pedido);
    if (!order) {
      order = {
        id: row.id_pedido,
        date: row.fecha_pedido,
        status: normalizeOrderState(row.estado),
        total: row.total_pedido,
        products: [],
      };
      byId.set(row.id_pedido, order);
      orders.push(order);
    }
    if (row.id_detalle != null) {
      order.products.push({ name: row.producto_nombre, quantity: row.cantidad });
    }
  }
  return { farmer, orders };
};

module.exports = { loadFarmerHistory };
