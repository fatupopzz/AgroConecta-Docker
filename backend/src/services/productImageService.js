const fs = require("node:fs/promises");
const path = require("node:path");
const { pool } = require("../config/db");
const {
  PRODUCT_IMAGES_DIR,
  PRODUCT_IMAGES_PUBLIC_PATH,
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

/**
 * Borra la foto anterior del volumen. Se usa basename porque el valor viene de
 * la base de datos y no se quiere que un registro alterado a mano termine
 * borrando algo fuera de uploads/productos. Si falla solo se registra: la
 * imagen nueva ya quedó guardada y no tiene sentido fallar por un archivo
 * viejo que sobró.
 */
const removePreviousImage = async (previousUrl, currentUrl) => {
  if (!previousUrl || previousUrl === currentUrl) return;
  if (!previousUrl.startsWith(`${PRODUCT_IMAGES_PUBLIC_PATH}/`)) return;

  const previousPath = path.join(PRODUCT_IMAGES_DIR, path.basename(previousUrl));
  await fs.rm(previousPath, { force: true }).catch((error) => {
    console.warn(`No se pudo borrar la imagen anterior ${previousPath}: ${error.message}`);
  });
};

/**
 * Apunta el producto a un archivo que ya está en uploads/productos y borra el
 * que tenía antes. Devuelve null si el producto no existe o está inactivo.
 *
 * La subconsulta con FOR UPDATE lee la URL anterior en la misma sentencia que
 * la reemplaza. Si se leyera antes por separado, dos subidas simultáneas
 * podrían quedarse con la misma "anterior" y dejar una foto huérfana.
 */
const replaceProductImage = async (idProducto, fileName) => {
  const imagenUrl = `${PRODUCT_IMAGES_PUBLIC_PATH}/${fileName}`;

  const result = await pool.query(
    `UPDATE producto p
     SET imagen_url = $1
     FROM (
       SELECT id_producto, imagen_url
       FROM producto
       WHERE id_producto = $2 AND activo = TRUE
       FOR UPDATE
     ) anterior
     WHERE p.id_producto = anterior.id_producto
     RETURNING p.id_producto, p.imagen_url, anterior.imagen_url AS imagen_anterior`,
    [imagenUrl, Number(idProducto)],
  );

  if (result.rowCount === 0) {
    return null;
  }

  const { id_producto, imagen_url, imagen_anterior } = result.rows[0];
  await removePreviousImage(imagen_anterior, imagen_url);
  return { id_producto, imagen_url, imagen_anterior };
};

module.exports = {
  detectImageMime,
  readFileHeader,
  removePreviousImage,
  replaceProductImage,
};
