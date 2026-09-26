const assert = require("node:assert/strict");
const test = require("node:test");

const dbPath = require.resolve("../src/config/db");
const controllerPath = require.resolve("../src/controllers/recurringOrderController");
const schedulerPath = require.resolve("../src/services/recurringOrderScheduler");
const {
  validateCreateRecurringOrder,
  validateUpdateRecurringOrder,
} = require("../src/services/recurringOrderValidation");

const response = () => ({
  statusCode: 200,
  body: null,
  status(code) {
    this.statusCode = code;
    return this;
  },
  json(body) {
    this.body = body;
    return this;
  },
});

const loadWithPool = (modulePath, pool) => {
  delete require.cache[modulePath];
  require.cache[dbPath] = {
    id: dbPath,
    filename: dbPath,
    loaded: true,
    exports: { pool },
  };
  return require(modulePath);
};

const configuration = {
  frecuencia: "semanal",
  id_distribuidor: 3,
  direccion_entrega: "Parcela norte, Guatemala",
  metodo_pago: "efectivo",
  productos: [{ id_inventario: 8, cantidad: 2 }],
  fecha_proximo: "2026-10-01T12:00:00.000Z",
};

test("valida y normaliza la configuracion de un pedido recurrente", () => {
  const result = validateCreateRecurringOrder(configuration);
  assert.deepEqual(result.value, {
    frecuencia: "semanal",
    productos: {
      id_distribuidor: 3,
      direccion_entrega: "Parcela norte, Guatemala",
      metodo_pago: "contra_entrega",
      items: [{ id_inventario: 8, cantidad: 2 }],
    },
    fecha_proximo: "2026-10-01T12:00:00.000Z",
  });
});

test("rechaza frecuencias, productos y fechas invalidas", () => {
  assert.match(
    validateCreateRecurringOrder({ ...configuration, frecuencia: "anual" }).error.message,
    /frecuencia inválida/
  );
  assert.match(
    validateCreateRecurringOrder({ ...configuration, productos: [] }).error.message,
    /al menos un producto/
  );
  assert.match(
    validateCreateRecurringOrder({ ...configuration, fecha_proximo: "ayer" }).error.message,
    /fecha_proximo inválida/
  );
});

test("PATCH traduce acciones de pausa, reanudacion y cancelacion", () => {
  const current = {
    frecuencia: "semanal",
    productos: validateCreateRecurringOrder(configuration).value.productos,
    fecha_proximo: configuration.fecha_proximo,
    estado: "activo",
  };
  assert.equal(validateUpdateRecurringOrder({ accion: "pausar" }, current).value.estado, "pausado");
  assert.equal(validateUpdateRecurringOrder({ accion: "cancelar" }, current).value.estado, "cancelado");
  assert.equal(validateUpdateRecurringOrder({ accion: "reanudar" }, current).value.estado, "activo");
});

test("POST crea la recurrencia para el usuario autenticado", async () => {
  const queries = [];
  const pool = {
    async query(sql, params) {
      queries.push({ sql, params });
      if (sql.includes("FROM agricultor")) return { rows: [{}] };
      if (sql.includes("FROM distribuidor")) return { rows: [{}] };
      if (sql.includes("FROM inventario_distribuidor")) {
        return { rows: [{ id_inventario: 8 }] };
      }
      return {
        rows: [{
          id: 5,
          id_usuario: 17,
          frecuencia: "semanal",
          productos: validateCreateRecurringOrder(configuration).value.productos,
          fecha_proximo: configuration.fecha_proximo,
          estado: "activo",
        }],
      };
    },
  };
  const { createRecurringOrder } = loadWithPool(controllerPath, pool);
  const res = response();
  await createRecurringOrder(
    { user: { id: 17, tipo: "agricultor" }, body: configuration },
    res
  );
  assert.equal(res.statusCode, 201);
  assert.equal(res.body.pedido_recurrente.id_usuario, 17);
  const insert = queries.find(({ sql }) => sql.includes("INSERT INTO pedido_recurrente"));
  assert.equal(insert.params[0], 17);
});

test("los endpoints no permiten gestionar recurrencias de otro tipo de usuario", async () => {
  const pool = { query: async () => assert.fail("no debe consultar la base") };
  const { listRecurringOrders } = loadWithPool(controllerPath, pool);
  const res = response();
  await listRecurringOrders({ user: { id: 1, tipo: "distribuidor" } }, res);
  assert.equal(res.statusCode, 403);
});

