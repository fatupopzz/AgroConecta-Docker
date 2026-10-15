const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { installCommonJsMock } = require("./helpers/commonJsMocks");
const { createTestImage } = require("./helpers/testImages");

// Antes de cargar el script: config/uploads lee UPLOADS_DIR al importarse.
const uploadsDir = fs.mkdtempSync(path.join(os.tmpdir(), "agroconecta-thumbs-"));
process.env.UPLOADS_DIR = uploadsDir;
const productImagesDir = path.join(uploadsDir, "productos");
const thumbsDir = path.join(productImagesDir, "thumbs");

const pool = { query: vi.fn() };
const cacheService = { invalidate: vi.fn(), get: vi.fn(), set: vi.fn() };

installCommonJsMock(require.resolve("../src/config/db"), { pool, shutdownPool: vi.fn() });
installCommonJsMock(require.resolve("../src/services/cacheService"), cacheService);

const sharp = require("sharp");
const { generateThumbnails } = require("../scripts/generate_thumbnails");

const storedThumbs = () => (fs.existsSync(thumbsDir) ? fs.readdirSync(thumbsDir).sort() : []);
const byProduct = (results) => Object.fromEntries(results.map((result) => [result.idProducto, result]));
const thumbUpdates = () => pool.query.mock.calls.filter(([sql]) => sql.includes("SET imagen_thumb_url"));

let photo;
const writePhoto = (name, content = photo) =>
  fs.writeFileSync(path.join(productImagesDir, name), content);

/**
 * La base devuelve las filas indicadas y acepta el UPDATE salvo para los ids
 * de cambiadas, que simulan una foto reemplazada mientras corría el script.
 */
const mockDatabase = (rows, { cambiadas = [] } = {}) => {
  pool.query.mockImplementation(async (sql, params) => {
    if (sql.includes("SELECT id_producto, imagen_url, imagen_thumb_url")) return { rows };
    if (sql.includes("SET imagen_thumb_url")) {
      return { rowCount: cambiadas.includes(params[1]) ? 0 : 1, rows: [] };
    }
    throw new Error(`Consulta no esperada en el test: ${sql}`);
  });
};

