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
    const notificationResults = await Promise.allSettled(
      seguidores.map((seguimiento) =>
        this.notificacionService.crearNotificacionBajaPrecio(
          seguimiento.idAgricultor,
          productoId,
          previousPrice,
          newPrice,
          porcentajeDescuento
        )
      )
    );

    const notificacionesCreadas = [];
    const notificacionesFallidas = [];

    notificationResults.forEach((result, index) => {
      if (result.status === "fulfilled") {
        notificacionesCreadas.push(result.value);
        return;
      }

      const seguimiento = seguidores[index];
      notificacionesFallidas.push({
        id_agricultor: seguimiento.idAgricultor,
        error: result.reason?.message || "Error al crear notificación",
      });
      console.error("Error al crear notificación de baja de precio:", {
        id_agricultor: seguimiento.idAgricultor,
        id_producto: productoId,
        error: result.reason,
      });
    });

    if (notificacionesCreadas.length === 0 && notificacionesFallidas.length > 0) {
      throw new PrecioNotificacionServiceError(
        "No se pudo crear ninguna notificación de baja de precio",
        500
      );
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
      notificaciones_fallidas: notificacionesFallidas,
    };
  }
}

module.exports = {
  PrecioNotificacionService,
  PrecioNotificacionServiceError,
  calculateDiscountPercentage,
};
