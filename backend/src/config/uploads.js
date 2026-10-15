const path = require("node:path");

// En el contenedor esto resuelve a /app/uploads, que es donde docker-compose
// monta el volumen uploads_data. La variable existe para que los tests
// escriban en un directorio temporal y no ensucien el repositorio.
const UPLOADS_DIR = path.resolve(
  process.env.UPLOADS_DIR?.trim() || path.join(__dirname, "..", "..", "uploads"),
);

const PRODUCT_IMAGES_DIR = path.join(UPLOADS_DIR, "productos");

// Es lo que se guarda en producto.imagen_url. Debe coincidir con el prefijo
// con el que app.js expone UPLOADS_DIR.
const PRODUCT_IMAGES_PUBLIC_PATH = "/uploads/productos";

// Las miniaturas viven junto a las fotos para que el mismo express.static de
// /uploads las sirva sin configurar otra ruta.
const PRODUCT_THUMBS_DIR = path.join(PRODUCT_IMAGES_DIR, "thumbs");
const PRODUCT_THUMBS_PUBLIC_PATH = `${PRODUCT_IMAGES_PUBLIC_PATH}/thumbs`;

// Suficiente para las tarjetas del catálogo en pantallas de alta densidad, y
// mucho más liviano que la foto original de hasta 5 MB.
const PRODUCT_THUMB_WIDTH = 400;

const PRODUCT_IMAGE_MAX_BYTES = 5 * 1024 * 1024;

const PRODUCT_IMAGE_EXTENSIONS = {
  "image/jpeg": ".jpg",
  "image/png": ".png",
  "image/webp": ".webp",
};

module.exports = {
  UPLOADS_DIR,
  PRODUCT_IMAGES_DIR,
  PRODUCT_IMAGES_PUBLIC_PATH,
  PRODUCT_THUMBS_DIR,
  PRODUCT_THUMBS_PUBLIC_PATH,
  PRODUCT_THUMB_WIDTH,
  PRODUCT_IMAGE_MAX_BYTES,
  PRODUCT_IMAGE_EXTENSIONS,
};
