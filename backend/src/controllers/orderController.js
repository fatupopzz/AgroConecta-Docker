const { pool } = require("../config/db");
const {
  LEGACY_ORDER_STATES,
  ORDER_STATES,
  ORDER_STATES_FILTERABLE,
  ORDER_STATES_UPDATEABLE,
  normalizeOrderState,
} = require("../constants/orderStates");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const normalizeCashPaymentMethod = (value) => {
  if (value === undefined || value === null || value === "") {
    return "contra_entrega";
  }

  if (typeof value !== "string") {
    return null;
  }

  const normalized = value.trim().toLowerCase();

  if (normalized === "efectivo" || normalized === "contra_entrega") {
    return "contra_entrega";
  }

  return null;
};

const insertOrderTracking = async (db, orderId, estado, notas = null) => {
  await db.query(
    `INSERT INTO pedido_tracking (id_pedido, estado, notas)
     VALUES ($1, $2, $3)`,
    [orderId, estado, notas]
  );
};

const getOrderDetailData = async (db, orderId) => {
  const orderResult = await db.query(
    `SELECT
        p.id_pedido,
        p.fecha_pedido,
        p.fecha_entrega_real,
        p.estado,
        p.tipo_entrega,
        p.direccion_entrega,
        p.es_urgente,
        p.total_pedido,
        p.costo_envio,
        p.notas,
        a.id_agricultor,
        ua.nombre AS agricultor_nombre,
        ua.email AS agricultor_email,
        ua.telefono AS agricultor_telefono,
        d.id_distribuidor,
        d.nombre_negocio AS distribuidor_nombre,
        ud.nombre AS distribuidor_contacto,
        ud.email AS distribuidor_email,
        ud.telefono AS distribuidor_telefono,
        pa.metodo_pago,
        pa.estado_pago,
        pa.monto
     FROM pedido p
     JOIN agricultor a ON p.id_agricultor = a.id_agricultor
     JOIN usuario ua ON a.id_usuario = ua.id_usuario
     JOIN distribuidor d ON p.id_distribuidor = d.id_distribuidor
     JOIN usuario ud ON d.id_usuario = ud.id_usuario
     LEFT JOIN pago pa ON p.id_pedido = pa.id_pedido
     WHERE p.id_pedido = $1`,
    [orderId]
  );

  if (orderResult.rows.length === 0) {
    return null;
  }

  const order = orderResult.rows[0];
  order.estado = normalizeOrderState(order.estado);

  const itemsResult = await db.query(
    `SELECT
        dp.id_detalle,
        dp.id_inventario,
        dp.cantidad,
        dp.precio_unitario,
        dp.subtotal,
        i.id_producto,
        i.unidad_medida,
        pr.nombre AS producto_nombre,
        pr.marca AS producto_marca
     FROM detalle_pedido dp
     JOIN inventario_distribuidor i ON dp.id_inventario = i.id_inventario
     JOIN producto pr ON i.id_producto = pr.id_producto
     WHERE dp.id_pedido = $1
     ORDER BY dp.id_detalle ASC`,
    [orderId]
  );

  return {
    ...order,
    productos: itemsResult.rows,
  };
};

