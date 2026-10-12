const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { installCommonJsMock } = require("./helpers/commonJsMocks");

// Debe quedar definido antes de cargar app.js: config/uploads lee la variable
// al importarse y así las fotos de prueba no caen dentro del repositorio.
const uploadsDir = fs.mkdtempSync(path.join(os.tmpdir(), "agroconecta-uploads-"));
process.env.UPLOADS_DIR = uploadsDir;
const productImagesDir = path.join(uploadsDir, "productos");

const pool = { query: vi.fn() };
const jwt = { verify: vi.fn() };

installCommonJsMock(require.resolve("../src/config/db"), { pool });
installCommonJsMock(require.resolve("jsonwebtoken"), jwt);

const request = require("supertest");
const app = require("../app");

const PNG_BYTES = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  Buffer.alloc(64, 1),
]);
const JPEG_BYTES = Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe0]), Buffer.alloc(64, 2)]);
const UUID_FILE = /^\/uploads\/productos\/[0-9a-f-]{36}\.(png|jpg|webp)$/;

const distributor = { id: 7, tipo: "distribuidor" };
const admin = { id: 1, tipo: "administrador" };

const storedImages = () =>
  fs.existsSync(productImagesDir) ? fs.readdirSync(productImagesDir) : [];

/**
 * Simula la base según la consulta en lugar de encadenar respuestas por orden,
 * así el test no se rompe si el controlador cambia cuántas consultas hace.
 */
const mockDatabase = ({
  esDuenio = true,
  verificado = true,
  existe = true,
  imagenAnterior = null,
} = {}) => {
  pool.query.mockImplementation(async (sql, params) => {
    if (sql.includes("AS es_duenio")) {
      return {
        rows: existe ? [{ id_producto: params[0], es_duenio: esDuenio, verificado }] : [],
      };
    }
    if (sql.includes("SET imagen_url")) {
      return {
        rowCount: 1,
        rows: [{ id_producto: params[1], imagen_url: params[0], imagen_anterior: imagenAnterior }],
      };
    }
    throw new Error(`Consulta no esperada en el test: ${sql}`);
  });
};

const uploadAs = (user, productId = 5) => {
  jwt.verify.mockReturnValue(user);
  return request(app)
    .post(`/api/products/${productId}/image`)
    .set("Authorization", "Bearer token-valido");
};

