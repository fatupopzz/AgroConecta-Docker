const { parsePositiveInteger, parsePositivePrice } = require("../src/utils/validation");

class ValidationError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.statusCode = statusCode;
  }
}
class AnotherValidationError extends ValidationError {}

describe("parsePositiveInteger", () => {
  test.each([[7, 7], ["7", 7]])("converts %s to %s", (value, expected) => {
    expect(parsePositiveInteger(value, "id_producto", ValidationError)).toBe(expected);
  });
});

describe("parsePositivePrice", () => {
  test.each([[7, 7], [7.25, 7.25], ["7.25", 7.25], [1.004, 1.004], ["0.004", 0.004]])(
    "converts %s without rounding", (value, expected) => {
      expect(parsePositivePrice(value, "precio_nuevo", ValidationError)).toBe(expected);
    }
  );
});

for (const ErrorClass of [ValidationError, AnotherValidationError]) {
  for (const [validator, field, message, invalid] of [
    [parsePositiveInteger, "id_producto", "id_producto inválido", [0, -1, 1.5, "", "   ", "abc", null, undefined, NaN, Infinity]],
    [parsePositivePrice, "precio_nuevo", "precio_nuevo debe ser un precio positivo", [0, -1, "", "   ", "abc", null, undefined, NaN, Infinity]],
  ]) {
    describe(`${validator.name} with ${ErrorClass.name}`, () => {
      test.each(invalid)("rejects %s with the exact error contract", value => {
        let error;
        try { validator(value, field, ErrorClass); } catch (caught) { error = caught; }
        expect(error).toBeInstanceOf(ErrorClass);
        expect(error.constructor).toBe(ErrorClass);
        expect(error.message).toBe(message);
        expect(error.statusCode).toBe(400);
      });
    });
  }
}
