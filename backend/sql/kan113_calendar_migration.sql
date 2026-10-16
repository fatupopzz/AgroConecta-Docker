-- KAN-113: calendario de cultivo. Cada siembra copia las tareas de su
-- plantilla con fechas concretas; así cambiar una plantilla después no mueve
-- el calendario de quien ya sembró.
-- Todo es idempotente porque las migraciones se aplican a mano y puede
-- correrse más de una vez.
BEGIN;

CREATE TABLE IF NOT EXISTS plantilla_cultivo (
    id_plantilla SERIAL PRIMARY KEY,
    -- Clave normalizada con normalizeCropName (minúsculas, con tilde): es lo
    -- que la app manda en POST /api/plantings.
    cultivo      VARCHAR(80) NOT NULL UNIQUE,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  TEXT,
    CONSTRAINT plantilla_cultivo_cultivo_no_vacio CHECK (BTRIM(cultivo) <> '')
);

CREATE TABLE IF NOT EXISTS plantilla_tarea (
    id_plantilla_tarea    SERIAL PRIMARY KEY,
    id_plantilla          INT NOT NULL
        REFERENCES plantilla_cultivo(id_plantilla) ON DELETE CASCADE,
    nombre                VARCHAR(120) NOT NULL,
    tipo                  VARCHAR(20) NOT NULL CHECK (
        tipo IN ('sembrar', 'fertilizar', 'fumigar', 'deshierbar', 'cosechar', 'otro')
    ),
    dia_desde_siembra     SMALLINT NOT NULL CHECK (dia_desde_siembra BETWEEN 0 AND 365),
    -- Desempata las tareas del mismo día; el id no sirve porque el orden de
    -- inserción de un INSERT ... SELECT no está garantizado.
    orden                 SMALLINT NOT NULL DEFAULT 0,
    id_categoria_sugerida INT REFERENCES categoria(id_categoria) ON DELETE SET NULL,
    CONSTRAINT plantilla_tarea_nombre_key UNIQUE (id_plantilla, nombre)
);

