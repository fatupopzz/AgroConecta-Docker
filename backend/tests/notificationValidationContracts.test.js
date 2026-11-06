const notification = require("../src/services/NotificacionService");
const price = require("../src/services/PrecioNotificacionService");
const followed = require("../src/services/ProductoSeguidoService");

const invalid = [0, -1, 1.5, "", "   ", "abc", null, undefined, NaN, Infinity];

function expectServiceError(error, ErrorClass, message) {
  expect(error).toBeInstanceOf(ErrorClass);
  expect(error.name).toBe(ErrorClass.name);
  expect(error.message).toBe(message);
  expect(error.statusCode).toBe(400);
}

const cases = [
  ["notification farmer", notification.NotificacionServiceError, "id_agricultor", (v) => new notification.NotificacionService({}).crearNotificacionBajaPrecio(v, 4, 100, 75, 25)],
  ["notification product", notification.NotificacionServiceError, "id_producto", (v) => new notification.NotificacionService({}).crearNotificacionBajaPrecio(7, v, 100, 75, 25)],
  ["price product", price.PrecioNotificacionServiceError, "id_producto", (v) => new price.PrecioNotificacionService({}, {}, {}).verificarYNotificarBajaDePrecio(v, 100, 75)],
];
for (const method of ["seguirProducto", "dejarDeSeguir", "estaSiguiendo"]) {
  for (const field of ["id_agricultor", "id_producto"]) {
    cases.push([`${method} ${field}`, followed.ProductoSeguidoServiceError, field,
      (v) => new followed.ProductoSeguidoService({}, {})[method](field === "id_agricultor" ? v : 7, field === "id_producto" ? v : 4)]);
  }
}

describe.each(cases)("%s", (_label, ErrorClass, field, invoke) => {
  test.each(invalid)("rejects %s with the public error contract", async (value) => {
    await expect(invoke(value)).rejects.toSatisfy(error => {
      expectServiceError(error, ErrorClass, `${field} inválido`);
      return true;
    });
  });
});

for (const [field, invoke, ErrorClass] of [
  ["precio_anterior", v => new notification.NotificacionService({}).crearNotificacionBajaPrecio(7, 4, v, 75, 25), notification.NotificacionServiceError],
  ["precio_nuevo", v => new notification.NotificacionService({}).crearNotificacionBajaPrecio(7, 4, 100, v, 25), notification.NotificacionServiceError],
  ["precio_nuevo", v => new price.PrecioNotificacionService({}, {}, {}).verificarYNotificarBajaDePrecio(4, 100, v), price.PrecioNotificacionServiceError],
]) {
  test.each([0, -1, "", "   ", "abc", null, undefined, NaN, Infinity])(`${ErrorClass.name} ${field}: %s`, async value => {
    await expect(invoke(value)).rejects.toSatisfy(error => {
      expectServiceError(error, ErrorClass, `${field} debe ser un precio positivo`);
      return true;
    });
  });
}

test.each(["seguirProducto", "dejarDeSeguir", "estaSiguiendo"])("%s converts string IDs without changing the response", async method => {
  const row = { id: 3, id_agricultor: 7, id_producto: 4, precio_al_seguir: "75.12", fecha: "2026-01-01" };
  const entity = require("../src/models/ProductoSeguido").fromRow(row);
  const query = vi.fn().mockResolvedValue({ rowCount: 1, rows: [row] });
  const repository = {
    existsByIdAgricultorAndIdProducto: vi.fn().mockResolvedValue(false),
    deleteByIdAgricultorAndIdProducto: vi.fn().mockResolvedValue(entity),
    findByIdAgricultorAndIdProducto: vi.fn().mockResolvedValue(entity),
  };
  const service = new followed.ProductoSeguidoService({ query }, repository);
  service.ensureAgricultorExists = vi.fn().mockResolvedValue();
  service.ensureProductExists = vi.fn().mockResolvedValue();
  service.getProductWithCurrentPrice = vi.fn().mockResolvedValue({ precio_actual: "75.12" });
  expect(await service[method]("7", "4")).toEqual({
    siguiendo: method !== "dejarDeSeguir",
    producto_seguido: { id: 3, id_agricultor: 7, id_producto: 4, precio_al_seguir: 75.12, fecha: "2026-01-01" },
  });
  expect(service.ensureAgricultorExists).toHaveBeenCalledWith(7);
  const repoMethod = method === "seguirProducto" ? "existsByIdAgricultorAndIdProducto" : method === "dejarDeSeguir" ? "deleteByIdAgricultorAndIdProducto" : "findByIdAgricultorAndIdProducto";
  expect(repository[repoMethod]).toHaveBeenCalledWith(7, 4);
});

test("public module exports remain unchanged", () => {
  expect(Object.keys(notification).sort()).toEqual(["NotificacionService", "NotificacionServiceError"]);
  expect(Object.keys(price).sort()).toEqual(["PrecioNotificacionService", "PrecioNotificacionServiceError", "calculateDiscountPercentage"]);
  expect(Object.keys(followed).sort()).toEqual(["ProductoSeguidoService", "ProductoSeguidoServiceError"]);
});
