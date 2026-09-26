const request = require("supertest");
const jwt = require("jsonwebtoken");
const zlib = require("node:zlib");

jest.mock("../src/config/db", () => ({ pool: { query: jest.fn() } }));
const { pool } = require("../src/config/db");
const { loadFarmerHistory } = require("../src/services/orderExportRepository");
const app = require("../app");

const collectBinary = (res, callback) => {
  const chunks = [];
  res.on("data", (chunk) => chunks.push(chunk));
  res.on("end", () => callback(null, Buffer.concat(chunks)));
};

const pdfText = (buffer) => {
  const text = [];
  let start = 0;
  while ((start = buffer.indexOf("stream\n", start)) !== -1) {
    start += 7;
    const end = buffer.indexOf("\nendstream", start);
    if (end === -1) break;
    try {
      const content = zlib.inflateSync(buffer.subarray(start, end)).toString("latin1");
      for (const match of content.matchAll(/\[(.*?)\] TJ/gs)) {
        text.push([...match[1].matchAll(/<([0-9a-f]+)>/gi)]
          .map((part) => new TextDecoder("windows-1252")
            .decode(Buffer.from(part[1], "hex"))).join(""));
      }
    } catch (_) { /* Image and font streams are not text. */ }
    start = end + 10;
  }
  return text.join("\n");
};

const auth = (id, tipo) => `Bearer ${jwt.sign({ id, tipo }, process.env.JWT_SECRET)}`;

beforeAll(() => { process.env.JWT_SECRET = "order-export-test-only-secret"; });
beforeEach(() => { pool.query.mockReset(); });

test("sin token devuelve 401 y un distribuidor recibe 403", async () => {
  expect((await request(app).get("/api/orders/export/pdf")).status).toBe(401);
  expect((await request(app).get("/api/orders/export/pdf")
    .set("Authorization", auth(2, "distribuidor"))).status).toBe(403);
  expect(pool.query).not.toHaveBeenCalled();
});

test("exporta solo el historial del agricultor autenticado con cabeceras y firma PDF", async () => {
  pool.query
    .mockResolvedValueOnce({ rows: [{ id_agricultor: 7, nombre: "Ana", apellido: "López", email: "ana@example.com", telefono: "1234" }] })
    .mockResolvedValueOnce({ rows: [
      { id_pedido: 15, fecha_pedido: "01/09/2026", estado: "confirmado", total_pedido: "105.50", id_detalle: 1, cantidad: 2, producto_nombre: "Abono" },
      { id_pedido: 15, fecha_pedido: "01/09/2026", estado: "confirmado", total_pedido: "105.50", id_detalle: 2, cantidad: 1, producto_nombre: "Semilla" },
    ] });
  const result = await request(app).get("/api/orders/export/pdf?farmerId=999")
    .set("Authorization", auth(42, "agricultor"))
    .buffer(true).parse(collectBinary);
  expect(result.status).toBe(200);
  expect(result.headers["content-type"]).toMatch(/^application\/pdf/);
  expect(result.headers["content-disposition"]).toMatch(/^attachment; filename="historial-pedidos-agroconecta.pdf"/);
  expect(result.body.subarray(0, 5).toString()).toBe("%PDF-");
  expect(result.body.length).toBeGreaterThan(1000);
  expect(result.body.toString("latin1")).toContain("/Subtype /Image");
  const content = pdfText(result.body);
  expect(content).toContain("Agricultor: Ana López");
  expect(content).toContain("Fecha: 01/09/2026");
  expect(content).toContain("Abono — Cantidad: 2");
  expect(content).toContain("Semilla — Cantidad: 1");
  expect(content).toContain("Estado: confirmado");
  expect(content).toContain("Total: Q105.50");
  expect(pool.query.mock.calls[0][1]).toEqual([42]);
  expect(pool.query.mock.calls[0][0]).toMatch(/FROM agricultor a/);
  expect(pool.query.mock.calls[1][1]).toEqual([7]);
  expect(pool.query.mock.calls[1][0]).toMatch(/WHERE p\.id_agricultor = \$1/);
  expect(pool.query).toHaveBeenCalledTimes(2);
});

test("no expone detalles internos si la consulta falla", async () => {
  const spy = jest.spyOn(console, "error").mockImplementation(() => {});
  pool.query.mockRejectedValueOnce(new Error("ruta-interna/clave-privada"));
  const result = await request(app).get("/api/orders/export/pdf")
    .set("Authorization", auth(42, "agricultor"));
  expect(result.status).toBe(500);
  expect(JSON.stringify(result.body)).not.toContain("ruta-interna");
  spy.mockRestore();
});

test("historial vacío produce PDF válido", async () => {
  pool.query
    .mockResolvedValueOnce({ rows: [{ id_agricultor: 7, nombre: "Ana" }] })
    .mockResolvedValueOnce({ rows: [] });
  const result = await request(app).get("/api/orders/export/pdf")
    .set("Authorization", auth(42, "agricultor"))
    .buffer(true).parse(collectBinary);
  expect(result.status).toBe(200);
  expect(result.body.subarray(0, 5).toString()).toBe("%PDF-");
  expect(pdfText(result.body)).toContain("No existen pedidos en tu historial.");
});

test("agrupa productos y conserva total y fecha autoritativos", async () => {
  const db = { query: jest.fn()
    .mockResolvedValueOnce({ rows: [{ id_agricultor: 7, nombre: "Ana" }] })
    .mockResolvedValueOnce({ rows: [
      { id_pedido: 15, fecha_pedido: "01/09/2026", estado: "confirmado", total_pedido: "105.50", id_detalle: 1, cantidad: 2, producto_nombre: "Abono" },
      { id_pedido: 15, fecha_pedido: "01/09/2026", estado: "confirmado", total_pedido: "105.50", id_detalle: 2, cantidad: 1, producto_nombre: "Semilla" },
    ] }) };
  const history = await loadFarmerHistory(db, 42);
  expect(history.orders).toEqual([{
    id: 15, date: "01/09/2026", status: "confirmado", total: "105.50",
    products: [{ name: "Abono", quantity: 2 }, { name: "Semilla", quantity: 1 }],
  }]);
});