const getOrderTrackingData = async (db, orderId) => {
  const orderResult = await db.query(
    `SELECT
        p.id_pedido,
        CASE
          WHEN p.estado = $2 THEN $3
          WHEN p.estado = $4 THEN $5
          ELSE p.estado
        END AS estado_actual,
        p.fecha_pedido,
        CASE
          WHEN p.estado = $6 THEN p.fecha_entrega_real
          WHEN p.estado = $7 THEN NULL
          ELSE p.fecha_pedido + (COALESCE(MAX(i.tiempo_entrega_dias), 2)::int * INTERVAL '1 day')
        END AS tiempo_estimado_entrega
     FROM pedido p
     LEFT JOIN detalle_pedido dp ON p.id_pedido = dp.id_pedido
     LEFT JOIN inventario_distribuidor i ON dp.id_inventario = i.id_inventario
     WHERE p.id_pedido = $1
     GROUP BY p.id_pedido`,
    [
      orderId,
      LEGACY_ORDER_STATES.PENDING,
      ORDER_STATES.CONFIRMED,
      LEGACY_ORDER_STATES.IN_TRANSIT,
      ORDER_STATES.IN_ROUTE,
      ORDER_STATES.DELIVERED,
      ORDER_STATES.CANCELED,
    ]
  );

  if (orderResult.rows.length === 0) {
    return null;
  }

  const order = orderResult.rows[0];

  const trackingResult = await db.query(
    `SELECT
        id_tracking,
        estado,
        "timestamp",
        notas
     FROM pedido_tracking
     WHERE id_pedido = $1
     ORDER BY "timestamp" ASC, id_tracking ASC`,
    [orderId]
  );

  const cambios = trackingResult.rows.map((row) => ({
    estado: normalizeOrderState(row.estado),
    timestamp: row.timestamp,
    notas: row.notas,
  }));

  if (cambios.length === 0) {
    cambios.push({
      estado: normalizeOrderState(order.estado_actual),
      timestamp: order.fecha_pedido,
      notas: "Estado inicial del pedido",
    });
  }

  return {
    id_pedido: order.id_pedido,
    estado_actual: normalizeOrderState(order.estado_actual),
    cambios,
    tiempo_estimado_entrega: order.tiempo_estimado_entrega,
  };
};

const createOrder = async (req, res) => {
  const {
    id_distribuidor,
    direccion_entrega,
    productos,
    metodo_pago,
  } = req.body;

  const id_agricultor = req.agricultorId;

  if (!isPositiveInteger(id_agricultor)) {
    return res.status(400).json({ error: "id_agricultor inválido" });
  }

  if (!isPositiveInteger(id_distribuidor)) {
    return res.status(400).json({ error: "id_distribuidor inválido" });
  }

  if (
    typeof direccion_entrega !== "string" ||
    direccion_entrega.trim().length < 5
  ) {
    return res.status(400).json({ error: "direccion_entrega inválida" });
  }

  if (!Array.isArray(productos) || productos.length === 0) {
    return res.status(400).json({
      error: "Debe enviar al menos un producto en el pedido",
    });
  }

  const paymentMethod = normalizeCashPaymentMethod(metodo_pago);
  if (!paymentMethod) {
    return res.status(400).json({
      error: "metodo_pago inválido, use efectivo o contra_entrega",
    });
  }

  const normalizedProducts = [];
  const inventoryIds = [];

  for (const item of productos) {
    if (!item || !isPositiveInteger(item.id_inventario)) {
      return res
        .status(400)
        .json({ error: "id_inventario inválido en productos" });
    }

    if (!isPositiveInteger(item.cantidad)) {
      return res.status(400).json({ error: "cantidad inválida en productos" });
    }

    const normalizedItem = {
      id_inventario: Number(item.id_inventario),
      cantidad: Number(item.cantidad),
    };

    normalizedProducts.push(normalizedItem);
    inventoryIds.push(normalizedItem.id_inventario);
  }

  if (new Set(inventoryIds).size !== inventoryIds.length) {
    return res.status(400).json({
      error: "No se permiten productos repetidos en el pedido",
    });
  }

  const client = await pool.connect();

  try {
    await client.query("BEGIN");

    const farmerResult = await client.query(
      "SELECT 1 FROM agricultor WHERE id_agricultor = $1",
      [Number(id_agricultor)]
    );

    if (farmerResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Agricultor no encontrado" });
    }

    const distributorResult = await client.query(
      "SELECT 1 FROM distribuidor WHERE id_distribuidor = $1",
      [Number(id_distribuidor)]
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
      [inventoryIds]
    );

    if (inventoryResult.rows.length !== inventoryIds.length) {
      await client.query("ROLLBACK");
      return res.status(404).json({
        error: "Uno o más productos del pedido no existen en inventario",
      });
    }

    const inventoryMap = new Map();
    for (const row of inventoryResult.rows) {
      inventoryMap.set(Number(row.id_inventario), row);
    }

    let totalPedido = 0;

    for (const item of normalizedProducts) {
      const inventory = inventoryMap.get(item.id_inventario);

      if (!inventory) {
        await client.query("ROLLBACK");
        return res.status(404).json({
          error: "Producto de inventario no encontrado",
        });
      }

      if (Number(inventory.id_distribuidor) !== Number(id_distribuidor)) {
        await client.query("ROLLBACK");
        return res.status(400).json({
          error: "Todos los productos deben pertenecer al distribuidor indicado",
        });
      }

      if (Number(inventory.stock_disponible) < item.cantidad) {
        await client.query("ROLLBACK");
        return res.status(400).json({
          error: `Stock insuficiente para el producto ${inventory.producto_nombre}`,
        });
      }

      totalPedido += Number(inventory.precio) * item.cantidad;
    }

    totalPedido = Number(totalPedido.toFixed(2));

    const orderResult = await client.query(
      `INSERT INTO pedido
       (id_agricultor, id_distribuidor, estado, tipo_entrega, direccion_entrega, es_urgente, total_pedido, costo_envio, notas)
       VALUES ($1, $2, $3, 'domicilio', $4, false, $5, 0, NULL)
       RETURNING *`,
      [
        Number(id_agricultor),
        Number(id_distribuidor),
        ORDER_STATES.CONFIRMED,
        direccion_entrega.trim(),
        totalPedido,
      ]
    );

    const createdOrder = orderResult.rows[0];

    for (const item of normalizedProducts) {
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
      [createdOrder.id_pedido, paymentMethod, totalPedido]
    );

    const farmerInfo = await client.query(
      `SELECT u.nombre
      FROM agricultor a
      JOIN usuario u ON a.id_usuario = u.id_usuario
      WHERE a.id_agricultor = $1`,
      [Number(id_agricultor)]
    );

    const agricultorNombre =
      farmerInfo.rows.length > 0
        ? farmerInfo.rows[0].nombre
        : "Agricultor";

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
        Number(id_distribuidor),
        createdOrder.id_pedido,
        "nuevo_pedido",
        JSON.stringify({
          mensaje: "Nuevo pedido recibido",
          agricultor: agricultorNombre,
          monto: totalPedido,
          pedido: createdOrder.id_pedido
        })
      ]
    );


    await client.query("COMMIT");

    const orderDetail = await getOrderDetailData(client, createdOrder.id_pedido);

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
        error: "No se pudo crear el pedido por una referencia inválida en la base de datos",
      });
    }

    return res.status(500).json({ error: "Error al crear el pedido" });
  } finally {
    client.release();
  }
};

