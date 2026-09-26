const { pool } = require("../config/db");
const { NOTIFICATION_TYPES } = require("../constants/notificationTypes");
const { ORDER_STATES } = require("../constants/orderStates");

const DEFAULT_INTERVAL_MS = 60_000;
let scheduler = null;
let running = false;

const nextOccurrence = (frequency, from = new Date()) => {
  const next = new Date(from);
  if (frequency === "diaria") next.setUTCDate(next.getUTCDate() + 1);
  else if (frequency === "semanal") next.setUTCDate(next.getUTCDate() + 7);
  else if (frequency === "quincenal") next.setUTCDate(next.getUTCDate() + 15);
  else if (frequency === "mensual") {
    const originalDay = next.getUTCDate();
    next.setUTCDate(1);
    next.setUTCMonth(next.getUTCMonth() + 1);
    const lastDay = new Date(Date.UTC(
      next.getUTCFullYear(),
      next.getUTCMonth() + 1,
      0
    )).getUTCDate();
    next.setUTCDate(Math.min(originalDay, lastDay));
  }
  else throw new Error(`Frecuencia recurrente no soportada: ${frequency}`);
  return next;
};

const nextFutureOccurrence = (frequency, scheduledAt, now = new Date()) => {
  let next = nextOccurrence(frequency, scheduledAt);
  while (next <= now) next = nextOccurrence(frequency, next);
  return next;
};

const notifySkippedProducts = async (client, farmerId, recurringId, skipped) => {
  if (skipped.length === 0) return;
  await client.query(
    `INSERT INTO notificacion (id_agricultor, tipo, contenido, leida)
     VALUES ($1, $2, $3::jsonb, FALSE)`,
    [
      farmerId,
      NOTIFICATION_TYPES.PEDIDO_RECURRENTE_SIN_STOCK,
      JSON.stringify({
        mensaje: "Algunos productos del pedido recurrente fueron omitidos por falta de stock",
        id_pedido_recurrente: recurringId,
        productos_omitidos: skipped,
      }),
    ]
  );
};

