const assert = require("node:assert/strict");
const test = require("node:test");

const dbPath = require.resolve("../src/config/db");
const cartControllerPath = require.resolve("../src/controllers/cartController");
const orderControllerPath = require.resolve("../src/controllers/orderController");

const loadController = (controllerPath, pool) => {
  delete require.cache[controllerPath];
  require.cache[dbPath] = {
    id: dbPath,
    filename: dbPath,
    loaded: true,
    exports: { pool },
  };
  return require(controllerPath);
};

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

test("getCart suma subtotales decimales con precios y cantidades diferentes", async () => {
  const items = [
    {
      id_item: 1,
      id_inventario: 8,
      cantidad: 3,
      precio_unitario: 7.07,
      subtotal: 21.21,
    },
    {
      id_item: 2,
      id_inventario: 9,
      cantidad: 2,
      precio_unitario: 13.74,
      subtotal: 27.48,
    },
  ];
  const pool = {
    async query() {
      return { rows: [{ id_carrito: 4, items }] };
    },
  };
  const { getCart } = loadController(cartControllerPath, pool);
  const res = response();

  await getCart({ params: { id_agricultor: "2" } }, res);

  assert.equal(res.statusCode, 200);
  assert.equal(Number(res.body.total.toFixed(2)), 48.69);
  assert.deepEqual(res.body.items, items);
});

test("createOrder ignora montos del cliente y persiste el total del inventario", async () => {
  const queries = [];
  const client = {
    release() {},
    async query(sql, params) {
      queries.push({ sql, params });

      if (sql.includes("SELECT 1 FROM agricultor")) return { rows: [{}] };
      if (sql.includes("SELECT 1 FROM distribuidor")) return { rows: [{}] };
      if (sql.includes("FROM inventario_distribuidor i")) {
        return {
          rows: [
            {
              id_inventario: 8,
              id_distribuidor: 3,
              precio: "7.07",
              stock_disponible: 10,
              producto_nombre: "Fertilizante",
            },
            {
              id_inventario: 9,
              id_distribuidor: 3,
              precio: "13.74",
              stock_disponible: 10,
              producto_nombre: "Fungicida",
            },
          ],
        };
      }
      if (sql.includes("INSERT INTO pedido\n")) {
        return { rows: [{ id_pedido: 11 }] };
      }
      if (sql.includes("SELECT u.nombre")) {
        return { rows: [{ nombre: "Ana" }] };
      }
      if (sql.includes("FROM pedido p\n")) {
        return {
          rows: [{
            id_pedido: 11,
            estado: "confirmado",
            total_pedido: "48.69",
            monto: "48.69",
          }],
        };
      }
      if (sql.includes("FROM detalle_pedido dp")) {
        return {
          rows: [
            {
              id_inventario: 8,
              cantidad: 3,
              precio_unitario: "7.07",
              subtotal: "21.21",
            },
            {
              id_inventario: 9,
              cantidad: 2,
              precio_unitario: "13.74",
              subtotal: "27.48",
            },
          ],
        };
      }
      return { rows: [] };
    },
  };
  const { createOrder } = loadController(orderControllerPath, {
    connect: async () => client,
  });
  const res = response();

  await createOrder(
    {
      agricultorId: 2,
      body: {
        id_distribuidor: 3,
        direccion_entrega: "Parcela norte, Guatemala",
        productos: [
          {
            id_inventario: 8,
            cantidad: 3,
            precio_unitario: 0.01,
            subtotal: 0.03,
          },
          {
            id_inventario: 9,
            cantidad: 2,
            precio_unitario: 0.01,
            subtotal: 0.02,
          },
        ],
        metodo_pago: "efectivo",
        total: 0.05,
        total_pedido: 0.05,
        monto: 0.05,
      },
    },
    res
  );

  assert.equal(res.statusCode, 201);

  const orderInsert = queries.find(({ sql }) =>
    sql.includes("INSERT INTO pedido\n")
  );
  const detailInserts = queries.filter(({ sql }) =>
    sql.includes("INSERT INTO detalle_pedido")
  );
  const paymentInsert = queries.find(({ sql }) =>
    sql.includes("INSERT INTO pago")
  );

  assert.equal(orderInsert.params[6], 48.69);
  assert.deepEqual(
    detailInserts.map(({ params }) => params.slice(1)),
    [
      [8, 3, "7.07"],
      [9, 2, "13.74"],
    ]
  );
  assert.equal(
    Number(
      detailInserts
        .reduce(
          (total, { params }) => total + params[2] * Number(params[3]),
          0
        )
        .toFixed(2)
    ),
    48.69
  );
  assert.deepEqual(paymentInsert.params, [11, "contra_entrega", 48.69]);
  assert.equal(res.body.pedido.total_pedido, "48.69");
  assert.equal(res.body.pedido.monto, "48.69");
  assert.deepEqual(
    res.body.pedido.productos.map(({ subtotal }) => subtotal),
    ["21.21", "27.48"]
  );
});
