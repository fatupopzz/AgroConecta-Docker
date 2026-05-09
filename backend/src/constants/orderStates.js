const ORDER_STATES = {
  PENDING: "pendiente",
  CONFIRMED: "confirmado",
  IN_TRANSIT: "en_camino",
  DELIVERED: "entregado",
  CANCELED: "cancelado",
};

// Estados que el agricultor/distribuidor pueden filtrar
const ORDER_STATES_FILTERABLE = [
  ORDER_STATES.PENDING,
  ORDER_STATES.CONFIRMED,
  ORDER_STATES.IN_TRANSIT,
  ORDER_STATES.DELIVERED,
  ORDER_STATES.CANCELED,
];

// Estados a los que el distribuidor puede transicionar un pedido
const ORDER_STATES_UPDATEABLE = [
  ORDER_STATES.CONFIRMED,
  ORDER_STATES.IN_TRANSIT,
  ORDER_STATES.DELIVERED,
];

module.exports = {
  ORDER_STATES,
  ORDER_STATES_FILTERABLE,
  ORDER_STATES_UPDATEABLE,
};
