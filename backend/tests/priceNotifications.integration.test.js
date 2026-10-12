// Opt-in: KAN102_PG_PORT=<puerto de PostgreSQL temporal> npm test -- priceNotifications.integration
// Todas las tablas son TEMP y se eliminan automáticamente al cerrar la conexión.
const assert = require("node:assert/strict");
const { Client } = require("pg");
const request = require("supertest");
const jwt = require("jsonwebtoken");
const express = require("express");
let client;
const dbPath = require.resolve("../src/config/db");
const pool = {
  query: (...args) => client.query(...args),
  connect: async () => ({ query: (...args) => client.query(...args), release() {} })
};
require.cache[dbPath] = { id: dbPath, filename: dbPath, loaded: true, exports: { pool } };
const CompraFrecuenteRepository = require("../src/repositories/CompraFrecuenteRepository");
const verifyToken = require("../src/middleware/authMiddleware");
const app = express();
app.use(express.json());
app.use("/api/inventory", require("../src/routes/inventoryRoutes"));
app.use("/api/notifications", verifyToken, require("../src/routes/notificationRoutes"));
const token = (id, tipo = "agricultor") => jwt.sign({ id, tipo }, process.env.JWT_SECRET);

const integration = process.env.KAN102_PG_PORT ? describe : describe.skip;
integration("KAN-102 PostgreSQL real y endpoints autenticados", () => {
  beforeAll(async () => {
    client = new Client({ host: "127.0.0.1", port: Number(process.env.KAN102_PG_PORT), user: "postgres", database: "postgres" });
    await client.connect();
    await client.query(`
      CREATE TEMP TABLE agricultor(id_agricultor int PRIMARY KEY, id_usuario int);
      CREATE TEMP TABLE distribuidor(id_distribuidor int PRIMARY KEY, id_usuario int, estado_verificacion text);
      CREATE TEMP TABLE producto(id_producto int PRIMARY KEY, nombre text, activo boolean);
      CREATE TEMP TABLE inventario_distribuidor(id_inventario serial PRIMARY KEY, id_distribuidor int, id_producto int,
        precio numeric(10,2), stock_disponible int, unidad_medida text, tiempo_entrega_dias int,
        ultima_actualizacion timestamp, UNIQUE(id_distribuidor,id_producto));
      CREATE TEMP TABLE pedido(id_pedido int PRIMARY KEY, id_agricultor int, estado text, fecha_pedido timestamp);
      CREATE TEMP TABLE detalle_pedido(id_pedido int, id_inventario int);
      CREATE TEMP TABLE producto_seguido(id serial, id_agricultor int, id_producto int, precio_al_seguir numeric, fecha timestamp);
      CREATE TEMP TABLE notificacion(id_notificacion serial, id_agricultor int, id_distribuidor int, tipo text,
        contenido jsonb, id_pedido int, leida boolean DEFAULT false, fecha timestamp DEFAULT NOW());
      INSERT INTO agricultor VALUES (1,101),(2,102),(3,103),(4,104),(5,105),(6,106),(7,107),(8,108);
      INSERT INTO distribuidor VALUES (1,201,'verificado');
      INSERT INTO producto VALUES (4,'Maíz',true),(5,'Otro',true);
      INSERT INTO inventario_distribuidor(id_inventario,id_distribuidor,id_producto,precio,stock_disponible)
        VALUES (1,1,4,100,10),(2,1,5,100,10);
      SELECT setval(pg_get_serial_sequence('inventario_distribuidor','id_inventario'),2);
    `);
  });
  afterAll(async () => { if (client) await client.end(); });
  beforeEach(async () => {
    await client.query(`TRUNCATE pedido, detalle_pedido, producto_seguido, notificacion;
      UPDATE producto SET activo=true;
      UPDATE inventario_distribuidor SET precio=100, stock_disponible=10;`);
  });
  async function order(id, farmer, state = 'entregado', days = 10, inventory = 1) {
    await client.query("INSERT INTO pedido VALUES ($1,$2,$3,NOW() - ($4 * INTERVAL '1 day'))", [id,farmer,state,days]);
    await client.query("INSERT INTO detalle_pedido VALUES ($1,$2)", [id,inventory]);
  }
  async function seedRecipients() {
    await order(1,1); await order(2,1);
    await client.query("INSERT INTO producto_seguido(id_agricultor,id_producto) VALUES (1,4),(2,4)");
  }
  const update = body => request(app).put("/api/inventory/1").set("Authorization", `Bearer ${token(201,"distribuidor")}`).send(body);

  test("dos pedidos distintos califican; líneas repetidas, estados, antigüedad y otro producto no", async () => {
    await order(1,1); await order(2,1);
    await order(3,2);
    await client.query("INSERT INTO detalle_pedido VALUES (3,1)");
    await order(4,3,'confirmado'); await order(5,3,'entregado');
    await order(6,4,'cancelado'); await order(7,4);
    await order(8,5,'entregado',91); await order(9,5);
    await order(10,6); await order(11,6,'entregado',10,2);
    await order(12,7,'pendiente'); await order(13,7);
    await order(14,8,'entregado',-1); await order(15,8);
    assert.deepEqual(await new CompraFrecuenteRepository(pool).findByIdProducto(4), [{ idAgricultor: 1 }]);
  });

  test("PUT crea un aviso por destinatario; GET y PATCH aíslan usuarios", async () => {
    await seedRecipients();
    assert.equal((await update({ precio: 75 })).status,200);
    const rows = (await client.query("SELECT * FROM notificacion ORDER BY id_agricultor")).rows;
    assert.equal(rows.length,2);
    assert.equal(rows[0].contenido.porcentaje_descuento,25);
    assert.equal(rows[0].contenido.producto,"Maíz");
    const get = id => request(app).get("/api/notifications").set("Authorization", `Bearer ${token(id)}`);
    assert.equal((await get(101)).body.length,1);
    assert.equal((await get(102)).body.length,1);
    assert.equal((await get(103)).body.length,0);
    assert.equal((await request(app).get("/api/notifications")).status,401);
    const patch = id => request(app).patch(`/api/notifications/${rows[0].id_notificacion}/read`).set("Authorization", `Bearer ${token(id)}`);
    assert.equal((await patch(102)).status,404);
    assert.equal((await patch(101)).status,200);
    assert.equal((await get(101)).body[0].leida,true);
    assert.equal((await request(app).get("/api/notifications").set("Authorization", `Bearer ${token(201,"distribuidor")}`)).body.length,0);
  });

  test.each([{precio:100},{precio:110},{precio:75,stock_disponible:0}])("sin baja o sin stock no notifica: %j", async body => {
    await seedRecipients();
    assert.equal((await update(body)).status,200);
    assert.equal((await client.query("SELECT * FROM notificacion")).rowCount,0);
  });

  test("producto inactivo no notifica y actualización válida persiste", async () => {
    await seedRecipients(); await client.query("UPDATE producto SET activo=false WHERE id_producto=4");
    const log = vi.spyOn(console,"error").mockImplementation(() => {});
    try { assert.equal((await update({precio:75})).status,200); } finally { log.mockRestore(); }
    assert.equal((await client.query("SELECT * FROM notificacion")).rowCount,0);
    assert.equal(Number((await client.query("SELECT precio FROM inventario_distribuidor WHERE id_inventario=1")).rows[0].precio),75);
  });

  test("fallo parcial persiste éxitos una sola vez y no revierte inventario", async () => {
    await seedRecipients();
    await client.query("ALTER TABLE notificacion ADD CONSTRAINT simulated_failure CHECK (id_agricultor <> 2)");
    const log = vi.spyOn(console,"error").mockImplementation(() => {});
    try {
      assert.equal((await update({precio:75})).status,200);
      assert.equal((await update({precio:75})).status,200);
      const rows = (await client.query("SELECT * FROM notificacion")).rows;
      assert.equal(rows.length,1);
      assert.equal(rows[0].id_agricultor,1);
      assert.equal(Number((await client.query("SELECT precio FROM inventario_distribuidor WHERE id_inventario=1")).rows[0].precio),75);
    } finally {
      log.mockRestore();
      await client.query("ALTER TABLE notificacion DROP CONSTRAINT simulated_failure");
    }
  });

  test("POST sobre inventario existente también notifica sin duplicar al repetir precio", async () => {
    await seedRecipients();
    const post = () => request(app).post("/api/inventory").set("Authorization", `Bearer ${token(201,"distribuidor")}`).send({id_producto:4,precio:75,stock_disponible:10});
    assert.equal((await post()).status,201); assert.equal((await post()).status,201);
    assert.equal((await client.query("SELECT * FROM notificacion")).rowCount,2);
  });
});
