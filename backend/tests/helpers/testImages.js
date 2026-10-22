const sharp = require("sharp");

// Imágenes reales y no solo la firma de los primeros bytes: la subida ahora
// decodifica la foto para generar la miniatura, y un PNG de relleno se rechaza.
const createTestImage = ({ width = 32, height = 24, format = "png" } = {}) =>
  sharp({
    create: { width, height, channels: 3, background: { r: 60, g: 140, b: 70 } },
  })
    .toFormat(format)
    .toBuffer();

module.exports = { createTestImage };
