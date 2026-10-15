/**
 * Genera las miniaturas de las fotos que ya estaban en el volumen antes de
 * KAN-117 y guarda su ruta en producto.imagen_thumb_url.
 *
 * Recorre los productos con imagen_url y no el contenido de la carpeta: un
 * archivo que ningún producto usa no necesita miniatura. Las fotos que ya
 * tienen la suya (en la base y en disco) se dejan como están, así el script se
 * puede volver a correr sin rehacer todo.
 *
 * Uso dentro del contenedor del backend, que es quien ve la base y el volumen:
 *   node scripts/generate_thumbnails.js [--dry-run]
 */
require("dotenv").config();

const fs = require("node:fs/promises");
const path = require("node:path");
const { pool, shutdownPool } = require("../src/config/db");
const { connectRedis, shutdownRedis } = require("../src/config/redis");
const cacheService = require("../src/services/cacheService");
const {
  PRODUCT_IMAGES_DIR,
  PRODUCT_IMAGES_PUBLIC_PATH,
  PRODUCT_THUMBS_DIR,
} = require("../src/config/uploads");
const {
  thumbnailNameFor,
  generateThumbnail,
} = require("../src/services/productImageService");

const fileExists = (filePath) =>
  fs.access(filePath).then(() => true, () => false);

/**
 * Procesa un producto y devuelve qué pasó con él. Nunca lanza: una foto
 * dañada no debe dejar sin miniatura a las demás.
 */
const processOne = async (row, dryRun) => {
  const { id_producto: idProducto, imagen_url: imagenUrl, imagen_thumb_url: thumbUrl } = row;

  // Una URL externa o editada a mano no está en el volumen y no hay de dónde
  // sacar la miniatura.
  if (!imagenUrl.startsWith(`${PRODUCT_IMAGES_PUBLIC_PATH}/`)) {
    return { status: "omitido", idProducto, imagenUrl, reason: "la foto no está en uploads/productos" };
  }

  const fileName = path.basename(imagenUrl);
  if (!(await fileExists(path.join(PRODUCT_IMAGES_DIR, fileName)))) {
    return { status: "omitido", idProducto, imagenUrl, reason: "el archivo no existe en el volumen" };
  }

  const thumbPath = path.join(PRODUCT_THUMBS_DIR, thumbnailNameFor(fileName));
  if (thumbUrl && (await fileExists(thumbPath))) {
    return { status: "omitido", idProducto, imagenUrl, reason: "ya tiene miniatura" };
  }

  if (dryRun) {
    return { status: "generada", idProducto, imagenUrl, thumbUrl: "(simulación, no se escribió)" };
  }

  try {
    const newThumbUrl = await generateThumbnail(fileName);
    // Se condiciona a la misma imagen_url: si alguien subió otra foto mientras
    // corría el script, el endpoint ya le generó su propia miniatura y esta
    // pertenece a un archivo que ese reemplazo borró.
    const result = await pool.query(
      `UPDATE producto
       SET imagen_thumb_url = $1
       WHERE id_producto = $2 AND imagen_url = $3`,
      [newThumbUrl, idProducto, imagenUrl],
    );
    if (result.rowCount === 0) {
      await fs.rm(thumbPath, { force: true });
      return { status: "omitido", idProducto, imagenUrl, reason: "la foto cambió mientras corría el script" };
    }
    return { status: "generada", idProducto, imagenUrl, thumbUrl: newThumbUrl };
  } catch (error) {
    return { status: "error", idProducto, imagenUrl, reason: error.message };
  }
};

const generateThumbnails = async ({ dryRun = false } = {}) => {
  const { rows } = await pool.query(
    `SELECT id_producto, imagen_url, imagen_thumb_url
     FROM producto
     WHERE imagen_url IS NOT NULL AND imagen_url <> ''
     ORDER BY id_producto`,
  );

  const results = [];
  for (const row of rows) {
    results.push(await processOne(row, dryRun));
  }

  const generated = results.filter((result) => result.status === "generada");
  // GET /api/products está en caché; sin esto el catálogo seguiría mandando
  // imagen_thumb_url vacía hasta que venza el TTL.
  const cacheInvalidated = !dryRun && generated.length > 0
    ? await cacheService.invalidate(["products"])
    : false;

  return { results, cacheInvalidated };
};

const printReport = ({ results, cacheInvalidated }, dryRun) => {
  for (const result of results) {
    const detail = result.status === "generada" ? result.thumbUrl : result.reason;
    console.log(`[${result.status}] producto ${result.idProducto} (${result.imagenUrl}): ${detail}`);
  }

  const count = (status) => results.filter((result) => result.status === status).length;
  console.log(
    `\n${dryRun ? "Simulación: " : ""}${count("generada")} generadas, ` +
      `${count("omitido")} omitidas, ${count("error")} con error.`,
  );
  if (!dryRun && count("generada") > 0 && !cacheInvalidated) {
    console.log("No se pudo invalidar Redis: el catálogo mostrará las miniaturas cuando venza la caché.");
  }
};

const main = async () => {
  const dryRun = process.argv.slice(2).includes("--dry-run");

  await connectRedis();
  try {
    const report = await generateThumbnails({ dryRun });
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
    console.error(`No se pudieron generar las miniaturas: ${error.message}`);
    process.exitCode = 1;
  });
}

module.exports = { generateThumbnails };