CREATE TABLE IF NOT EXISTS siembra (
    id_siembra     SERIAL PRIMARY KEY,
    id_usuario     INT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    id_plantilla   INT NOT NULL REFERENCES plantilla_cultivo(id_plantilla),
    fecha_siembra  DATE NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tarea_siembra (
    id_tarea              SERIAL PRIMARY KEY,
    id_siembra            INT NOT NULL REFERENCES siembra(id_siembra) ON DELETE CASCADE,
    nombre                VARCHAR(120) NOT NULL,
    tipo                  VARCHAR(20) NOT NULL CHECK (
        tipo IN ('sembrar', 'fertilizar', 'fumigar', 'deshierbar', 'cosechar', 'otro')
    ),
    fecha_programada      DATE NOT NULL,
    id_categoria_sugerida INT REFERENCES categoria(id_categoria) ON DELETE SET NULL,
    hecha                 BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_hecha           TIMESTAMPTZ,
    notificada            BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT tarea_siembra_fecha_hecha_consistente CHECK (
        (hecha AND fecha_hecha IS NOT NULL) OR (NOT hecha AND fecha_hecha IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_siembra_usuario
    ON siembra (id_usuario, fecha_siembra DESC);

CREATE INDEX IF NOT EXISTS idx_tarea_siembra_siembra_fecha
    ON tarea_siembra (id_siembra, fecha_programada);

-- El scheduler solo busca tareas del día que siguen pendientes de aviso; el
-- índice parcial se queda chico aunque el historial de tareas crezca.
CREATE INDEX IF NOT EXISTS idx_tarea_siembra_fecha_pendiente
    ON tarea_siembra (fecha_programada)
    WHERE NOT hecha AND NOT notificada;

INSERT INTO plantilla_cultivo (cultivo, nombre, descripcion) VALUES
    ('maíz', 'Maíz (elote)',
     'Ciclo corto para cosechar elote tierno, unos 80 días desde la siembra.'),
    ('frijol', 'Frijol',
     'Frijol de grano, unos 85 días desde la siembra hasta el arranque.'),
    ('tomate', 'Tomate',
     'Los días se cuentan desde el trasplante al terreno definitivo.'),
    ('papa', 'Papa',
     'Papa de ciclo intermedio, unos 110 días desde la siembra.')
ON CONFLICT (cultivo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion;

-- Días orientativos para Guatemala; varían con la variedad, la altitud y la
-- época de lluvias. La categoría se busca por nombre porque los id no son los
-- mismos en todas las bases. Para fungicidas se prefiere la categoría
-- Fungicidas y, si esa base no la tiene, se usa Pesticidas.
INSERT INTO plantilla_tarea
    (id_plantilla, nombre, tipo, dia_desde_siembra, orden, id_categoria_sugerida)
SELECT p.id_plantilla, t.nombre, t.tipo, t.dia, t.orden,
       COALESCE(
           (SELECT c.id_categoria FROM categoria c WHERE c.nombre = t.categoria),
           (SELECT c.id_categoria FROM categoria c WHERE c.nombre = t.categoria_respaldo)
       )
FROM (VALUES
    ('maíz', 'Siembra', 'sembrar', 0, 1, 'Semillas', NULL),
    ('maíz', 'Primera fertilización (NPK)', 'fertilizar', 10, 2, 'Fertilizantes', NULL),
    ('maíz', 'Control de malezas', 'deshierbar', 20, 3, 'Herbicidas', NULL),
    ('maíz', 'Control de gusano cogollero', 'fumigar', 25, 4, 'Pesticidas', NULL),
    ('maíz', 'Segunda fertilización (nitrogenada)', 'fertilizar', 35, 5, 'Fertilizantes', NULL),
    ('maíz', 'Revisión de plagas en floración', 'fumigar', 50, 6, 'Pesticidas', NULL),
    ('maíz', 'Cosecha de elote', 'cosechar', 80, 7, 'Herramientas', NULL),

    ('frijol', 'Siembra', 'sembrar', 0, 1, 'Semillas', NULL),
    ('frijol', 'Fertilización inicial', 'fertilizar', 10, 2, 'Fertilizantes', NULL),
    ('frijol', 'Control de malezas', 'deshierbar', 20, 3, 'Herbicidas', NULL),
    ('frijol', 'Control de mosca blanca', 'fumigar', 25, 4, 'Pesticidas', NULL),
    ('frijol', 'Prevención de hongos en floración', 'fumigar', 40, 5, 'Fungicidas', 'Pesticidas'),
    ('frijol', 'Cosecha (arranque)', 'cosechar', 85, 6, 'Herramientas', NULL),

    ('tomate', 'Trasplante', 'sembrar', 0, 1, NULL, NULL),
    ('tomate', 'Fertilización de arranque', 'fertilizar', 7, 2, 'Fertilizantes', NULL),
    ('tomate', 'Colocar tutores', 'otro', 15, 3, 'Herramientas', NULL),
    ('tomate', 'Control de mosca blanca', 'fumigar', 21, 4, 'Pesticidas', NULL),
    ('tomate', 'Fertilización de floración', 'fertilizar', 30, 5, 'Fertilizantes', NULL),
    ('tomate', 'Prevención de tizón', 'fumigar', 40, 6, 'Fungicidas', 'Pesticidas'),
    ('tomate', 'Inicio de cosecha', 'cosechar', 75, 7, 'Herramientas', NULL),

    ('papa', 'Siembra', 'sembrar', 0, 1, 'Semillas', NULL),
    ('papa', 'Fertilización de base', 'fertilizar', 0, 2, 'Fertilizantes', NULL),
    ('papa', 'Aporque y deshierbe', 'deshierbar', 30, 3, 'Herramientas', NULL),
    ('papa', 'Prevención de tizón tardío', 'fumigar', 35, 4, 'Fungicidas', 'Pesticidas'),
    ('papa', 'Segunda fertilización', 'fertilizar', 45, 5, 'Fertilizantes', NULL),
    ('papa', 'Control de polilla', 'fumigar', 60, 6, 'Pesticidas', NULL),
    ('papa', 'Cosecha', 'cosechar', 110, 7, 'Herramientas', NULL)
) AS t (cultivo, nombre, tipo, dia, orden, categoria, categoria_respaldo)
JOIN plantilla_cultivo p ON p.cultivo = t.cultivo
ON CONFLICT (id_plantilla, nombre) DO UPDATE SET
    tipo = EXCLUDED.tipo,
    dia_desde_siembra = EXCLUDED.dia_desde_siembra,
    orden = EXCLUDED.orden,
    id_categoria_sugerida = EXCLUDED.id_categoria_sugerida;

COMMIT;