const getOrderById = async (req, res) => {
  try {
    const { id } = req.params;

    if (!isPositiveInteger(id)) {
      return res.status(400).json({ error: "ID de pedido inválido" });
    }

    const orderDetail = await getOrderDetailData(pool, Number(id));

    if (!orderDetail) {
      return res.status(404).json({ error: "Pedido no encontrado" });
    }

    return res.json(orderDetail);
  } catch (error) {
    console.error("Error en getOrderById:", error);
    return res.status(500).json({ error: "Error al obtener el pedido" });
  }
};

const getOrdersByFarmer = async (req, res) => {
  try {
    const { id } = req.params;

    if (!isPositiveInteger(id)) {
      return res.status(400).json({ error: "ID de agricultor inválido" });
    }

    const farmerId = Number(id);

    const requesterId = req.user ? Number(req.user.id) : null;
    const requesterTipo = req.user ? req.user.tipo : null;

    const farmerResult = await pool.query(
      `SELECT id_usuario FROM agricultor
       WHERE id_agricultor = $1
         AND (id_usuario = $2 OR $3::text = 'administrador')`,
      [farmerId, requesterId, requesterTipo]
    );

    if (farmerResult.rows.length === 0) {
      return res.status(404).json({ error: "Agricultor no encontrado" });
    }

    const { page: pageParam, limit: limitParam, estado: estadoParam } = req.query;

    const page = pageParam === undefined ? 1 : Number(pageParam);
    const limit = limitParam === undefined ? 10 : Number(limitParam);

    if (!Number.isInteger(page) || page < 1) {
      return res.status(400).json({ error: "page inválido" });
    }

    if (!Number.isInteger(limit) || limit < 1 || limit > 100) {
      return res.status(400).json({ error: "limit inválido (1-100)" });
    }

    let estadoFiltro = null;
    if (estadoParam !== undefined && estadoParam !== "") {
      if (
        typeof estadoParam !== "string" ||
        !ORDER_STATES_FILTERABLE.includes(estadoParam.trim())
      ) {
        return res.status(400).json({
          error: `estado inválido. Use: ${ORDER_STATES_FILTERABLE.join(" | ")}`,
        });
      }
      estadoFiltro = estadoParam.trim();
    }

    const filters = ["p.id_agricultor = $1"];
    const params = [farmerId];

    if (estadoFiltro) {
      params.push(estadoFiltro);
      filters.push(`p.estado = $${params.length}`);
    }

    const whereClause = filters.join(" AND ");

    const countResult = await pool.query(
      `SELECT COUNT(*)::int AS total FROM pedido p WHERE ${whereClause}`,
      params
    );

    const total = countResult.rows[0].total;
    const totalPages = total === 0 ? 0 : Math.ceil(total / limit);
    const offset = (page - 1) * limit;

    const dataParams = [...params, limit, offset];

    const result = await pool.query(
      `SELECT
          p.id_pedido          AS id,
          CASE
            WHEN p.estado = '${LEGACY_ORDER_STATES.PENDING}' THEN '${ORDER_STATES.CONFIRMED}'
            WHEN p.estado = '${LEGACY_ORDER_STATES.IN_TRANSIT}' THEN '${ORDER_STATES.IN_ROUTE}'
            ELSE p.estado
          END AS estado,
          p.fecha_pedido,
          p.total_pedido,
          d.nombre_negocio     AS distribuidor_nombre,
          COALESCE(COUNT(dp.id_detalle), 0)::int AS cantidad_productos
       FROM pedido p
       JOIN distribuidor d ON p.id_distribuidor = d.id_distribuidor
       LEFT JOIN detalle_pedido dp ON p.id_pedido = dp.id_pedido
       WHERE ${whereClause}
       GROUP BY p.id_pedido, p.estado, p.fecha_pedido, p.total_pedido, d.nombre_negocio
       ORDER BY p.fecha_pedido DESC, p.id_pedido DESC
       LIMIT $${dataParams.length - 1} OFFSET $${dataParams.length}`,
      dataParams
    );

    return res.json({
      data: result.rows,
      total,
      page,
      totalPages,
    });
  } catch (error) {
    console.error("Error en getOrdersByFarmer:", error);
    return res.status(500).json({
      error: "Error al obtener pedidos del agricultor",
    });
  }
};

