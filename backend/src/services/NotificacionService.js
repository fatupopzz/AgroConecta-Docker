const { pool } = require("../config/db");
const { NOTIFICATION_TYPES } = require("../constants/notificationTypes");

class NotificacionServiceError extends Error {
  constructor(message, statusCode = 400) {
    super(message);
    this.name = "NotificacionServiceError";
    this.statusCode = statusCode;
  }
}

const parsePositiveInteger = (value, fieldName) => {
  const parsed = Number(value);

  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new NotificacionServiceError(`${fieldName} inválido`, 400);
  }

  return parsed;
};

const parsePositivePrice = (value, fieldName) => {
  const parsed = Number(value);

  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new NotificacionServiceError(`${fieldName} debe ser un precio positivo`, 400);
  }

  return Number(parsed.toFixed(2));
};

const parseDiscountPercentage = (value) => {
  const parsed = Number(value);

  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new NotificacionServiceError("porcentaje_descuento inválido", 400);
  }

  return Number(parsed.toFixed(2));
};

class NotificacionService {
  constructor(db = pool) {
    this.db = db;
  }

  async crearNotificacionBajaPrecio(
    idAgricultor,
    idProducto,
    precioAnterior,
    precioNuevo,
    porcentajeDescuento
  ) {
    const agricultorId = parsePositiveInteger(idAgricultor, "id_agricultor");
    const productoId = parsePositiveInteger(idProducto, "id_producto");
    const previousPrice = parsePositivePrice(precioAnterior, "precio_anterior");
    const newPrice = parsePositivePrice(precioNuevo, "precio_nuevo");
    const discountPercentage = parseDiscountPercentage(porcentajeDescuento);
    const product = await this.getProduct(productoId);

    const contenido = {
      id_producto: productoId,
      producto: product.nombre,
      precio_anterior: previousPrice,
      precio_nuevo: newPrice,
      porcentaje_descuento: discountPercentage,
    };

    const result = await this.db.query(
      `INSERT INTO notificacion
         (id_agricultor, tipo, contenido)
       VALUES ($1, $2, $3)
       RETURNING *`,
      [agricultorId, NOTIFICATION_TYPES.BAJA_PRECIO, contenido]
    );

    return result.rows[0];
  }

  async getProduct(idProducto) {
    const result = await this.db.query(
      `SELECT id_producto, nombre
       FROM producto
       WHERE id_producto = $1
         AND activo = true`,
      [idProducto]
    );

    if (result.rowCount === 0) {
      throw new NotificacionServiceError("Producto no encontrado", 404);
    }

    return result.rows[0];
  }
}

module.exports = {
  NotificacionService,
  NotificacionServiceError,
};
