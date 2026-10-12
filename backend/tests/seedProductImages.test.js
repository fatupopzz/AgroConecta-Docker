const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { installCommonJsMock } = require("./helpers/commonJsMocks");

// Antes de cargar el script: config/uploads lee UPLOADS_DIR al importarse.
const uploadsDir = fs.mkdtempSync(path.join(os.tmpdir(), "agroconecta-seed-uploads-"));
process.env.UPLOADS_DIR = uploadsDir;
const productImagesDir = path.join(uploadsDir, "productos");

const pool = { query: vi.fn() };
const cacheService = { invalidate: vi.fn(), get: vi.fn(), set: vi.fn() };

installCommonJsMock(require.resolve("../src/config/db"), { pool, shutdownPool: vi.fn() });
installCommonJsMock(require.resolve("../src/services/cacheService"), cacheService);

const { seedProductImages } = require("../scripts/seed_product_images");

const PNG = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  Buffer.alloc(32, 1),
]);
const JPEG = Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe0]), Buffer.alloc(32, 2)]);

let sourceDir;

const writeSource = (name, content) => fs.writeFileSync(path.join(sourceDir, name), content);
const storedImages = () =>
  fs.existsSync(productImagesDir) ? fs.readdirSync(productImagesDir) : [];
const byFile = (results) => Object.fromEntries(results.map((result) => [result.fileName, result]));

/**
 * Productos activos: 1, 3 y 5. El 3 ya tenía foto, para comprobar que se borra.
 */
const mockDatabase = () => {
  const activos = new Set([1, 3, 5]);
  const anteriores = { 3: "/uploads/productos/vieja.jpg" };
  pool.query.mockImplementation(async (sql, params) => {
    if (sql.includes("SELECT 1 FROM producto")) {
      return { rows: activos.has(params[0]) ? [{ "?column?": 1 }] : [] };
    }
    if (sql.includes("SET imagen_url")) {
      const [imagenUrl, id] = params;
      if (!activos.has(id)) return { rowCount: 0, rows: [] };
      return {
        rowCount: 1,
        rows: [{ id_producto: id, imagen_url: imagenUrl, imagen_anterior: anteriores[id] ?? null }],
      };
    }
    throw new Error(`Consulta no esperada en el test: ${sql}`);
  });
};

describe("seed_product_images", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    cacheService.invalidate.mockResolvedValue(true);
    fs.rmSync(productImagesDir, { recursive: true, force: true });
    sourceDir = fs.mkdtempSync(path.join(os.tmpdir(), "agroconecta-seed-src-"));
    mockDatabase();
  });

  afterEach(() => {
    fs.rmSync(sourceDir, { recursive: true, force: true });
  });

  afterAll(() => {
    fs.rmSync(uploadsDir, { recursive: true, force: true });
  });

  test("copia las imágenes válidas con nombre aleatorio y actualiza imagen_url", async () => {
    writeSource("1.png", PNG);
    writeSource("5.jpeg", JPEG);

    const { results, cacheInvalidated } = await seedProductImages({ sourceDir });

    const report = byFile(results);
    expect(report["1.png"]).toMatchObject({ status: "actualizado", idProducto: 1 });
    expect(report["1.png"].imagenUrl).toMatch(/^\/uploads\/productos\/[0-9a-f-]{36}\.png$/);
    expect(report["5.jpeg"].imagenUrl).toMatch(/^\/uploads\/productos\/[0-9a-f-]{36}\.jpg$/);

    const stored = storedImages().sort();
    expect(stored).toHaveLength(2);
    expect(fs.readFileSync(path.join(productImagesDir, path.basename(report["1.png"].imagenUrl))))
      .toEqual(PNG);
    expect(cacheService.invalidate).toHaveBeenCalledWith(["products"]);
    expect(cacheInvalidated).toBe(true);
  });

  test("borra la foto anterior del producto", async () => {
    fs.mkdirSync(productImagesDir, { recursive: true });
    fs.writeFileSync(path.join(productImagesDir, "vieja.jpg"), JPEG);
    writeSource("3.png", PNG);

    await seedProductImages({ sourceDir });

    expect(storedImages()).not.toContain("vieja.jpg");
    expect(storedImages()).toHaveLength(1);
  });

  test("guarda con la extensión del contenido real y no la del nombre", async () => {
    writeSource("1.jpg", PNG);

    const { results } = await seedProductImages({ sourceDir });

    expect(results[0].imagenUrl).toMatch(/\.png$/);
  });

  test("omite lo que no se puede cargar sin frenar a los demás", async () => {
    writeSource("1.png", PNG);
    writeSource("notas.txt", "hola");
    writeSource("abc.png", PNG);
    writeSource("3.png", Buffer.from("<html>no soy imagen</html>"));
    writeSource("9.png", PNG);
    writeSource("5.png", Buffer.concat([PNG, Buffer.alloc(5 * 1024 * 1024)]));

    const { results } = await seedProductImages({ sourceDir });

    const report = byFile(results);
    expect(report["1.png"].status).toBe("actualizado");
    expect(report["notas.txt"]).toMatchObject({ status: "omitido", reason: "el nombre no es <id_producto>.jpg|png|webp" });
    expect(report["abc.png"].status).toBe("omitido");
    expect(report["3.png"]).toMatchObject({ status: "omitido", reason: "no es una imagen JPEG, PNG o WEBP válida" });
    expect(report["9.png"]).toMatchObject({ status: "omitido", reason: "el producto no existe o está inactivo" });
    expect(report["5.png"]).toMatchObject({ status: "omitido", reason: "supera los 5 MB" });
    expect(storedImages()).toHaveLength(1);
  });

  test("con dos imágenes del mismo producto solo usa la primera", async () => {
    writeSource("1.jpg", JPEG);
    writeSource("1.png", PNG);

    const { results } = await seedProductImages({ sourceDir });

    const report = byFile(results);
    expect(report["1.jpg"].status).toBe("actualizado");
    expect(report["1.png"]).toMatchObject({ status: "omitido", reason: "ya hay otra imagen para ese producto" });
    expect(storedImages()).toHaveLength(1);
  });

  test("dry-run informa sin escribir archivos, base ni caché", async () => {
    writeSource("1.png", PNG);
    writeSource("9.png", PNG);

    const { results } = await seedProductImages({ sourceDir, dryRun: true });

    expect(byFile(results)["1.png"].status).toBe("actualizado");
    expect(byFile(results)["9.png"].status).toBe("omitido");
    expect(storedImages()).toEqual([]);
    expect(pool.query.mock.calls.some(([sql]) => sql.includes("SET imagen_url"))).toBe(false);
    expect(cacheService.invalidate).not.toHaveBeenCalled();
  });

  test("si falla la base no deja el archivo copiado", async () => {
    writeSource("1.png", PNG);
    pool.query.mockImplementation(async (sql) => {
      if (sql.includes("SELECT 1 FROM producto")) return { rows: [{}] };
      throw new Error("conexión perdida");
    });

    const { results } = await seedProductImages({ sourceDir });

    expect(results[0]).toMatchObject({ status: "error", reason: "conexión perdida" });
    expect(storedImages()).toEqual([]);
    expect(cacheService.invalidate).not.toHaveBeenCalled();
  });
});