const getOrdersByDistributor = async (req, res) => {
  try {
    const { id } = req.params;

    if (!isPositiveInteger(id)) {
      return res.status(400).json({ error: "ID de distribuidor inválido" });
    }

    const distributorId = Number(id);

    const distributorResult = await pool.query(
      "SELECT 1 FROM distribuidor WHERE id_distribuidor = $1",
      [distributorId]
    );

    if (distributorResult.rows.length === 0) {
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    const result = await pool.query(
      `SELECT
          p.id_pedido,
          p.fecha_pedido,
          CASE
            WHEN p.estado = '${LEGACY_ORDER_STATES.PENDING}' THEN '${ORDER_STATES.CONFIRMED}'
            WHEN p.estado = '${LEGACY_ORDER_STATES.IN_TRANSIT}' THEN '${ORDER_STATES.IN_ROUTE}'
            ELSE p.estado
          END AS estado,
          p.direccion_entrega,
          p.total_pedido,
          p.costo_envio,
          p.notas,
          a.id_agricultor,
          ua.nombre AS agricultor_nombre,
          ua.email AS agricultor_email,
          ua.telefono AS agricultor_telefono,
          pa.metodo_pago,
          pa.estado_pago
       FROM pedido p
       JOIN agricultor a ON p.id_agricultor = a.id_agricultor
       JOIN usuario ua ON a.id_usuario = ua.id_usuario
       LEFT JOIN pago pa ON p.id_pedido = pa.id_pedido
       WHERE p.id_distribuidor = $1
       ORDER BY p.fecha_pedido DESC, p.id_pedido DESC`,
      [distributorId]
    );

    return res.json(result.rows);
  } catch (error) {
    console.error("Error en getOrdersByDistributor:", error);
    return res.status(500).json({
      error: "Error al obtener pedidos del distribuidor",
    });
  }
};

const updateOrderStatus = async (req, res) => {
  const { id } = req.params;
  const { estado, notas } = req.body;
  const distributorId = req.distributorId;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de pedido inválido" });
  }

  if (typeof estado !== "string" || !ORDER_STATES_UPDATEABLE.includes(estado.trim())) {
    return res.status(400).json({
      error: `estado inválido. Use: ${ORDER_STATES_UPDATEABLE.join(", ")}`,
    });
  }

  const estadoNormalizado = estado.trim();
  const notasTracking =
    typeof notas === "string" && notas.trim().length > 0 ? notas.trim() : null;
  const orderId = Number(id);

  const client = await pool.connect();

  try {
    await client.query("BEGIN");

    const orderResult = await client.query(
      `SELECT id_pedido, id_distribuidor, estado
       FROM pedido
       WHERE id_pedido = $1`,
      [orderId]
    );

    if (orderResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Pedido no encontrado" });
    }

    const order = orderResult.rows[0];

    if (distributorId && Number(order.id_distribuidor) !== distributorId) {
      await client.query("ROLLBACK");
      return res.status(403).json({
        error: "El distribuidor no tiene permiso para actualizar este pedido",
      });
    }

    const updatedOrderResult = await client.query(
      `UPDATE pedido
       SET estado = $1,
           fecha_entrega_real = CASE
             WHEN $1 = $3 THEN COALESCE(fecha_entrega_real, NOW())
             ELSE fecha_entrega_real
           END
       WHERE id_pedido = $2
       RETURNING *`,
      [estadoNormalizado, orderId, ORDER_STATES.DELIVERED]
    );

    if (normalizeOrderState(order.estado) !== estadoNormalizado) {
      await insertOrderTracking(client, orderId, estadoNormalizado, notasTracking);
    }

    if (estadoNormalizado === "entregado") {
      await client.query(
        `UPDATE pago
         SET estado_pago = 'completado',
             fecha_pago = NOW()
         WHERE id_pedido = $1
           AND metodo_pago = 'contra_entrega'`,
        [orderId]
      );
    }

    await client.query("COMMIT");

    const orderDetail = await getOrderDetailData(client, orderId);

    return res.json({
      message: "Estado del pedido actualizado correctamente",
      pedido: orderDetail || updatedOrderResult.rows[0],
    });
  } catch (error) {
    try {
      await client.query("ROLLBACK");
    } catch (rollbackError) {
      console.error(
        "Error al hacer rollback en updateOrderStatus:",
        rollbackError
      );
    }

    console.error("Error en updateOrderStatus:", error);
    return res.status(500).json({
      error: "Error al actualizar estado del pedido",
    });
  } finally {
    client.release();
  }
};

