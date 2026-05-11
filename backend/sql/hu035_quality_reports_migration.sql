CREATE TABLE IF NOT EXISTS reporte_calidad (
    id_reporte SERIAL PRIMARY KEY,
    id_agricultor INTEGER NOT NULL,
    id_pedido INTEGER NOT NULL,
    id_producto INTEGER NOT NULL,
    descripcion_problema TEXT NOT NULL,
    estado_reporte VARCHAR(20) NOT NULL DEFAULT 'abierto',
    fecha_reporte TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolucion TEXT,
    fecha_resolucion TIMESTAMP,

    CONSTRAINT reporte_calidad_estado_reporte_check
        CHECK (estado_reporte IN ('abierto', 'en_revision', 'resuelto', 'cerrado'))
);

ALTER TABLE reporte_calidad
ADD COLUMN IF NOT EXISTS fecha_resolucion TIMESTAMP;
