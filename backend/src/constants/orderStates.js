const ORDER_STATES = {
  CONFIRMED: "confirmado",
  PREPARING: "preparando",
  IN_ROUTE: "en_ruta",
  DELIVERED: "entregado",
  CANCELED: "cancelado",
};

const LEGACY_ORDER_STATES = {
  PENDING: "pendiente",
  IN_TRANSIT: "en_camino",
};

// Estados que el agricultor/distribuidor pueden filtrar
const ORDER_STATES_FILTERABLE = [
  ORDER_STATES.CONFIRMED,
  ORDER_STATES.PREPARING,
  ORDER_STATES.IN_ROUTE,
  ORDER_STATES.DELIVERED,
  ORDER_STATES.CANCELED,
];

// Estados a los que el distribuidor puede transicionar un pedido
const ORDER_STATES_UPDATEABLE = [
  ORDER_STATES.CONFIRMED,
  ORDER_STATES.PREPARING,
  ORDER_STATES.IN_ROUTE,
  ORDER_STATES.DELIVERED,
  ORDER_STATES.CANCELED,
];

const normalizeOrderState = (estado) => {
  if (estado === LEGACY_ORDER_STATES.PENDING) {
    return ORDER_STATES.CONFIRMED;
  }

  if (estado === LEGACY_ORDER_STATES.IN_TRANSIT) {
    return ORDER_STATES.IN_ROUTE;
  }

  return estado;
};

module.exports = {
  ORDER_STATES,
  LEGACY_ORDER_STATES,
  ORDER_STATES_FILTERABLE,
  ORDER_STATES_UPDATEABLE,
  normalizeOrderState,
};
