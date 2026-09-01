CREATE TABLE IF NOT EXISTS mensaje_asesoria (
    id_mensaje SERIAL PRIMARY KEY,
    id_pedido INT NOT NULL
        REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    id_usuario_remitente INT NOT NULL
        REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    mensaje TEXT NOT NULL
        CHECK (char_length(trim(mensaje)) BETWEEN 1 AND 1000),
    fecha_envio TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mensaje_asesoria_pedido_fecha
    ON mensaje_asesoria (id_pedido, fecha_envio, id_mensaje);
