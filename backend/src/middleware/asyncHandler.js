/**
 * Envuelve un handler async de Express 4 para que cualquier error o promesa
 * rechazada llegue al middleware central de errores mediante next(error).
 */
const asyncHandler = (handler) => (req, res, next) => {
  Promise.resolve()
    .then(() => handler(req, res, next))
    .catch(next);
};

module.exports = asyncHandler;
