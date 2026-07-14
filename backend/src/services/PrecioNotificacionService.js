const { NOTIFICATION_TYPES } = require("../constants/notificationTypes");
const ProductoSeguidoRepository = require("../repositories/ProductoSeguidoRepository");

class PrecioNotificacionServiceError extends Error {
  constructor(message, statusCode = 400) {
    super(message);
    this.name = "PrecioNotificacionServiceError";
    this.statusCode = statusCode;
  }
}

const parsePositiveInteger = (value, fieldName) => {
  const parsed = Number(value);

  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new PrecioNotificacionServiceError(`${fieldName} inválido`, 400);
  }

  return parsed;
};

const parsePositivePrice = (value, fieldName) => {
  const parsed = Number(value);

  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new PrecioNotificacionServiceError(`${fieldName} debe ser un precio positivo`, 400);
  }

  return parsed;
};

const roundMoney = (value) => Number(Number(value).toFixed(2));

const calculateDiscountPercentage = (precioAnterior, precioNuevo) =>
  Number((((precioAnterior - precioNuevo) / precioAnterior) * 100).toFixed(2));

class PrecioNotificacionService {
  constructor(productoSeguidoRepository = new ProductoSeguidoRepository()) {
    this.productoSeguidoRepository = productoSeguidoRepository;
  }

  async verificarYNotificarBajaDePrecio(idProducto, precioAnterior, precioNuevo) {
    const productoId = parsePositiveInteger(idProducto, "id_producto");
    const previousPrice = parsePositivePrice(precioAnterior, "precio_anterior");
    const newPrice = parsePositivePrice(precioNuevo, "precio_nuevo");

    if (newPrice >= previousPrice) {
      return {
        hay_baja_precio: false,
        tipo_notificacion: NOTIFICATION_TYPES.BAJA_PRECIO,
        id_producto: productoId,
        precio_anterior: roundMoney(previousPrice),
        precio_nuevo: roundMoney(newPrice),
        porcentaje_descuento: 0,
        seguidores: [],
      };
    }

    const seguidores = await this.productoSeguidoRepository.findByIdProducto(productoId);

    return {
      hay_baja_precio: true,
      tipo_notificacion: NOTIFICATION_TYPES.BAJA_PRECIO,
      id_producto: productoId,
      precio_anterior: roundMoney(previousPrice),
      precio_nuevo: roundMoney(newPrice),
      porcentaje_descuento: calculateDiscountPercentage(previousPrice, newPrice),
      seguidores,
    };
  }
}

module.exports = {
  PrecioNotificacionService,
  PrecioNotificacionServiceError,
  calculateDiscountPercentage,
};
