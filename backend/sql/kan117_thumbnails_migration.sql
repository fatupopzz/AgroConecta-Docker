-- Miniatura WEBP de 400 px de ancho para las tarjetas del catálogo, inicio,
-- carrito y favoritos; el detalle del producto sigue usando imagen_url.
-- Igual que imagen_url, guarda la ruta relativa (/uploads/productos/thumbs/...).
-- Queda en NULL para las fotos subidas antes de KAN-117 hasta correr
-- scripts/generate_thumbnails.js; mientras tanto la app usa la foto grande.
-- IF NOT EXISTS porque las migraciones se aplican a mano y puede correrse dos veces.
ALTER TABLE producto
ADD COLUMN IF NOT EXISTS imagen_thumb_url VARCHAR(255);
