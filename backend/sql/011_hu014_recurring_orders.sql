CREATE TABLE IF NOT EXISTS pedido_recurrente (
    id SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    frecuencia VARCHAR(20) NOT NULL
        CHECK (frecuencia IN ('diaria', 'semanal', 'quincenal', 'mensual')),
    productos JSONB NOT NULL,
    fecha_proximo TIMESTAMPTZ NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'activo'
        CHECK (estado IN ('activo', 'pausado', 'cancelado')),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pedido_recurrente_productos_objeto
        CHECK (jsonb_typeof(productos) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_pedido_recurrente_pendientes
    ON pedido_recurrente (fecha_proximo)
    WHERE estado = 'activo';

CREATE INDEX IF NOT EXISTS idx_pedido_recurrente_usuario
    ON pedido_recurrente (id_usuario, fecha_creacion DESC);
