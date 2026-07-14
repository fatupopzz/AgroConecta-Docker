CREATE TABLE IF NOT EXISTS producto_seguido (
    id SERIAL PRIMARY KEY,
    id_agricultor INT NOT NULL REFERENCES agricultor(id_agricultor) ON DELETE CASCADE,
    id_producto INT NOT NULL REFERENCES producto(id_producto) ON DELETE CASCADE,
    precio_al_seguir DECIMAL(10,2) NOT NULL,
    fecha TIMESTAMP DEFAULT NOW(),
    UNIQUE (id_agricultor, id_producto)
);

CREATE INDEX IF NOT EXISTS idx_producto_seguido_agricultor_producto
    ON producto_seguido (id_agricultor, id_producto);

CREATE INDEX IF NOT EXISTS idx_producto_seguido_producto
    ON producto_seguido (id_producto);
