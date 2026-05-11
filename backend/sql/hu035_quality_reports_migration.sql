CREATE TABLE IF NOT EXISTS reporte_calidad (
    id SERIAL PRIMARY KEY,
    agricultor_id INTEGER NOT NULL,
    producto_id INTEGER NOT NULL,
    pedido_id INTEGER NOT NULL,
    descripcion TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    accion_tomada TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion TIMESTAMP,

    CONSTRAINT chk_estado_reporte_calidad
        CHECK (estado IN ('pendiente', 'en_revision', 'resuelto'))
);
