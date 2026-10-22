const AppError = require("../errors/AppError");
const {
  detectImageMime,
  readFileHeader,
  replaceProductImage,
} = require("../services/productImageService");

// POST /api/products/:id/image
// La autorización y la validación de tamaño/MIME ya pasaron en los middlewares
// de la ruta; aquí solo queda confirmar el contenido y persistir.
const uploadProductImage = async (req, res) => {
  if (!req.file) {
    throw new AppError(400, 'Falta la imagen en el campo "imagen"');
  }

  const realMime = detectImageMime(await readFileHeader(req.file.path));
  if (realMime !== req.file.mimetype) {
    throw new AppError(400, "El archivo no es una imagen JPEG, PNG o WEBP válida");
  }

  const producto = await replaceProductImage(req.params.id, req.file.filename);

  // El producto pudo desactivarse entre la autorización y este punto.
  if (!producto) {
    throw new AppError(404, "Producto no encontrado");
  }

  return res.json({
    message: "Imagen del producto actualizada",
    producto: {
      id_producto: producto.id_producto,
      imagen_url: producto.imagen_url,
      imagen_thumb_url: producto.imagen_thumb_url,
    },
  });
};

module.exports = { uploadProductImage };