const getOrderTracking = async (req, res) => {
  try {
    const { id } = req.params;
    const requesterId = req.user ? Number(req.user.id) : null;
    const requesterTipo = req.user ? req.user.tipo : null;

    if (!isPositiveInteger(id)) {
      return res.status(400).json({ error: "ID de pedido inválido" });
    }

    if (!requesterId || !requesterTipo) {
      return res.status(401).json({ error: "Usuario no autenticado" });
    }

    const orderId = Number(id);

    const permissionResult = await pool.query(
      `SELECT
          a.id_usuario AS agricultor_usuario_id,
          d.id_usuario AS distribuidor_usuario_id
       FROM pedido p
       JOIN agricultor a ON p.id_agricultor = a.id_agricultor
       JOIN distribuidor d ON p.id_distribuidor = d.id_distribuidor
       WHERE p.id_pedido = $1`,
      [orderId]
    );

    if (permissionResult.rows.length === 0) {
      return res.status(404).json({ error: "Pedido no encontrado" });
    }

    const orderPermission = permissionResult.rows[0];
    const canView =
      requesterTipo === "administrador" ||
      Number(orderPermission.agricultor_usuario_id) === requesterId ||
      Number(orderPermission.distribuidor_usuario_id) === requesterId;

    if (!canView) {
      return res.status(403).json({
        error: "No tienes permiso para ver el seguimiento de este pedido",
      });
    }

    const tracking = await getOrderTrackingData(pool, orderId);

    if (!tracking) {
      return res.status(404).json({ error: "Pedido no encontrado" });
    }

    return res.json(tracking);
  } catch (error) {
    console.error("Error en getOrderTracking:", error);
    return res.status(500).json({
      error: "Error al obtener seguimiento del pedido",
    });
  }
};


