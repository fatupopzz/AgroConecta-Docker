const { NOTIFICATION_TYPES } = require("../constants/notificationTypes");
const { NotificacionService } = require("./NotificacionService");
const ProductoSeguidoRepository = require("../repositories/ProductoSeguidoRepository");
const CompraFrecuenteRepository = require("../repositories/CompraFrecuenteRepository");
const AppError = require("../errors/AppError");

class PrecioNotificacionServiceError extends AppError {
  constructor(message, statusCode = 400) {
    super(statusCode, message);
    this.name = "PrecioNotificacionServiceError";
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

const normalizeOptionalMoney = (value) => {
  if (!Number.isFinite(value) || value <= 0) {
    return 0;
  }

  return roundMoney(value);
};

const calculateDiscountPercentage = (precioAnterior, precioNuevo) => {
  if (!Number.isFinite(precioAnterior) || precioAnterior <= 0 || !Number.isFinite(precioNuevo)) {
    return 0;
  }

  const discountPercentage = ((precioAnterior - precioNuevo) / precioAnterior) * 100;
  return Number(Math.max(discountPercentage, 0).toFixed(2));
};

const NOTIFICATION_BATCH_SIZE = 10;

const createNotificationsWithLimit = async (
  seguidores,
  notificacionService,
  productoId,
  previousPrice,
  newPrice,
  porcentajeDescuento,
  product
) => {
  const results = [];

  for (let index = 0; index < seguidores.length; index += NOTIFICATION_BATCH_SIZE) {
    const batch = seguidores.slice(index, index + NOTIFICATION_BATCH_SIZE);
    const batchResults = await Promise.allSettled(
      batch.map((seguimiento) =>
        notificacionService.crearNotificacionBajaPrecio(
          seguimiento.idAgricultor,
          productoId,
          previousPrice,
          newPrice,
          porcentajeDescuento,
          product
        )
      )
    );

    batchResults.forEach((result, batchIndex) => {
      results.push({ result, seguimiento: batch[batchIndex] });
    });
  }

  return results;
};

class PrecioNotificacionService {
  constructor(
    productoSeguidoRepository = new ProductoSeguidoRepository(),
    notificacionService = new NotificacionService(),
    compraFrecuenteRepository = new CompraFrecuenteRepository()
  ) {
    this.productoSeguidoRepository = productoSeguidoRepository;
    this.notificacionService = notificacionService;
    this.compraFrecuenteRepository = compraFrecuenteRepository;
  }

  async verificarYNotificarBajaDePrecio(idProducto, precioAnterior, precioNuevo) {
    const productoId = parsePositiveInteger(idProducto, "id_producto");
    const previousPrice = Number(precioAnterior);
    const newPrice = parsePositivePrice(precioNuevo, "precio_nuevo");

    if (!Number.isFinite(previousPrice) || previousPrice <= 0 || newPrice >= previousPrice) {
      return {
        hay_baja_precio: false,
        tipo_notificacion: NOTIFICATION_TYPES.BAJA_PRECIO,
        id_producto: productoId,
        precio_anterior: normalizeOptionalMoney(previousPrice),
        precio_nuevo: roundMoney(newPrice),
        porcentaje_descuento: 0,
        seguidores: [],
        notificaciones_creadas: [],
      };
    }

    // Una consulta de producto por cambio, no por destinatario.
    const product = await this.notificacionService.getProduct(productoId);
    const [manuales, frecuentes] = await Promise.all([
      this.productoSeguidoRepository.findByIdProducto(productoId),
      this.compraFrecuenteRepository.findByIdProducto(productoId),
    ]);
    const seguidores = [...new Map([...manuales, ...frecuentes].map(
      destinatario => [Number(destinatario.idAgricultor), destinatario]
    )).values()];
    const porcentajeDescuento = calculateDiscountPercentage(previousPrice, newPrice);
    const notificationResults = await createNotificationsWithLimit(
      seguidores,
      this.notificacionService,
      productoId,
      previousPrice,
      newPrice,
      porcentajeDescuento,
      product
    );

    const notificacionesCreadas = [];
    const notificacionesFallidas = [];

    notificationResults.forEach(({ result, seguimiento }) => {
      if (result.status === "fulfilled") {
        notificacionesCreadas.push(result.value);
        return;
      }

      notificacionesFallidas.push({
        id_agricultor: seguimiento.idAgricultor,
        error: "Error al crear notificación",
      });
      console.error("Error al crear notificación de baja de precio:", {
        id_agricultor: seguimiento.idAgricultor,
        id_producto: productoId,
        error: "Error al persistir notificación",
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
