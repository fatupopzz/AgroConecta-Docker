const AppError = require("../errors/AppError");

const INTERNAL_ERROR_MESSAGE = "Error interno del servidor";

// Errores de PostgreSQL que se deben a datos enviados por el cliente.
const POSTGRES_ERRORS = {
  "23505": { statusCode: 409, message: "El registro ya existe" },
  "23503": { statusCode: 409, message: "El registro hace referencia a datos inexistentes o en uso" },
  "23502": { statusCode: 400, message: "Faltan campos obligatorios" },
  "23514": { statusCode: 400, message: "Los datos no cumplen las restricciones" },
  "22P02": { statusCode: 400, message: "Formato de dato inválido" },
  "22001": { statusCode: 400, message: "Un valor excede la longitud permitida" },
  "22003": { statusCode: 400, message: "Un valor numérico está fuera de rango" },
};

const toHttpError = (error) => {
  if (error instanceof AppError) {
    return {
      statusCode: error.statusCode,
      body: error.details === undefined
        ? { error: error.message }
        : { error: error.message, details: error.details },
    };
  }

  // JSON mal formado o cuerpo demasiado grande (body-parser).
  if (error?.type === "entity.parse.failed") {
    return { statusCode: 400, body: { error: "JSON inválido" } };
  }
  if (error?.type === "entity.too.large") {
    return { statusCode: 413, body: { error: "La solicitud es demasiado grande" } };
  }

  const postgresError = POSTGRES_ERRORS[error?.code];
  if (postgresError) {
    return {
      statusCode: postgresError.statusCode,
      body: { error: postgresError.message },
    };
  }

  return { statusCode: 500, body: { error: INTERNAL_ERROR_MESSAGE } };
};

const notFoundHandler = (req, res) => {
  res.status(404).json({ error: "Ruta no encontrada" });
};

// Express identifica el middleware de errores por sus cuatro parámetros.
const errorHandler = (error, req, res, next) => {
  const { statusCode, body } = toHttpError(error);

  if (statusCode >= 500) {
    console.error(`Error en ${req.method} ${req.originalUrl}:`, error);
  }

  if (res.headersSent) {
    return next(error);
  }

  return res.status(statusCode).json(body);
};

module.exports = {
  errorHandler,
  notFoundHandler,
  toHttpError,
  INTERNAL_ERROR_MESSAGE,
};
