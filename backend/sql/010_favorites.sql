CREATE TABLE IF NOT EXISTS favorito (
    id SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    id_producto INT NOT NULL REFERENCES producto(id_producto) ON DELETE CASCADE,
    fecha_agregado TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (id_usuario, id_producto)
);

CREATE INDEX IF NOT EXISTS idx_favorito_usuario_fecha
    ON favorito (id_usuario, fecha_agregado DESC);

CREATE INDEX IF NOT EXISTS idx_favorito_producto
    ON favorito (id_producto);