describe("POST /api/products/:id/image", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fs.rmSync(productImagesDir, { recursive: true, force: true });
  });

  afterAll(() => {
    fs.rmSync(uploadsDir, { recursive: true, force: true });
  });

  test("requiere token", async () => {
    const response = await request(app)
      .post("/api/products/5/image")
      .attach("imagen", PNG_BYTES, { filename: "foto.png", contentType: "image/png" });

    expect(response.statusCode).toBe(401);
    expect(pool.query).not.toHaveBeenCalled();
    expect(storedImages()).toEqual([]);
  });

  test("guarda la imagen del dueño con nombre aleatorio y ruta relativa", async () => {
    mockDatabase();

    const response = await uploadAs(distributor)
      .attach("imagen", PNG_BYTES, { filename: "../../etc/passwd.png", contentType: "image/png" });

    expect(response.statusCode).toBe(200);
    expect(response.body.producto.id_producto).toBe(5);
    expect(response.body.producto.imagen_url).toMatch(UUID_FILE);

    const fileName = path.basename(response.body.producto.imagen_url);
    expect(storedImages()).toEqual([fileName]);
    expect(fs.readFileSync(path.join(productImagesDir, fileName))).toEqual(PNG_BYTES);

    const [, updateParams] = pool.query.mock.calls.find(([sql]) => sql.includes("SET imagen_url"));
    expect(updateParams).toEqual([response.body.producto.imagen_url, 5]);
  });

  test("borra la imagen anterior al reemplazarla", async () => {
    fs.mkdirSync(productImagesDir, { recursive: true });
    fs.writeFileSync(path.join(productImagesDir, "vieja.jpg"), JPEG_BYTES);
    mockDatabase({ imagenAnterior: "/uploads/productos/vieja.jpg" });

    const response = await uploadAs(distributor)
      .attach("imagen", JPEG_BYTES, { filename: "nueva.jpg", contentType: "image/jpeg" });

    expect(response.statusCode).toBe(200);
    expect(storedImages()).toEqual([path.basename(response.body.producto.imagen_url)]);
  });

  test("no borra archivos fuera de uploads/productos aunque la base lo indique", async () => {
    const outsider = path.join(uploadsDir, "no-tocar.txt");
    fs.writeFileSync(outsider, "dato");
    mockDatabase({ imagenAnterior: "/uploads/../no-tocar.txt" });

    const response = await uploadAs(distributor)
      .attach("imagen", PNG_BYTES, { filename: "foto.png", contentType: "image/png" });

    expect(response.statusCode).toBe(200);
    expect(fs.existsSync(outsider)).toBe(true);
  });

  test("un administrador puede subir aunque no tenga inventario", async () => {
    mockDatabase({ esDuenio: false });

    const response = await uploadAs(admin)
      .attach("imagen", PNG_BYTES, { filename: "foto.png", contentType: "image/png" });

    expect(response.statusCode).toBe(200);
    expect(response.body.producto.imagen_url).toMatch(UUID_FILE);
  });

  test("rechaza tipos que no son jpeg, png o webp", async () => {
    mockDatabase();

    const response = await uploadAs(distributor)
      .attach("imagen", Buffer.from("GIF89a"), { filename: "foto.gif", contentType: "image/gif" });

    expect(response.statusCode).toBe(400);
    expect(response.body).toEqual({ error: "Formato no permitido. Usa JPEG, PNG o WEBP" });
    expect(storedImages()).toEqual([]);
  });

  test("rechaza un archivo que dice ser png pero no lo es y no deja rastro en disco", async () => {
    mockDatabase();

    const response = await uploadAs(distributor)
      .attach("imagen", Buffer.from("<html><script>alert(1)</script></html>"), {
        filename: "foto.png",
        contentType: "image/png",
      });

    expect(response.statusCode).toBe(400);
    expect(response.body).toEqual({ error: "El archivo no es una imagen JPEG, PNG o WEBP válida" });
    expect(storedImages()).toEqual([]);
    expect(pool.query.mock.calls.some(([sql]) => sql.includes("SET imagen_url"))).toBe(false);
  });

  test("rechaza archivos de más de 5 MB", async () => {
    mockDatabase();
    const tooBig = Buffer.concat([PNG_BYTES, Buffer.alloc(5 * 1024 * 1024)]);

    const response = await uploadAs(distributor)
      .attach("imagen", tooBig, { filename: "grande.png", contentType: "image/png" });

    expect(response.statusCode).toBe(413);
    expect(response.body).toEqual({ error: "La imagen supera el máximo de 5 MB" });
    expect(storedImages()).toEqual([]);
  });

  test("responde 403 a un distribuidor que no es dueño sin escribir en disco", async () => {
    mockDatabase({ esDuenio: false });

    const response = await uploadAs({ id: 99, tipo: "distribuidor" })
      .attach("imagen", PNG_BYTES, { filename: "foto.png", contentType: "image/png" });

    expect(response.statusCode).toBe(403);
    expect(response.body).toEqual({
      error: "No puedes cambiar la imagen de un producto que no publicaste",
    });
    expect(storedImages()).toEqual([]);
  });

  test("a un dueño con la cuenta sin verificar le explica el motivo real", async () => {
    mockDatabase({ verificado: false });

    const response = await uploadAs(distributor)
      .attach("imagen", PNG_BYTES, { filename: "foto.png", contentType: "image/png" });

    expect(response.statusCode).toBe(403);
    expect(response.body).toEqual({ error: "Tu cuenta aún no está verificada" });
    expect(storedImages()).toEqual([]);
  });

  test("un admin no depende de la verificación de distribuidor", async () => {
    mockDatabase({ esDuenio: false, verificado: false });

    const response = await uploadAs(admin)
      .attach("imagen", PNG_BYTES, { filename: "foto.png", contentType: "image/png" });

    expect(response.statusCode).toBe(200);
  });

  test("responde 403 a un agricultor sin consultar la base", async () => {
    const response = await uploadAs({ id: 3, tipo: "agricultor" })
      .attach("imagen", PNG_BYTES, { filename: "foto.png", contentType: "image/png" });

    expect(response.statusCode).toBe(403);
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("responde 404 si el producto no existe o está inactivo", async () => {
    mockDatabase({ existe: false });

    const response = await uploadAs(distributor)
      .attach("imagen", PNG_BYTES, { filename: "foto.png", contentType: "image/png" });

    expect(response.statusCode).toBe(404);
    expect(storedImages()).toEqual([]);
  });

  test("responde 400 si no llega el campo imagen", async () => {
    mockDatabase();

    const response = await uploadAs(distributor).field("otro", "valor");

    expect(response.statusCode).toBe(400);
    expect(response.body).toEqual({ error: 'Falta la imagen en el campo "imagen"' });
  });

  test("valida el id antes de tocar la base", async () => {
    const response = await uploadAs(distributor, "abc")
      .attach("imagen", PNG_BYTES, { filename: "foto.png", contentType: "image/png" });

    expect(response.statusCode).toBe(400);
    expect(pool.query).not.toHaveBeenCalled();
  });
});

describe("lectura de imagen_url", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("GET /api/products incluye imagen_url en cada producto", async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ total: "1" }] })
      .mockResolvedValueOnce({
        rows: [{ id_producto: 5, nombre: "Abono", imagen_url: "/uploads/productos/a.png" }],
      });

    const response = await request(app).get("/api/products");

    expect(response.statusCode).toBe(200);
    expect(response.body.products[0].imagen_url).toBe("/uploads/productos/a.png");
    expect(pool.query.mock.calls[1][0]).toMatch(/p\.imagen_url/);
  });

  test("GET /api/products/:id incluye imagen_url", async () => {
    pool.query
      .mockResolvedValueOnce({
        rows: [{ id_producto: 5, nombre: "Abono", categoria: "Fertilizantes", imagen_url: null }],
      })
      .mockResolvedValueOnce({ rows: [] });

    const response = await request(app).get("/api/products/5");

    expect(response.statusCode).toBe(200);
    expect(response.body).toHaveProperty("imagen_url", null);
  });

  test("/uploads se sirve sin token y con cache de larga duración", async () => {
    fs.mkdirSync(productImagesDir, { recursive: true });
    fs.writeFileSync(path.join(productImagesDir, "publica.png"), PNG_BYTES);

    const response = await request(app).get("/uploads/productos/publica.png");

    expect(response.statusCode).toBe(200);
    expect(response.headers["content-type"]).toBe("image/png");
    expect(response.headers["cache-control"]).toBe("public, max-age=604800, immutable");
    expect(response.headers["x-content-type-options"]).toBe("nosniff");
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("/uploads responde 404 en JSON cuando el archivo no existe", async () => {
    const response = await request(app).get("/uploads/productos/no-existe.png");

    expect(response.statusCode).toBe(404);
    expect(response.body).toEqual({ error: "Ruta no encontrada" });
  });
});
