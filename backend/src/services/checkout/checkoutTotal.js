const checkoutError = (statusCode, message) => ({
  error: { statusCode, message },
});

// CART-01: la vista del carrito conserva los subtotales calculados por PostgreSQL.
const calculateCartTotal = (items) =>
  items.reduce((sum, item) => sum + Number(item.subtotal), 0);

const validateInventoryAndCalculateTotal = ({
  inventoryRows,
  inventoryIds,
  normalizedProducts,
  distributorId,
}) => {
  if (inventoryRows.length !== inventoryIds.length) {
    return checkoutError(
      404,
      "Uno o más productos del pedido no existen en inventario"
    );
  }

  const inventoryMap = new Map();
  for (const row of inventoryRows) {
    inventoryMap.set(Number(row.id_inventario), row);
  }

  // CART-01: al crear el pedido, inventario vuelve a ser la fuente autoritativa.
  let total = 0;

  for (const item of normalizedProducts) {
    const inventory = inventoryMap.get(item.id_inventario);

    if (!inventory) {
      return checkoutError(404, "Producto de inventario no encontrado");
    }

    if (Number(inventory.id_distribuidor) !== distributorId) {
      return checkoutError(
        400,
        "Todos los productos deben pertenecer al distribuidor indicado"
      );
    }

    if (Number(inventory.stock_disponible) < item.cantidad) {
      return checkoutError(
        400,
        `Stock insuficiente para el producto ${inventory.producto_nombre}`
      );
    }

    total += Number(inventory.precio) * item.cantidad;
  }

  return {
    value: {
      inventoryMap,
      total: Number(total.toFixed(2)),
    },
  };
};

module.exports = {
  calculateCartTotal,
  validateInventoryAndCalculateTotal,
};
