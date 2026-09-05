const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const invalid = (message) => ({
  error: {
    statusCode: 400,
    message,
  },
});

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

const validateCheckoutRequest = ({ farmerId, body }) => {
  const {
    id_distribuidor,
    direccion_entrega,
    productos,
    metodo_pago,
    esUrgente = false,
    tipoPlaga,
  } = body;

  if (!isPositiveInteger(farmerId)) {
    return invalid("id_agricultor inválido");
  }

  if (!isPositiveInteger(id_distribuidor)) {
    return invalid("id_distribuidor inválido");
  }

  if (
    typeof direccion_entrega !== "string" ||
    direccion_entrega.trim().length < 5
  ) {
    return invalid("direccion_entrega inválida");
  }

  if (!Array.isArray(productos) || productos.length === 0) {
    return invalid("Debe enviar al menos un producto en el pedido");
  }

  if (typeof esUrgente !== "boolean") {
    return invalid("esUrgente debe ser boolean");
  }

  if (
    tipoPlaga !== undefined &&
    tipoPlaga !== null &&
    (typeof tipoPlaga !== "string" ||
      tipoPlaga.trim().length === 0 ||
      tipoPlaga.trim().length > 100)
  ) {
    return invalid("tipoPlaga debe ser un texto de 1 a 100 caracteres");
  }

  const paymentMethod = normalizeCashPaymentMethod(metodo_pago);
  if (!paymentMethod) {
    return invalid("metodo_pago inválido, use efectivo o contra_entrega");
  }

  const normalizedProducts = [];
  const inventoryIds = [];

  for (const item of productos) {
    if (!item || !isPositiveInteger(item.id_inventario)) {
      return invalid("id_inventario inválido en productos");
    }

    if (!isPositiveInteger(item.cantidad)) {
      return invalid("cantidad inválida en productos");
    }

    const normalizedItem = {
      id_inventario: Number(item.id_inventario),
      cantidad: Number(item.cantidad),
    };

    normalizedProducts.push(normalizedItem);
    inventoryIds.push(normalizedItem.id_inventario);
  }

  if (new Set(inventoryIds).size !== inventoryIds.length) {
    return invalid("No se permiten productos repetidos en el pedido");
  }

  return {
    value: {
      farmerId: Number(farmerId),
      distributorId: Number(id_distribuidor),
      deliveryAddress: direccion_entrega.trim(),
      urgent: esUrgente,
      pestType: typeof tipoPlaga === "string" ? tipoPlaga.trim() : null,
      paymentMethod,
      normalizedProducts,
      inventoryIds,
    },
  };
};

module.exports = {
  normalizeCashPaymentMethod,
  validateCheckoutRequest,
};
