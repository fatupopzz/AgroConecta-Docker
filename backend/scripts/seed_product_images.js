/**
 * Carga fotos de prueba para los productos desde una carpeta local.
 *
 * Cada archivo se llama como el id del producto: 3.jpg, 12.png, 40.webp.
 * Pasa por las mismas reglas que POST /api/products/:id/image (formato real,
 * 5 MB, nombre aleatorio, borrar la foto anterior) para que los datos de
 * prueba no queden distintos a lo que generaría la app.
 *
 * Uso dentro del contenedor del backend, que es quien ve la base y el volumen:
 *   node scripts/seed_product_images.js <carpeta> [--dry-run]
 */
require("dotenv").config();

const crypto = require("node:crypto");
const fs = require("node:fs/promises");
const path = require("node:path");
const { pool, shutdownPool } = require("../src/config/db");
const { connectRedis, shutdownRedis } = require("../src/config/redis");
const cacheService = require("../src/services/cacheService");
const {
  PRODUCT_IMAGES_DIR,
  PRODUCT_IMAGE_MAX_BYTES,
  PRODUCT_IMAGE_EXTENSIONS,
} = require("../src/config/uploads");
const {
  detectImageMime,
  replaceProductImage,
} = require("../src/services/productImageService");

const FILE_NAME_PATTERN = /^([1-9]\d*)\.(jpe?g|png|webp)$/i;

const productIsActive = async (idProducto) => {
  const result = await pool.query(
    "SELECT 1 FROM producto WHERE id_producto = $1 AND activo = TRUE",
    [idProducto],
  );
  return result.rows.length > 0;
};

/**
 * Procesa un archivo y devuelve qué pasó con él. Nunca lanza: un archivo malo
 * no debe dejar a medias la carga de los demás.
 */
const seedOne = async ({ sourceDir, fileName, dryRun, seenIds }) => {
  const match = FILE_NAME_PATTERN.exec(fileName);
  if (!match) {
    return { status: "omitido", fileName, reason: "el nombre no es <id_producto>.jpg|png|webp" };
  }

  const idProducto = Number(match[1]);
  // Con 3.jpg y 3.png en la carpeta, la segunda pisaría a la primera y el
  // resultado dependería del orden del sistema de archivos.
  if (seenIds.has(idProducto)) {
    return { status: "omitido", fileName, idProducto, reason: "ya hay otra imagen para ese producto" };
  }
  seenIds.add(idProducto);

  const sourcePath = path.join(sourceDir, fileName);
  let writtenPath = null;

  try {
    const { size } = await fs.stat(sourcePath);
    if (size > PRODUCT_IMAGE_MAX_BYTES) {
      return { status: "omitido", fileName, idProducto, reason: "supera los 5 MB" };
    }

    const bytes = await fs.readFile(sourcePath);
    // La extensión del archivo de origen no se usa: un .jpg que en realidad es
    // PNG se guarda como .png, igual que haría el endpoint.
    const mimeType = detectImageMime(bytes.subarray(0, 12));
    if (!mimeType) {
      return { status: "omitido", fileName, idProducto, reason: "no es una imagen JPEG, PNG o WEBP válida" };
    }

    if (!(await productIsActive(idProducto))) {
      return { status: "omitido", fileName, idProducto, reason: "el producto no existe o está inactivo" };
    }

    if (dryRun) {
      return { status: "actualizado", fileName, idProducto, imagenUrl: "(simulación, no se escribió)" };
    }

    const storedName = `${crypto.randomUUID()}${PRODUCT_IMAGE_EXTENSIONS[mimeType]}`;
    await fs.mkdir(PRODUCT_IMAGES_DIR, { recursive: true });
    writtenPath = path.join(PRODUCT_IMAGES_DIR, storedName);
    await fs.writeFile(writtenPath, bytes, { flag: "wx" });

    const producto = await replaceProductImage(idProducto, storedName);
    if (!producto) {
      // Se desactivó entre la verificación y el UPDATE.
      await fs.rm(writtenPath, { force: true });
      return { status: "omitido", fileName, idProducto, reason: "el producto no existe o está inactivo" };
    }

    return { status: "actualizado", fileName, idProducto, imagenUrl: producto.imagen_url };
  } catch (error) {
    if (writtenPath) {
      await fs.rm(writtenPath, { force: true });
    }
    return { status: "error", fileName, idProducto, reason: error.message };
  }
};

const seedProductImages = async ({ sourceDir, dryRun = false }) => {
  const entries = await fs.readdir(sourceDir, { withFileTypes: true });
  const fileNames = entries
    .filter((entry) => entry.isFile())
    .map((entry) => entry.name)
    .sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));

  const seenIds = new Set();
  const results = [];
  for (const fileName of fileNames) {
    results.push(await seedOne({ sourceDir, fileName, dryRun, seenIds }));
  }

  const updated = results.filter((result) => result.status === "actualizado");
  // GET /api/products está en caché; sin esto el catálogo seguiría mostrando
  // los productos sin foto hasta que venza el TTL.
  const cacheInvalidated = !dryRun && updated.length > 0
    ? await cacheService.invalidate(["products"])
    : false;

  return { results, cacheInvalidated };
};

const printReport = ({ results, cacheInvalidated }, dryRun) => {
  for (const result of results) {
    const id = result.idProducto ? ` (producto ${result.idProducto})` : "";
    const detail = result.status === "actualizado" ? result.imagenUrl : result.reason;
    console.log(`[${result.status}] ${result.fileName}${id}: ${detail}`);
  }

  const count = (status) => results.filter((result) => result.status === status).length;
  console.log(
    `\n${dryRun ? "Simulación: " : ""}${count("actualizado")} actualizados, ` +
      `${count("omitido")} omitidos, ${count("error")} con error.`,
  );
  if (!dryRun && count("actualizado") > 0 && !cacheInvalidated) {
    console.log("No se pudo invalidar Redis: el catálogo mostrará las fotos cuando venza la caché.");
  }
};

const main = async () => {
  const args = process.argv.slice(2);
  const dryRun = args.includes("--dry-run");
  const sourceDir = args.find((arg) => !arg.startsWith("--"));

  if (!sourceDir) {
    console.error("Uso: node scripts/seed_product_images.js <carpeta> [--dry-run]");
    process.exitCode = 2;
    return;
  }

  await connectRedis();
  try {
    const report = await seedProductImages({ sourceDir, dryRun });
    printReport(report, dryRun);
    if (report.results.some((result) => result.status === "error")) {
      process.exitCode = 1;
    }
  } finally {
    await shutdownRedis();
    await shutdownPool();
  }
};

if (require.main === module) {
  main().catch((error) => {
    console.error(`No se pudieron cargar las imágenes: ${error.message}`);
    process.exitCode = 1;
  });
}

module.exports = { seedProductImages };