test("calcula la siguiente fecha sin desbordar el fin de mes", () => {
  const { nextOccurrence } = loadWithPool(schedulerPath, {});
  assert.equal(
    nextOccurrence("mensual", new Date("2027-01-31T10:00:00.000Z")).toISOString(),
    "2027-02-28T10:00:00.000Z"
  );
  assert.equal(
    nextOccurrence("quincenal", new Date("2026-09-26T10:00:00.000Z")).toISOString(),
    "2026-10-11T10:00:00.000Z"
  );
});

test("scheduler omite productos sin stock, notifica y crea pedido con los disponibles", async () => {
  const queries = [];
  const client = {
    release() {},
    async query(sql, params) {
      queries.push({ sql, params });
      if (sql.includes("FROM pedido_recurrente pr")) {
        return {
          rows: [{
            id: 5,
            id_usuario: 17,
            id_agricultor: 2,
            agricultor_nombre: "Juan",
            frecuencia: "semanal",
            fecha_proximo: "2026-09-25T10:00:00.000Z",
            productos: {
              id_distribuidor: 3,
              direccion_entrega: "Parcela norte, Guatemala",
              metodo_pago: "contra_entrega",
              items: [
                { id_inventario: 8, cantidad: 2 },
                { id_inventario: 9, cantidad: 5 },
              ],
            },
          }],
        };
      }
      if (sql.includes("FROM inventario_distribuidor")) {
        return {
          rows: [
            { id_inventario: 8, id_distribuidor: 3, precio: 10, stock_disponible: 4, producto_nombre: "Semilla" },
            { id_inventario: 9, id_distribuidor: 3, precio: 12, stock_disponible: 1, producto_nombre: "Abono" },
          ],
        };
      }
      if (sql.includes("INSERT INTO pedido\n")) return { rows: [{ id_pedido: 44 }] };
      return { rows: [] };
    },
  };
  const { processRecurringOrder } = loadWithPool(schedulerPath, {});
  const result = await processRecurringOrder(5, { connect: async () => client });
  assert.equal(result.orderId, 44);
  assert.equal(result.skipped, 1);
  const notification = queries.find(({ sql, params }) =>
    sql.includes("INSERT INTO notificacion") && params?.[1] === "pedido_recurrente_sin_stock"
  );
  assert.ok(notification);
  assert.equal(JSON.parse(notification.params[2]).productos_omitidos[0].id_inventario, 9);
  const detail = queries.find(({ sql }) => sql.includes("INSERT INTO detalle_pedido"));
  assert.deepEqual(detail.params.slice(0, 3), [44, 8, 2]);
  assert.ok(queries.some(({ sql }) => sql.includes("UPDATE pedido_recurrente")));
  assert.ok(queries.some(({ sql }) => sql === "COMMIT"));
});

test("scheduler no crea un pedido vacio cuando ningun producto tiene stock", async () => {
  const queries = [];
  const client = {
    release() {},
    async query(sql, params) {
      queries.push({ sql, params });
      if (sql.includes("FROM pedido_recurrente pr")) {
        return {
          rows: [{
            id: 6,
            id_agricultor: 2,
            agricultor_nombre: "Juan",
            frecuencia: "diaria",
            fecha_proximo: "2026-09-25T10:00:00.000Z",
            productos: {
              id_distribuidor: 3,
              direccion_entrega: "Parcela norte, Guatemala",
              metodo_pago: "contra_entrega",
              items: [{ id_inventario: 9, cantidad: 5 }],
            },
          }],
        };
      }
      if (sql.includes("FROM inventario_distribuidor")) {
        return { rows: [{ id_inventario: 9, id_distribuidor: 3, precio: 12, stock_disponible: 0, producto_nombre: "Abono" }] };
      }
      return { rows: [] };
    },
  };
  const { processRecurringOrder } = loadWithPool(schedulerPath, {});
  const result = await processRecurringOrder(6, { connect: async () => client });
  assert.equal(result.orderId, null);
  assert.equal(queries.some(({ sql }) => sql.includes("INSERT INTO pedido\n")), false);
  assert.ok(queries.some(({ sql }) => sql.includes("UPDATE pedido_recurrente")));
});
