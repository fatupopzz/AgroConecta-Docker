-- Ejecutar después de init.sql y hu029_pest_alerts_migration.sql.
BEGIN;

CREATE TABLE IF NOT EXISTS diagnostico (
    id_diagnostico SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    cultivo VARCHAR(100) NOT NULL CHECK (BTRIM(cultivo) <> ''),
    enfermedad VARCHAR(150) NOT NULL CHECK (BTRIM(enfermedad) <> ''),
    confianza DOUBLE PRECISION NOT NULL CHECK (confianza BETWEEN 0 AND 1),
    foto TEXT NOT NULL CHECK (BTRIM(foto) <> ''),
    latitud DECIMAL(10,7) CHECK (latitud BETWEEN -90 AND 90),
    longitud DECIMAL(10,7) CHECK (longitud BETWEEN -180 AND 180),
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    id_alerta INT UNIQUE REFERENCES alerta_plaga(id_alerta) ON DELETE SET NULL,
    CHECK ((latitud IS NULL) = (longitud IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_diagnostico_usuario_fecha
    ON diagnostico (id_usuario, fecha DESC, id_diagnostico DESC);

-- Un mapeo por enfermedad/cultivo/categoría. Los nombres de enfermedad deben
-- coincidir con las etiquetas del modelo; no inferir tratamientos por categoría.
CREATE TABLE IF NOT EXISTS enfermedad_tratamiento (
    id_tratamiento SERIAL PRIMARY KEY,
    enfermedad VARCHAR(150) NOT NULL CHECK (BTRIM(enfermedad) <> ''),
    cultivo VARCHAR(100) NOT NULL CHECK (BTRIM(cultivo) <> ''),
    id_categoria INT NOT NULL REFERENCES categoria(id_categoria),
    ingredientes_activos TEXT[] NOT NULL CHECK (
        cardinality(ingredientes_activos) > 0
        AND array_position(ingredientes_activos, NULL) IS NULL
        AND array_position(ingredientes_activos, '') IS NULL
    ),
    recomendacion_general TEXT NOT NULL CHECK (BTRIM(recomendacion_general) <> '')
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_enfermedad_tratamiento_clave
    ON enfermedad_tratamiento (LOWER(enfermedad), LOWER(cultivo), id_categoria);

COMMIT;
