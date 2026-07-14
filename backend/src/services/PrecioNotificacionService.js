const { NOTIFICATION_TYPES } = require("../constants/notificationTypes");
const { NotificacionService } = require("./NotificacionService");
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
  constructor(
    productoSeguidoRepository = new ProductoSeguidoRepository(),
    notificacionService = new NotificacionService()
  ) {
    this.productoSeguidoRepository = productoSeguidoRepository;
    this.notificacionService = notificacionService;
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
        notificaciones_creadas: [],
      };
    }

    const seguidores = await this.productoSeguidoRepository.findByIdProducto(productoId);
    const porcentajeDescuento = calculateDiscountPercentage(previousPrice, newPrice);
    const notificacionesCreadas = [];

    for (const seguimiento of seguidores) {
      const notificacion = await this.notificacionService.crearNotificacionBajaPrecio(
        seguimiento.idAgricultor,
        productoId,
        previousPrice,
        newPrice,
        porcentajeDescuento
      );
      notificacionesCreadas.push(notificacion);
    }

    return {
      hay_baja_precio: true,
      tipo_notificacion: NOTIFICATION_TYPES.BAJA_PRECIO,
      id_producto: productoId,
      precio_anterior: roundMoney(previousPrice),
      precio_nuevo: roundMoney(newPrice),
      porcentaje_descuento: porcentajeDescuento,
      seguidores,
      notificaciones_creadas: notificacionesCreadas,
    };
  }
}

module.exports = {
  PrecioNotificacionService,
  PrecioNotificacionServiceError,
  calculateDiscountPercentage,
};
