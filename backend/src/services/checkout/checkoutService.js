const { NOTIFICATION_TYPES } = require("../../constants/notificationTypes");
const { ORDER_STATES } = require("../../constants/orderStates");
const { validateCheckoutRequest } = require("./checkoutValidation");
const { validateInventoryAndCalculateTotal } = require("./checkoutTotal");

const sendCheckoutError = (res, error) =>
  res.status(error.statusCode).json({ error: error.message });

const executeCheckout = async ({
  req,
  res,
  pool,
  getOrderDetailData,
  insertOrderTracking,
}) => {
  const validation = validateCheckoutRequest({
    farmerId: req.agricultorId,
    body: req.body,
  });

  if (validation.error) {
    return sendCheckoutError(res, validation.error);
  }

  const checkout = validation.value;
  const client = await pool.connect();

  try {
    await client.query("BEGIN");

    const farmerResult = await client.query(
      "SELECT 1 FROM agricultor WHERE id_agricultor = $1",
      [checkout.farmerId]
    );

    if (farmerResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Agricultor no encontrado" });
    }

    const distributorResult = await client.query(
      "SELECT 1 FROM distribuidor WHERE id_distribuidor = $1",
      [checkout.distributorId]
    );

    if (distributorResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    const inventoryResult = await client.query(
      `SELECT
          i.id_inventario,
          i.id_distribuidor,
          i.id_producto,
          i.precio,
          i.stock_disponible,
          i.unidad_medida,
          p.nombre AS producto_nombre
       FROM inventario_distribuidor i
       JOIN producto p ON i.id_producto = p.id_producto
       WHERE i.id_inventario = ANY($1::int[])`,
      [checkout.inventoryIds]
    );

    const calculated = validateInventoryAndCalculateTotal({
      inventoryRows: inventoryResult.rows,
      inventoryIds: checkout.inventoryIds,
      normalizedProducts: checkout.normalizedProducts,
      distributorId: checkout.distributorId,
    });

    if (calculated.error) {
      await client.query("ROLLBACK");
      return sendCheckoutError(res, calculated.error);
    }

    const { inventoryMap, total } = calculated.value;
    const orderResult = await client.query(
      // El flujo existente persiste este literal incluso si recibe recogida.
      // Se mantiene intencionalmente porque CART-03 no cambia comportamiento.
      `INSERT INTO pedido
       (id_agricultor, id_distribuidor, estado, tipo_entrega, direccion_entrega, es_urgente, tipo_plaga, total_pedido, costo_envio, notas)
       VALUES ($1, $2, $3, 'domicilio', $4, $5, $6, $7, 0, NULL)
       RETURNING *`,
      [
        checkout.farmerId,
        checkout.distributorId,
        ORDER_STATES.CONFIRMED,
        checkout.deliveryAddress,
        checkout.urgent,
        checkout.pestType,
        total,
      ]
    );

    const createdOrder = orderResult.rows[0];

    for (const item of checkout.normalizedProducts) {
      const inventory = inventoryMap.get(item.id_inventario);

      await client.query(
        `INSERT INTO detalle_pedido
         (id_pedido, id_inventario, cantidad, precio_unitario)
         VALUES ($1, $2, $3, $4)`,
        [
          createdOrder.id_pedido,
          item.id_inventario,
          item.cantidad,
          inventory.precio,
        ]
      );
    }

    await client.query(
      `INSERT INTO pago
       (id_pedido, metodo_pago, monto, estado_pago, fecha_pago, referencia_transaccion)
       VALUES ($1, $2, $3, 'pendiente', NULL, NULL)`,
      [createdOrder.id_pedido, checkout.paymentMethod, total]
    );

    const farmerInfo = await client.query(
      `SELECT u.nombre
      FROM agricultor a
      JOIN usuario u ON a.id_usuario = u.id_usuario
      WHERE a.id_agricultor = $1`,
      [checkout.farmerId]
    );

    const farmerName =
      farmerInfo.rows.length > 0 ? farmerInfo.rows[0].nombre : "Agricultor";

    await insertOrderTracking(
      client,
      createdOrder.id_pedido,
      ORDER_STATES.CONFIRMED,
      "Pedido confirmado"
    );

    await client.query(
      `INSERT INTO notificacion
      (
          id_distribuidor,
          id_pedido,
          tipo,
          contenido,
          leida
      )
      VALUES ($1, $2, $3, $4, FALSE)`,
      [
        checkout.distributorId,
        createdOrder.id_pedido,
        checkout.urgent
          ? NOTIFICATION_TYPES.PEDIDO_URGENTE
          : NOTIFICATION_TYPES.NUEVO_PEDIDO,
        JSON.stringify({
          mensaje: checkout.urgent
            ? "Pedido urgente por detección de plaga"
            : "Nuevo pedido recibido",
          agricultor: farmerName,
          monto: total,
          pedido: createdOrder.id_pedido,
          esUrgente: checkout.urgent,
          tipoPlaga: checkout.pestType,
        }),
      ]
    );

    await client.query("COMMIT");

    const orderDetail = await getOrderDetailData(
      client,
      createdOrder.id_pedido
    );

    return res.status(201).json({
      message: "Pedido creado correctamente",
      pedido: orderDetail,
    });
  } catch (error) {
    try {
      await client.query("ROLLBACK");
    } catch (rollbackError) {
      console.error("Error al hacer rollback en createOrder:", rollbackError);
    }

    console.error("Error en createOrder:", {
      message: error.message,
      code: error.code,
      detail: error.detail,
      constraint: error.constraint,
      table: error.table,
    });

    if (error.code === "23503") {
      return res.status(400).json({
        error:
          "No se pudo crear el pedido por una referencia inválida en la base de datos",
      });
    }

    return res.status(500).json({ error: "Error al crear el pedido" });
  } finally {
    client.release();
  }
};

module.exports = { executeCheckout };