const processRecurringOrder = async (recurringId, db = pool) => {
  const client = await db.connect();
  try {
    await client.query("BEGIN");
    const recurringResult = await client.query(
      `SELECT pr.*, a.id_agricultor, u.nombre AS agricultor_nombre
       FROM pedido_recurrente pr
       JOIN usuario u ON u.id_usuario = pr.id_usuario
       JOIN agricultor a ON a.id_usuario = pr.id_usuario
       WHERE pr.id = $1
         AND pr.estado = 'activo'
         AND pr.fecha_proximo <= NOW()
       FOR UPDATE OF pr SKIP LOCKED`,
      [recurringId]
    );

    if (recurringResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return { processed: false };
    }

    const recurring = recurringResult.rows[0];
    const config = recurring.productos;
    const items = Array.isArray(config.items) ? config.items : [];
    const inventoryIds = items.map((item) => Number(item.id_inventario));
    const inventoryResult = inventoryIds.length === 0
      ? { rows: [] }
      : await client.query(
        `SELECT i.id_inventario, i.id_distribuidor, i.precio,
                i.stock_disponible, p.nombre AS producto_nombre
         FROM inventario_distribuidor i
         JOIN producto p ON p.id_producto = i.id_producto
         WHERE i.id_inventario = ANY($1::int[])
         FOR UPDATE OF i`,
        [inventoryIds]
      );
    const inventoryById = new Map(
      inventoryResult.rows.map((row) => [Number(row.id_inventario), row])
    );

    const available = [];
    const skipped = [];
    for (const item of items) {
      const inventory = inventoryById.get(Number(item.id_inventario));
      const quantity = Number(item.cantidad);
      if (
        !inventory ||
        Number(inventory.id_distribuidor) !== Number(config.id_distribuidor) ||
        Number(inventory.stock_disponible) < quantity
      ) {
        skipped.push({
          id_inventario: Number(item.id_inventario),
          cantidad_solicitada: quantity,
          stock_disponible: inventory ? Number(inventory.stock_disponible) : 0,
          producto: inventory?.producto_nombre ?? "Producto no disponible",
        });
      } else {
        available.push({ item, inventory });
      }
    }

    await notifySkippedProducts(
      client,
      recurring.id_agricultor,
      recurring.id,
      skipped
    );

    let orderId = null;
    if (available.length > 0) {
      const total = Number(available.reduce(
        (sum, { item, inventory }) => sum + Number(item.cantidad) * Number(inventory.precio),
        0
      ).toFixed(2));
      const orderResult = await client.query(
        `INSERT INTO pedido
           (id_agricultor, id_distribuidor, estado, tipo_entrega,
            direccion_entrega, es_urgente, total_pedido, costo_envio, notas)
         VALUES ($1, $2, $3, 'domicilio', $4, FALSE, $5, 0, $6)
         RETURNING id_pedido`,
        [
          recurring.id_agricultor,
          config.id_distribuidor,
          ORDER_STATES.CONFIRMED,
          config.direccion_entrega,
          total,
          `Generado automáticamente por pedido recurrente #${recurring.id}`,
        ]
      );
      orderId = orderResult.rows[0].id_pedido;

      for (const { item, inventory } of available) {
        await client.query(
          `INSERT INTO detalle_pedido
             (id_pedido, id_inventario, cantidad, precio_unitario)
           VALUES ($1, $2, $3, $4)`,
          [orderId, item.id_inventario, item.cantidad, inventory.precio]
        );
      }
      await client.query(
        `INSERT INTO pago (id_pedido, metodo_pago, monto, estado_pago)
         VALUES ($1, $2, $3, 'pendiente')`,
        [orderId, config.metodo_pago, total]
      );
      await client.query(
        `INSERT INTO pedido_tracking (id_pedido, estado, notas)
         VALUES ($1, $2, $3)`,
        [orderId, ORDER_STATES.CONFIRMED, "Pedido recurrente confirmado"]
      );
      await client.query(
        `INSERT INTO notificacion
           (id_distribuidor, id_pedido, tipo, contenido, leida)
         VALUES ($1, $2, $3, $4::jsonb, FALSE)`,
        [
          config.id_distribuidor,
          orderId,
          NOTIFICATION_TYPES.NUEVO_PEDIDO,
          JSON.stringify({
            mensaje: "Nuevo pedido recurrente recibido",
            agricultor: recurring.agricultor_nombre,
            monto: total,
            pedido: orderId,
            id_pedido_recurrente: recurring.id,
          }),
        ]
      );
    }

    const nextDate = nextFutureOccurrence(
      recurring.frecuencia,
      new Date(recurring.fecha_proximo),
      new Date()
    );
    await client.query(
      `UPDATE pedido_recurrente
       SET fecha_proximo = $1, fecha_actualizacion = NOW()
       WHERE id = $2`,
      [nextDate.toISOString(), recurring.id]
    );
    await client.query("COMMIT");
    return { processed: true, orderId, skipped: skipped.length, nextDate };
  } catch (error) {
    try {
      await client.query("ROLLBACK");
    } catch (rollbackError) {
      console.error("Error al revertir pedido recurrente:", rollbackError);
    }
    throw error;
  } finally {
    client.release();
  }
};

const processDueRecurringOrders = async (db = pool) => {
  if (running) return [];
  running = true;
  try {
    const due = await db.query(
      `SELECT id FROM pedido_recurrente
       WHERE estado = 'activo' AND fecha_proximo <= NOW()
       ORDER BY fecha_proximo ASC
       LIMIT 100`
    );
    const results = [];
    for (const row of due.rows) {
      try {
        results.push(await processRecurringOrder(row.id, db));
      } catch (error) {
        console.error(`Error procesando pedido recurrente ${row.id}:`, error);
        results.push({ processed: false, id: row.id, error: error.message });
      }
    }
    return results;
  } finally {
    running = false;
  }
};

const startRecurringOrderScheduler = (db = pool) => {
  if (scheduler) return scheduler;
  const configured = Number(process.env.RECURRING_ORDERS_INTERVAL_MS);
  const intervalMs = Number.isFinite(configured) && configured >= 1_000
    ? configured
    : DEFAULT_INTERVAL_MS;
  processDueRecurringOrders(db).catch((error) => {
    console.error("Error inicial del scheduler de pedidos recurrentes:", error);
  });
  scheduler = setInterval(() => {
    processDueRecurringOrders(db).catch((error) => {
      console.error("Error del scheduler de pedidos recurrentes:", error);
    });
  }, intervalMs);
  scheduler.unref?.();
  return scheduler;
};

const stopRecurringOrderScheduler = () => {
  if (scheduler) clearInterval(scheduler);
  scheduler = null;
};

module.exports = {
  nextOccurrence,
  nextFutureOccurrence,
  processRecurringOrder,
  processDueRecurringOrders,
  startRecurringOrderScheduler,
  stopRecurringOrderScheduler,
};
