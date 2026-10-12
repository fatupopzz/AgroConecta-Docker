const assert = require("node:assert/strict");
const { PrecioNotificacionService } = require("../src/services/PrecioNotificacionService");
const { NotificacionService } = require("../src/services/NotificacionService");
const CompraFrecuenteRepository = require("../src/repositories/CompraFrecuenteRepository");

function setup(manual = [1], frequent = [2, 1], fail = []) {
  const sent = [];
  let productReads = 0;
  const notifications = {
    getProduct: async () => { productReads++; return { id_producto: 4, nombre: "Maíz" }; },
    crearNotificacionBajaPrecio: async (...args) => {
      sent.push(args);
      if (fail.includes(args[0])) throw new Error("private error must not be logged");
      return { id: args[0] };
    }
  };
  const service = new PrecioNotificacionService(
    { findByIdProducto: async () => manual.map(idAgricultor => ({ idAgricultor })) },
    notifications,
    { findByIdProducto: async () => frequent.map(idAgricultor => ({ idAgricultor })) }
  );
  return { service, sent, notifications, productReads: () => productReads };
}

test("manual followers and frequent buyers are deduplicated; product read once", async () => {
  const fixture = setup();
  const result = await fixture.service.verificarYNotificarBajaDePrecio(4, 100, 75);
  assert.equal(result.notificaciones_creadas.length, 2);
  assert.deepEqual(fixture.sent.map(args => args[0]), [1, 2]);
  assert.deepEqual(fixture.sent[0], [1, 4, 100, 75, 25, { id_producto: 4, nombre: "Maíz" }]);
  assert.equal(fixture.productReads(), 1);
});

test.each([100, 110])("unchanged or increased price %s sends nothing", async (price) => {
  const fixture = setup();
  const result = await fixture.service.verificarYNotificarBajaDePrecio(4, 100, price);
  assert.equal(result.hay_baja_precio, false);
  assert.equal(fixture.sent.length, 0);
  assert.equal(fixture.productReads(), 0);
});

test("manual follower qualifies without purchases", async () => {
  const fixture = setup([7], []);
  await fixture.service.verificarYNotificarBajaDePrecio(4, 3, 2);
  assert.equal(fixture.sent[0][0], 7);
  assert.equal(fixture.sent[0][4], 33.33);
});

test("partial failure neither retries nor duplicates successful recipients", async () => {
  const fixture = setup([1, 2], [1, 2, 3], [2]);
  const log = vi.spyOn(console, "error").mockImplementation(() => {});
  try {
    const result = await fixture.service.verificarYNotificarBajaDePrecio(4, 100, 75);
    assert.equal(result.notificaciones_creadas.length, 2);
    assert.equal(result.notificaciones_fallidas.length, 1);
    assert.deepEqual(fixture.sent.map(args => args[0]), [1, 2, 3]);
    assert.ok(!JSON.stringify(log.mock.calls).includes("private error"));
  } finally { log.mockRestore(); }
});

test("inactive product sends no notifications", async () => {
  const fixture = setup();
  fixture.notifications.getProduct = async () => { throw new Error("Producto no encontrado"); };
  await assert.rejects(fixture.service.verificarYNotificarBajaDePrecio(4, 100, 75));
  assert.equal(fixture.sent.length, 0);
});

test("persistent notification retains compatible type and price JSON", async () => {
  const calls = [];
  const service = new NotificacionService({ query: async (...args) => {
    calls.push(args); return { rows: [{ id_notificacion: 1 }] };
  } });
  await service.crearNotificacionBajaPrecio(7, 4, 100, 75, 25, { nombre: "Maíz" });
  assert.equal(calls.length, 1);
  assert.equal(calls[0][1][0], 7);
  assert.equal(calls[0][1][1], "baja_precio");
  assert.deepEqual(JSON.parse(calls[0][1][2]), {
    id_producto: 4, producto: "Maíz", precio_anterior: 100, precio_nuevo: 75, porcentaje_descuento: 25
  });
});

test("frequent lookup is parameterized and maps farmer IDs", async () => {
  const repo = new CompraFrecuenteRepository({ query: async (sql, params) => {
    assert.deepEqual(params, [4]);
    assert.match(sql, /COUNT\(DISTINCT p.id_pedido\)/);
    return { rows: [{ id_agricultor: 7 }] };
  } });
  assert.deepEqual(await repo.findByIdProducto(4), [{ idAgricultor: 7 }]);
});
