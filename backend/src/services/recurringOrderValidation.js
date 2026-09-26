const FREQUENCIES = Object.freeze([
  "diaria",
  "semanal",
  "quincenal",
  "mensual",
]);

const STATES = Object.freeze(["activo", "pausado", "cancelado"]);

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const invalid = (message) => ({ error: { statusCode: 400, message } });

const normalizeProducts = (products) => {
  if (!Array.isArray(products) || products.length === 0) {
    return invalid("Debe enviar al menos un producto recurrente");
  }

  const normalized = [];
  const inventoryIds = new Set();

  for (const product of products) {
    if (!product || !isPositiveInteger(product.id_inventario)) {
      return invalid("id_inventario inválido en productos");
    }

    if (!isPositiveInteger(product.cantidad)) {
      return invalid("cantidad inválida en productos");
    }

    const inventoryId = Number(product.id_inventario);
    if (inventoryIds.has(inventoryId)) {
      return invalid("No se permiten productos repetidos");
    }

    inventoryIds.add(inventoryId);
    normalized.push({
      id_inventario: inventoryId,
      cantidad: Number(product.cantidad),
    });
  }

  return { value: normalized };
};

const normalizeDate = (value, { required = true } = {}) => {
  if ((value === undefined || value === null || value === "") && !required) {
    return { value: undefined };
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return invalid("fecha_proximo inválida");
  }

  return { value: parsed.toISOString() };
};

const normalizeConfiguration = (body, current = null) => {
  const productsSource = body.productos === undefined
    ? current?.items
    : body.productos;
  const products = normalizeProducts(productsSource);
  if (products.error) return products;

  const distributorSource = body.id_distribuidor === undefined
    ? current?.id_distribuidor
    : body.id_distribuidor;
  if (!isPositiveInteger(distributorSource)) {
    return invalid("id_distribuidor inválido");
  }

  const addressSource = body.direccion_entrega === undefined
    ? current?.direccion_entrega
    : body.direccion_entrega;
  if (typeof addressSource !== "string" || addressSource.trim().length < 5) {
    return invalid("direccion_entrega inválida");
  }

  const paymentSource = body.metodo_pago === undefined
    ? current?.metodo_pago
    : body.metodo_pago;
  const normalizedPayment = typeof paymentSource === "string"
    ? paymentSource.trim().toLowerCase()
    : "";
  if (!["efectivo", "contra_entrega"].includes(normalizedPayment)) {
    return invalid("metodo_pago inválido, use efectivo o contra_entrega");
  }

  return {
    value: {
      id_distribuidor: Number(distributorSource),
      direccion_entrega: addressSource.trim(),
      metodo_pago: "contra_entrega",
      items: products.value,
    },
  };
};

const validateCreateRecurringOrder = (body = {}) => {
  if (!FREQUENCIES.includes(body.frecuencia)) {
    return invalid(`frecuencia inválida. Use: ${FREQUENCIES.join(" | ")}`);
  }

  const configuration = normalizeConfiguration(body);
  if (configuration.error) return configuration;

  const date = normalizeDate(body.fecha_proximo ?? new Date().toISOString());
  if (date.error) return date;

  return {
    value: {
      frecuencia: body.frecuencia,
      productos: configuration.value,
      fecha_proximo: date.value,
    },
  };
};

const validateUpdateRecurringOrder = (body = {}, current) => {
  const allowed = new Set([
    "frecuencia",
    "productos",
    "fecha_proximo",
    "estado",
    "accion",
    "id_distribuidor",
    "direccion_entrega",
    "metodo_pago",
  ]);
  const supplied = Object.keys(body);
  if (supplied.length === 0 || supplied.some((key) => !allowed.has(key))) {
    return invalid("No hay campos válidos para actualizar");
  }

  let estado = body.estado;
  if (body.accion !== undefined) {
    const actions = { pausar: "pausado", reanudar: "activo", cancelar: "cancelado" };
    if (!actions[body.accion]) return invalid("accion inválida");
    if (estado !== undefined && estado !== actions[body.accion]) {
      return invalid("estado y accion son incompatibles");
    }
    estado = actions[body.accion];
  }

  if (estado !== undefined && !STATES.includes(estado)) {
    return invalid(`estado inválido. Use: ${STATES.join(" | ")}`);
  }

  const frecuencia = body.frecuencia ?? current.frecuencia;
  if (!FREQUENCIES.includes(frecuencia)) {
    return invalid(`frecuencia inválida. Use: ${FREQUENCIES.join(" | ")}`);
  }

  const configurationFields = [
    "productos",
    "id_distribuidor",
    "direccion_entrega",
    "metodo_pago",
  ];
  const changesConfiguration = configurationFields.some((key) => body[key] !== undefined);
  const configuration = changesConfiguration
    ? normalizeConfiguration(body, current.productos)
    : { value: current.productos };
  if (configuration.error) return configuration;

  const date = normalizeDate(body.fecha_proximo, { required: false });
  if (date.error) return date;

  return {
    value: {
      frecuencia,
      productos: configuration.value,
      fecha_proximo: date.value ?? current.fecha_proximo,
      estado: estado ?? current.estado,
    },
  };
};

module.exports = {
  FREQUENCIES,
  STATES,
  validateCreateRecurringOrder,
  validateUpdateRecurringOrder,
};
