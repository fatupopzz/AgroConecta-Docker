const fs = require("node:fs/promises");
const path = require("node:path");
const sharp = require("sharp");
const { pool } = require("../config/db");
const AppError = require("../errors/AppError");
const {
  PRODUCT_IMAGES_DIR,
  PRODUCT_IMAGES_PUBLIC_PATH,
  PRODUCT_THUMBS_DIR,
  PRODUCT_THUMBS_PUBLIC_PATH,
  PRODUCT_THUMB_WIDTH,
} = require("../config/uploads");

// Lo comparten el endpoint de subida y el script de datos de prueba, para que
// una foto cargada por cualquiera de los dos caminos pase las mismas reglas.

// El MIME que manda el cliente es solo una etiqueta; cualquiera puede subir un
// HTML diciendo que es image/png. Los primeros bytes sí delatan el formato real.
const detectImageMime = (header) => {
  if (header.length >= 3 && header[0] === 0xff && header[1] === 0xd8 && header[2] === 0xff) {
    return "image/jpeg";
  }
  if (
    header.length >= 8 &&
    header.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))
  ) {
    return "image/png";
  }
  if (
    header.length >= 12 &&
    header.toString("ascii", 0, 4) === "RIFF" &&
    header.toString("ascii", 8, 12) === "WEBP"
  ) {
    return "image/webp";
  }
  return null;
};

const readFileHeader = async (filePath) => {
  const handle = await fs.open(filePath, "r");
  const buffer = Buffer.alloc(12);
  const { bytesRead } = await handle.read(buffer, 0, buffer.length, 0).finally(() => handle.close());
  return buffer.subarray(0, bytesRead);
};

// La miniatura se llama igual que la foto pero siempre en WEBP. Derivarla del
// nombre permite encontrarla al borrar sin depender de imagen_thumb_url, que
// queda vacía en las fotos subidas antes de que existieran las miniaturas.
const thumbnailNameFor = (fileName) => `${path.parse(path.basename(fileName)).name}.webp`;

const removeFileQuietly = (filePath, label) =>
  fs.rm(filePath, { force: true }).catch((error) => {
    console.warn(`No se pudo borrar ${label} ${filePath}: ${error.message}`);
  });

/**
 * Borra una foto de uploads/productos junto con su miniatura. Se usa basename
 * porque el nombre puede venir de la base de datos y no se quiere que un
 * registro alterado a mano termine borrando algo fuera de esa carpeta.
 */
const removeProductImageFiles = async (fileName) => {
  const name = path.basename(fileName);
  await Promise.all([
    removeFileQuietly(path.join(PRODUCT_IMAGES_DIR, name), "la imagen"),
    removeFileQuietly(path.join(PRODUCT_THUMBS_DIR, thumbnailNameFor(name)), "la miniatura"),
  ]);
};

/**
 * Borra la foto anterior y su miniatura. Si falla solo se registra: la imagen
 * nueva ya quedó guardada y no tiene sentido fallar por un archivo viejo que
 * sobró.
 */
const removePreviousImage = async (previousUrl, currentUrl) => {
  if (!previousUrl || previousUrl === currentUrl) return;
  if (!previousUrl.startsWith(`${PRODUCT_IMAGES_PUBLIC_PATH}/`)) return;

  await removeProductImageFiles(previousUrl);
};

/**
 * Genera la miniatura WEBP de una foto que ya está en uploads/productos y
 * devuelve su ruta pública. withoutEnlargement deja tal cual las fotos que ya
 * miden menos de 400 px: agrandarlas solo las haría pesar más y verse peor.
 *
 * La firma de los primeros bytes no garantiza que el resto del archivo sea una
 * imagen; si sharp no la puede leer, la app tampoco podría mostrarla, así que
 * se rechaza como foto inválida.
 */
const generateThumbnail = async (fileName) => {
  const name = path.basename(fileName);
  const thumbName = thumbnailNameFor(name);
  const thumbPath = path.join(PRODUCT_THUMBS_DIR, thumbName);

  await fs.mkdir(PRODUCT_THUMBS_DIR, { recursive: true });
  try {
    await sharp(path.join(PRODUCT_IMAGES_DIR, name))
      // Las fotos de celular traen la orientación en EXIF. Coil la respeta en
      // la foto grande; sin rotate() la miniatura saldría acostada.
      .rotate()
      .resize({ width: PRODUCT_THUMB_WIDTH, withoutEnlargement: true })
      .webp({ quality: 80 })
      .toFile(thumbPath);
  } catch (error) {
    await removeFileQuietly(thumbPath, "la miniatura incompleta");
    console.warn(`No se pudo generar la miniatura de ${name}: ${error.message}`);
    throw new AppError(400, "No se pudo procesar la imagen. Prueba con otra foto JPEG, PNG o WEBP");
  }

  return `${PRODUCT_THUMBS_PUBLIC_PATH}/${thumbName}`;
};

/**
 * Apunta el producto a un archivo que ya está en uploads/productos, le genera
 * la miniatura y borra la foto que tenía antes junto con la suya. Devuelve null
 * si el producto no existe o está inactivo.
 *
 * La subconsulta con FOR UPDATE lee la URL anterior en la misma sentencia que
 * la reemplaza. Si se leyera antes por separado, dos subidas simultáneas
 * podrían quedarse con la misma "anterior" y dejar una foto huérfana.
 */
const replaceProductImage = async (idProducto, fileName) => {
  const imagenUrl = `${PRODUCT_IMAGES_PUBLIC_PATH}/${fileName}`;
  const imagenThumbUrl = await generateThumbnail(fileName);
  const discardThumbnail = () =>
    removeFileQuietly(path.join(PRODUCT_THUMBS_DIR, thumbnailNameFor(fileName)), "la miniatura");

  let result;
  try {
    result = await pool.query(
      `UPDATE producto p
       SET imagen_url = $1,
           imagen_thumb_url = $2
       FROM (
         SELECT id_producto, imagen_url
         FROM producto
         WHERE id_producto = $3 AND activo = TRUE
         FOR UPDATE
       ) anterior
       WHERE p.id_producto = anterior.id_producto
       RETURNING p.id_producto, p.imagen_url, p.imagen_thumb_url,
                 anterior.imagen_url AS imagen_anterior`,
      [imagenUrl, imagenThumbUrl, Number(idProducto)],
    );
  } catch (error) {
    await discardThumbnail();
    throw error;
  }

  if (result.rowCount === 0) {
    await discardThumbnail();
    return null;
  }

  const { id_producto, imagen_url, imagen_thumb_url, imagen_anterior } = result.rows[0];
  await removePreviousImage(imagen_anterior, imagen_url);
  return { id_producto, imagen_url, imagen_thumb_url, imagen_anterior };
};

module.exports = {
  detectImageMime,
  readFileHeader,
  thumbnailNameFor,
  generateThumbnail,
  removeProductImageFiles,
  removePreviousImage,
  replaceProductImage,
};
