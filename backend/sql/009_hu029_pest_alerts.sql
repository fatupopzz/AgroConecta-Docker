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

CREATE TABLE IF NOT EXISTS instalacion_alerta_plaga (
    fcm_registration_token   TEXT PRIMARY KEY,
    id_usuario               INT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    latitud                  DECIMAL(10,7) NOT NULL,
    longitud                 DECIMAL(10,7) NOT NULL,
    fecha_actualizacion      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Actualiza instalaciones creadas por versiones anteriores de HU-029.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'instalacion_alerta_plaga'
          AND column_name = 'firebase_installation_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'instalacion_alerta_plaga'
          AND column_name = 'fcm_registration_token'
    ) THEN
        ALTER TABLE instalacion_alerta_plaga
            RENAME COLUMN firebase_installation_id TO fcm_registration_token;
    END IF;
END $$;

ALTER TABLE instalacion_alerta_plaga
    ALTER COLUMN fcm_registration_token TYPE TEXT;
