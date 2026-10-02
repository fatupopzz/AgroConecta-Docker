/**
 * Error de negocio con código HTTP. Lanzarlo desde un controller hace que el
 * middleware central responda con ese status y { error: message }.
 */
class AppError extends Error {
  constructor(statusCode, message, details) {
    super(message);
    this.name = "AppError";
    this.statusCode = statusCode;
    this.details = details;
  }
}

module.exports = AppError;
