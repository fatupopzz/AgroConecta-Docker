-- =====================================================
-- Migración: HU-029 Alertas de plagas + geolocalización
-- =====================================================

CREATE TABLE IF NOT EXISTS alerta_plaga (
    id_alerta       SERIAL PRIMARY KEY,
    id_usuario      INT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    tipo_plaga      VARCHAR(150) NOT NULL CHECK (BTRIM(tipo_plaga) <> ''),
    cultivo_afectado VARCHAR(100) NOT NULL CHECK (BTRIM(cultivo_afectado) <> ''),
    descripcion     TEXT,
    latitud         DECIMAL(10,7) NOT NULL,
    longitud        DECIMAL(10,7) NOT NULL,
    departamento    VARCHAR(100),
    municipio       VARCHAR(100),
    severidad       VARCHAR(20) DEFAULT 'media'
                    CHECK (severidad IN ('baja', 'media', 'alta', 'critica')),
    activa          BOOLEAN DEFAULT TRUE,
    fecha_reporte   TIMESTAMP DEFAULT NOW(),
    fecha_expiracion TIMESTAMP DEFAULT (NOW() + INTERVAL '30 days')
);

CREATE INDEX IF NOT EXISTS idx_alerta_plaga_ubicacion
    ON alerta_plaga (latitud, longitud);

CREATE INDEX IF NOT EXISTS idx_alerta_plaga_cultivo
    ON alerta_plaga (LOWER(cultivo_afectado));

CREATE INDEX IF NOT EXISTS idx_alerta_plaga_activa_fecha
    ON alerta_plaga (activa, fecha_reporte DESC);
