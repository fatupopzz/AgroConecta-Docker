BEGIN;

CREATE TABLE IF NOT EXISTS ciclo_cultivo (
    id_ciclo SERIAL PRIMARY KEY,
    cultivo VARCHAR(80) NOT NULL,
    fase VARCHAR(100) NOT NULL,
    mes_inicio SMALLINT NOT NULL CHECK (mes_inicio BETWEEN 1 AND 12),
    mes_fin SMALLINT NOT NULL CHECK (mes_fin BETWEEN 1 AND 12),
    descripcion TEXT NOT NULL,
    productos_recomendados TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    CONSTRAINT ciclo_cultivo_cultivo_no_vacio CHECK (BTRIM(cultivo) <> ''),
    CONSTRAINT ciclo_cultivo_fase_no_vacia CHECK (BTRIM(fase) <> ''),
    CONSTRAINT ciclo_cultivo_productos_no_vacios
        CHECK (CARDINALITY(productos_recomendados) > 0),
    CONSTRAINT ciclo_cultivo_cultivo_fase_key UNIQUE (cultivo, fase)
);

CREATE INDEX IF NOT EXISTS idx_ciclo_cultivo_busqueda
    ON ciclo_cultivo (LOWER(cultivo), mes_inicio);

-- Calendario nacional de referencia. Las fechas pueden variar por región,
-- altitud, disponibilidad de riego y condiciones climáticas locales.
INSERT INTO ciclo_cultivo
    (cultivo, fase, mes_inicio, mes_fin, descripcion, productos_recomendados)
VALUES
    ('maíz', 'Pre-siembra', 1, 4,
     'Planificación del ciclo, selección de semilla y preparación del terreno antes del inicio de las lluvias.',
     ARRAY['Semilla certificada de maíz', 'Fertilizante orgánico', 'Herramientas para preparar suelo']),
    ('maíz', 'Siembra', 5, 6,
     'Establecimiento del cultivo al inicio de la época lluviosa y aplicación de fertilización de base.',
     ARRAY['Semilla certificada de maíz', 'Fertilizante NPK', 'Herbicida preemergente']),
    ('maíz', 'Desarrollo vegetativo', 7, 9,
     'Crecimiento activo; conviene monitorear malezas, nutrición, gusano cogollero y enfermedades.',
     ARRAY['Fertilizante nitrogenado', 'Insecticida para gusano cogollero', 'Fungicida agrícola']),
    ('maíz', 'Cosecha y postcosecha', 10, 12,
     'Cosecha, secado y almacenamiento del grano con control periódico de humedad y plagas.',
     ARRAY['Sacos para cosecha', 'Medidor de humedad', 'Protector de grano almacenado']),

    ('frijol', 'Pre-siembra', 1, 4,
     'Selección de semilla, planificación y preparación del suelo para la siembra de primera.',
     ARRAY['Semilla certificada de frijol', 'Abono orgánico', 'Tratamiento para semilla']),
    ('frijol', 'Siembra de primera', 5, 6,
     'Siembra asociada al inicio de las lluvias y fertilización inicial según las condiciones del suelo.',
     ARRAY['Semilla certificada de frijol', 'Fertilizante NPK', 'Inoculante para leguminosas']),
    ('frijol', 'Desarrollo y postrera', 7, 9,
     'Manejo del cultivo de primera y establecimiento de postrera donde las condiciones locales lo permiten.',
     ARRAY['Fertilizante foliar', 'Fungicida para frijol', 'Insecticida agrícola']),
    ('frijol', 'Cosecha y postcosecha', 10, 12,
     'Arranque, secado, selección y almacenamiento del grano para reducir pérdidas por humedad y plagas.',
     ARRAY['Lona para secado', 'Sacos para grano', 'Protector de grano almacenado']),

    ('café', 'Cosecha', 11, 3,
     'Corte selectivo de cerezas maduras y manejo cuidadoso del fruto; la fecha exacta depende de región y altitud.',
     ARRAY['Canastos de cosecha', 'Sacos para café', 'Herramientas de corte']),
    ('café', 'Poda y mantenimiento', 4, 5,
     'Poda posterior a la cosecha, manejo de sombra, limpieza y recuperación nutricional de la plantación.',
     ARRAY['Tijera de poda', 'Fertilizante para café', 'Fungicida agrícola']),
    ('café', 'Floración y cuajado', 6, 7,
     'Monitoreo de floración, cuajado, nutrición y sanidad durante el establecimiento de los frutos.',
     ARRAY['Fertilizante foliar', 'Micronutrientes', 'Control preventivo de roya']),
    ('café', 'Desarrollo del fruto', 8, 10,
     'Llenado y maduración del fruto con monitoreo de broca, roya y disponibilidad de nutrientes.',
     ARRAY['Trampa para broca', 'Fertilizante para café', 'Fungicida para roya']),

    ('elote', 'Pre-siembra', 1, 4,
     'Planificación, preparación del terreno y selección de semilla para producción de mazorca tierna.',
     ARRAY['Semilla de maíz dulce', 'Abono orgánico', 'Herramientas para preparar suelo']),
    ('elote', 'Siembra', 5, 6,
     'Siembra al inicio de las lluvias y aplicación de fertilizante de base para favorecer el establecimiento.',
     ARRAY['Semilla de maíz dulce', 'Fertilizante NPK', 'Herbicida preemergente']),
    ('elote', 'Desarrollo y llenado', 7, 8,
     'Crecimiento, floración y llenado de la mazorca con vigilancia de plagas y disponibilidad de agua.',
     ARRAY['Fertilizante nitrogenado', 'Insecticida para gusano cogollero', 'Equipo de riego']),
    ('elote', 'Cosecha y planificación', 9, 12,
     'Cosecha de mazorca tierna y preparación de insumos para el siguiente ciclo productivo.',
     ARRAY['Canastos de cosecha', 'Herramientas de corte', 'Semilla de maíz dulce']),

    ('tomate', 'Semillero', 1, 2,
     'Preparación de sustrato, germinación y manejo sanitario de plántulas antes del trasplante.',
     ARRAY['Semilla de tomate', 'Bandeja para semillero', 'Sustrato para plántulas']),
    ('tomate', 'Trasplante', 3, 4,
     'Preparación de camas y establecimiento de plántulas con fertilización base y riego controlado.',
     ARRAY['Fertilizante NPK', 'Cobertura plástica', 'Equipo de riego']),
    ('tomate', 'Desarrollo y floración', 5, 7,
     'Crecimiento, tutorado, floración y monitoreo preventivo de insectos y enfermedades.',
     ARRAY['Fertilizante foliar', 'Tutor para tomate', 'Control para mosca blanca']),
    ('tomate', 'Producción y cosecha', 8, 10,
     'Llenado, maduración y cortes escalonados con nutrición y riego ajustados a la producción.',
     ARRAY['Nitrato de potasio', 'Fungicida para tomate', 'Canastos de cosecha']),
    ('tomate', 'Preparación del siguiente ciclo', 11, 12,
     'Limpieza del área, manejo de residuos y preparación de suelo o estructuras para el siguiente ciclo.',
     ARRAY['Desinfectante agrícola', 'Abono orgánico', 'Herramientas para preparar suelo'])
ON CONFLICT (cultivo, fase) DO UPDATE SET
    mes_inicio = EXCLUDED.mes_inicio,
    mes_fin = EXCLUDED.mes_fin,
    descripcion = EXCLUDED.descripcion,
    productos_recomendados = EXCLUDED.productos_recomendados;

COMMIT;
