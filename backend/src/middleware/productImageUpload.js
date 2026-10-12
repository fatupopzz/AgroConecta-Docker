const crypto = require("node:crypto");
const fs = require("node:fs/promises");
const multer = require("multer");
const { pool } = require("../config/db");
const AppError = require("../errors/AppError");
const {
  PRODUCT_IMAGES_DIR,
  PRODUCT_IMAGE_MAX_BYTES,
  PRODUCT_IMAGE_EXTENSIONS,
} = require("../config/uploads");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

/**
 * Corre antes de multer a propósito: si el usuario no puede tocar el producto
 * se le responde sin haber escrito nada en disco.
 *
 * El producto no tiene columna de dueño; un distribuidor "es dueño" cuando lo
 * tiene publicado en su inventario. Se exige además que siga verificado, por la
 * misma razón que updateInventory: una cuenta suspendida no debe cambiar lo que
 * ven los agricultores. Los dos casos se separan para que el mensaje diga qué
 * hacer: no es lo mismo un producto ajeno que una cuenta en revisión.
 */
const authorizeProductImage = async (req, res, next) => {
  if (!isPositiveInteger(req.params.id)) {
    throw new AppError(400, "ID de producto inválido");
  }

  const { tipo, id: idUsuario } = req.user || {};
  if (tipo !== "administrador" && tipo !== "distribuidor") {
    throw new AppError(403, "Solo distribuidores o administradores pueden subir imágenes");
  }

  const result = await pool.query(
    `SELECT p.id_producto,
            EXISTS (
              SELECT 1
              FROM inventario_distribuidor i
              JOIN distribuidor d ON d.id_distribuidor = i.id_distribuidor
              WHERE i.id_producto = p.id_producto
                AND d.id_usuario = $2
            ) AS es_duenio,
            EXISTS (
              SELECT 1
              FROM distribuidor d
              WHERE d.id_usuario = $2
                AND d.estado_verificacion = 'verificado'
            ) AS verificado
     FROM producto p
     WHERE p.id_producto = $1
       AND p.activo = TRUE`,
    [Number(req.params.id), Number(idUsuario)],
  );

  if (result.rows.length === 0) {
    throw new AppError(404, "Producto no encontrado");
  }

  if (tipo === "distribuidor") {
    const { es_duenio, verificado } = result.rows[0];
    if (!es_duenio) {
      throw new AppError(403, "No puedes cambiar la imagen de un producto que no publicaste");
    }
    if (!verificado) {
      throw new AppError(403, "Tu cuenta aún no está verificada");
    }
  }

  next();
};

const storage = multer.diskStorage({
  // El directorio se crea al recibir la primera foto y no al cargar el módulo,
  // así importar app.js (por ejemplo en los tests) no deja carpetas sueltas.
  destination: (req, file, callback) => {
    fs.mkdir(PRODUCT_IMAGES_DIR, { recursive: true })
      .then(() => callback(null, PRODUCT_IMAGES_DIR))
      .catch(callback);
  },
  // El nombre que manda el cliente se descarta: puede traer rutas, caracteres
  // raros o chocar con otra foto. La extensión sale del MIME ya validado.
  filename: (req, file, callback) => {
    callback(null, `${crypto.randomUUID()}${PRODUCT_IMAGE_EXTENSIONS[file.mimetype]}`);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: PRODUCT_IMAGE_MAX_BYTES, files: 1 },
  fileFilter: (req, file, callback) => {
    if (!PRODUCT_IMAGE_EXTENSIONS[file.mimetype]) {
      callback(new AppError(400, "Formato no permitido. Usa JPEG, PNG o WEBP"));
      return;
    }
    callback(null, true);
  },
}).single("imagen");

const toUploadError = (error) => {
  if (!(error instanceof multer.MulterError)) {
    return error;
  }
  if (error.code === "LIMIT_FILE_SIZE") {
    return new AppError(413, "La imagen supera el máximo de 5 MB");
  }
  if (error.code === "LIMIT_UNEXPECTED_FILE" || error.code === "LIMIT_FILE_COUNT") {
    return new AppError(400, 'Envía una sola imagen en el campo "imagen"');
  }
  return new AppError(400, "No se pudo procesar la imagen enviada");
};

// multer entrega sus propios errores; se traducen a AppError para que el
// cliente reciba el mismo formato { error } que en el resto de la API.
const receiveProductImage = (req, res, next) => {
  upload(req, res, (error) => next(error ? toUploadError(error) : undefined));
};

/**
 * Si algo falla después de que multer escribió el archivo (firma inválida,
 * error de base de datos) la foto quedaría huérfana en el volumen. Este
 * manejador la borra y deja que el errorHandler central arme la respuesta.
 */
const discardUploadedImageOnError = (error, req, res, next) => {
  if (!req.file?.path) {
    next(error);
    return;
  }
  fs.rm(req.file.path, { force: true })
    .catch((rmError) => {
      console.warn(`No se pudo borrar la imagen descartada ${req.file.path}: ${rmError.message}`);
    })
    .finally(() => next(error));
};

module.exports = {
  authorizeProductImage,
  receiveProductImage,
  discardUploadedImageOnError,
};