const receiveOrder = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de pedido inválido" });
  }

  const orderId = Number(id);
  const requesterId = req.user ? Number(req.user.id) : null;

  if (!requesterId) {
    return res.status(401).json({ error: "Usuario no autenticado" });
  }

  const client = await pool.connect();

  try {
    await client.query("BEGIN");

    const orderResult = await client.query(
      `SELECT
          p.id_pedido,
          p.id_agricultor,
          p.estado,
          a.id_usuario
       FROM pedido p
       JOIN agricultor a ON p.id_agricultor = a.id_agricultor
       WHERE p.id_pedido = $1`,
      [orderId]
    );

    if (orderResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Pedido no encontrado" });
    }

    const order = orderResult.rows[0];

    if (Number(order.id_usuario) !== requesterId) {
      await client.query("ROLLBACK");
      return res.status(403).json({
        error: "El agricultor no tiene permiso para confirmar este pedido",
      });
    }

    if (normalizeOrderState(order.estado) !== ORDER_STATES.IN_ROUTE) {
      await client.query("ROLLBACK");
      return res.status(400).json({
        error: "Solo se puede confirmar recepción de pedidos en estado en_ruta",
      });
    }

    const updatedOrderResult = await client.query(
      `UPDATE pedido
       SET estado = $1,
           fecha_entrega_real = NOW()
       WHERE id_pedido = $2
         AND estado IN ($3, $4)
       RETURNING *`,
      [
        ORDER_STATES.DELIVERED,
        orderId,
        ORDER_STATES.IN_ROUTE,
        LEGACY_ORDER_STATES.IN_TRANSIT,
      ]
    );

    if (updatedOrderResult.rowCount === 0) {
      await client.query("ROLLBACK");
      return res.status(400).json({
        error: "El pedido ya no está en estado en_ruta",
      });
    }

    await client.query(
      `UPDATE pago
       SET estado_pago = 'completado',
           fecha_pago = COALESCE(fecha_pago, NOW())
       WHERE id_pedido = $1
         AND metodo_pago = 'contra_entrega'`,
      [orderId]
    );

    await insertOrderTracking(
      client,
      orderId,
      ORDER_STATES.DELIVERED,
      "Recepción confirmada por el agricultor"
    );

    await client.query("COMMIT");

    const orderDetail = await getOrderDetailData(pool, orderId);

    return res.json({
      message: "Recepción del pedido confirmada correctamente",
      pedido: orderDetail,
    });
  } catch (error) {
    try {
      await client.query("ROLLBACK");
    } catch (rollbackError) {
      console.error("Error al hacer rollback en receiveOrder:", rollbackError);
    }

    console.error("Error en receiveOrder:", error);
    return res.status(500).json({
      error: "Error al confirmar recepción del pedido",
    });
  } finally {
    client.release();
  }
};

module.exports = {
  createOrder,
  getOrderById,
  getOrdersByFarmer,
  getOrdersByDistributor,
  getOrderTracking,
  updateOrderStatus,
  receiveOrder,
};
