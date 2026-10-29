-- Las guardas se agregaron al versionar las migraciones: 001_init.sql ya crea
-- estas tablas, y sin ellas este script fallaba en una base nueva.
CREATE TABLE IF NOT EXISTS carrito (
  id_carrito SERIAL PRIMARY KEY,
  id_agricultor INTEGER NOT NULL REFERENCES agricultor(id_agricultor) ON DELETE CASCADE,
  fecha_creacion TIMESTAMP DEFAULT NOW(),
  fecha_actualizacion TIMESTAMP DEFAULT NOW(),
  UNIQUE(id_agricultor)
);

CREATE TABLE IF NOT EXISTS item_carrito (
  id_item SERIAL PRIMARY KEY,
  id_carrito INTEGER NOT NULL REFERENCES carrito(id_carrito) ON DELETE CASCADE,
  id_inventario INTEGER NOT NULL REFERENCES inventario_distribuidor(id_inventario),
  cantidad INTEGER NOT NULL CHECK (cantidad > 0),
  UNIQUE(id_carrito, id_inventario)
);
