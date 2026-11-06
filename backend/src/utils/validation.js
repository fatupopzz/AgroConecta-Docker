const parsePositiveInteger = (value, fieldName, ErrorClass) => {
  const parsed = Number(value);

  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new ErrorClass(`${fieldName} inválido`, 400);
  }

  return parsed;
};

const parsePositivePrice = (value, fieldName, ErrorClass) => {
  const parsed = Number(value);

  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new ErrorClass(`${fieldName} debe ser un precio positivo`, 400);
  }

  return parsed;
};

module.exports = {
  parsePositiveInteger,
  parsePositivePrice,
};