describe("generate_thumbnails", () => {
  beforeAll(async () => {
    photo = await createTestImage({ width: 1000, height: 500, format: "jpeg" });
  });

  beforeEach(() => {
    vi.clearAllMocks();
    cacheService.invalidate.mockResolvedValue(true);
    fs.rmSync(productImagesDir, { recursive: true, force: true });
    fs.mkdirSync(productImagesDir, { recursive: true });
  });

  afterAll(() => {
    fs.rmSync(uploadsDir, { recursive: true, force: true });
  });

  test("genera la miniatura de las fotos que no la tienen y guarda su ruta", async () => {
    writePhoto("aaa.jpg");
    writePhoto("bbb.png", await createTestImage({ format: "png" }));
    mockDatabase([
      { id_producto: 1, imagen_url: "/uploads/productos/aaa.jpg", imagen_thumb_url: null },
      { id_producto: 2, imagen_url: "/uploads/productos/bbb.png", imagen_thumb_url: null },
    ]);

    const { results, cacheInvalidated } = await generateThumbnails();

    const report = byProduct(results);
    expect(report[1]).toMatchObject({ status: "generada", thumbUrl: "/uploads/productos/thumbs/aaa.webp" });
    expect(report[2]).toMatchObject({ status: "generada", thumbUrl: "/uploads/productos/thumbs/bbb.webp" });
    expect(storedThumbs()).toEqual(["aaa.webp", "bbb.webp"]);
    expect(await sharp(path.join(thumbsDir, "aaa.webp")).metadata())
      .toMatchObject({ format: "webp", width: 400, height: 200 });
    // El UPDATE va atado a la foto que se procesó, no solo al producto.
    expect(thumbUpdates().map(([, params]) => params)).toEqual([
      ["/uploads/productos/thumbs/aaa.webp", 1, "/uploads/productos/aaa.jpg"],
      ["/uploads/productos/thumbs/bbb.webp", 2, "/uploads/productos/bbb.png"],
    ]);
    expect(cacheService.invalidate).toHaveBeenCalledWith(["products"]);
    expect(cacheInvalidated).toBe(true);
  });

  test("omite fotos externas, archivos que faltan y las que ya tienen miniatura", async () => {
    writePhoto("lista.jpg");
    fs.mkdirSync(thumbsDir, { recursive: true });
    fs.writeFileSync(path.join(thumbsDir, "lista.webp"), "miniatura existente");
    mockDatabase([
      { id_producto: 1, imagen_url: "https://cdn.example.com/a.jpg", imagen_thumb_url: null },
      { id_producto: 2, imagen_url: "/uploads/productos/no-esta.jpg", imagen_thumb_url: null },
      {
        id_producto: 3,
        imagen_url: "/uploads/productos/lista.jpg",
        imagen_thumb_url: "/uploads/productos/thumbs/lista.webp",
      },
    ]);

    const { results, cacheInvalidated } = await generateThumbnails();

    const report = byProduct(results);
    expect(report[1]).toMatchObject({ status: "omitido", reason: "la foto no está en uploads/productos" });
    expect(report[2]).toMatchObject({ status: "omitido", reason: "el archivo no existe en el volumen" });
    expect(report[3]).toMatchObject({ status: "omitido", reason: "ya tiene miniatura" });
    expect(fs.readFileSync(path.join(thumbsDir, "lista.webp"), "utf8")).toBe("miniatura existente");
    expect(thumbUpdates()).toEqual([]);
    expect(cacheInvalidated).toBe(false);
  });

  test("vuelve a generar la miniatura si la base la tiene pero el archivo se perdió", async () => {
    writePhoto("perdida.jpg");
    mockDatabase([
      {
        id_producto: 4,
        imagen_url: "/uploads/productos/perdida.jpg",
        imagen_thumb_url: "/uploads/productos/thumbs/perdida.webp",
      },
    ]);

    const { results } = await generateThumbnails();

    expect(results[0].status).toBe("generada");
    expect(storedThumbs()).toEqual(["perdida.webp"]);
  });

  test("dry-run informa sin escribir miniaturas, base ni caché", async () => {
    writePhoto("aaa.jpg");
    mockDatabase([{ id_producto: 1, imagen_url: "/uploads/productos/aaa.jpg", imagen_thumb_url: null }]);

    const { results } = await generateThumbnails({ dryRun: true });

    expect(results[0].status).toBe("generada");
    expect(storedThumbs()).toEqual([]);
    expect(thumbUpdates()).toEqual([]);
    expect(cacheService.invalidate).not.toHaveBeenCalled();
  });

  test("una foto dañada se reporta como error sin frenar a las demás", async () => {
    writePhoto("rota.jpg", Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe0]), Buffer.alloc(64, 2)]));
    writePhoto("buena.jpg");
    mockDatabase([
      { id_producto: 1, imagen_url: "/uploads/productos/rota.jpg", imagen_thumb_url: null },
      { id_producto: 2, imagen_url: "/uploads/productos/buena.jpg", imagen_thumb_url: null },
    ]);

    const { results } = await generateThumbnails();

    const report = byProduct(results);
    expect(report[1].status).toBe("error");
    expect(report[2].status).toBe("generada");
    expect(storedThumbs()).toEqual(["buena.webp"]);
  });

  test("si la foto cambió mientras corría no guarda ni deja la miniatura", async () => {
    writePhoto("reemplazada.jpg");
    mockDatabase(
      [{ id_producto: 6, imagen_url: "/uploads/productos/reemplazada.jpg", imagen_thumb_url: null }],
      { cambiadas: [6] },
    );

    const { results, cacheInvalidated } = await generateThumbnails();

    expect(results[0]).toMatchObject({ status: "omitido", reason: "la foto cambió mientras corría el script" });
    expect(storedThumbs()).toEqual([]);
    expect(cacheInvalidated).toBe(false);
  });
});
