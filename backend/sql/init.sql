CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario    SERIAL PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    apellido      VARCHAR(100),
    telefono      VARCHAR(20)  UNIQUE NOT NULL,
    email         VARCHAR(150) UNIQUE,
    contrasena_hash TEXT       NOT NULL,
    tipo_usuario  VARCHAR(20)  NOT NULL CHECK (tipo_usuario IN ('agricultor', 'distribuidor', 'administrador')),
    activo        BOOLEAN      DEFAULT TRUE,
    fecha_registro TIMESTAMP   DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS agricultor (
    id_agricultor   SERIAL PRIMARY KEY,
    id_usuario      INT UNIQUE NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    departamento    VARCHAR(100),
    municipio       VARCHAR(100),
    tipo_agricultor VARCHAR(30) CHECK (tipo_agricultor IN ('pequena_escala', 'mediana_escala', 'industrial')),
    tamano_terreno_ha DECIMAL(10,2),
    cultivos_principales TEXT,
    tiene_membresia BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS distribuidor (
    id_distribuidor   SERIAL PRIMARY KEY,
    id_usuario        INT UNIQUE NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    nombre_negocio    VARCHAR(150) NOT NULL,
    nit               VARCHAR(30)  UNIQUE,
    departamento      VARCHAR(100),
    direccion         TEXT,
    estado_verificacion VARCHAR(20) DEFAULT 'pendiente' CHECK (estado_verificacion IN ('pendiente', 'verificado', 'suspendido')),
    calificacion_promedio DECIMAL(3,2) DEFAULT 0.0,
    fecha_verificacion TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categoria (
    id_categoria SERIAL PRIMARY KEY,
    nombre       VARCHAR(80) UNIQUE NOT NULL,
    descripcion  TEXT
);

CREATE TABLE IF NOT EXISTS producto (
    id_producto         SERIAL PRIMARY KEY,
    id_categoria        INT REFERENCES categoria(id_categoria),
    nombre              VARCHAR(150) NOT NULL,
    marca               VARCHAR(100),
    descripcion         TEXT,
    composicion         TEXT,
    dosis_recomendada   TEXT,
    instrucciones_uso   TEXT,
    calificacion_promedio DECIMAL(3,2) DEFAULT 0.0,
    activo              BOOLEAN DEFAULT TRUE
);


CREATE TABLE IF NOT EXISTS direccion_entrega (
    id_direccion    SERIAL PRIMARY KEY,
    id_agricultor   INT NOT NULL REFERENCES agricultor(id_agricultor),
    alias           VARCHAR(60),
    descripcion     TEXT,
    departamento    VARCHAR(100),
    municipio       VARCHAR(100),
    latitud         DECIMAL(10,7),
    longitud        DECIMAL(10,7),
    es_predeterminada BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS pedido (
    id_pedido           SERIAL PRIMARY KEY,
    id_agricultor       INT NOT NULL REFERENCES agricultor(id_agricultor),
    id_distribuidor     INT NOT NULL REFERENCES distribuidor(id_distribuidor),
    fecha_pedido        TIMESTAMP DEFAULT NOW(),
    fecha_entrega_real  TIMESTAMP,
    estado              VARCHAR(30) DEFAULT 'confirmado' CHECK (estado IN ('confirmado', 'preparando', 'en_ruta', 'entregado', 'cancelado')),
    tipo_entrega        VARCHAR(20) CHECK (tipo_entrega IN ('domicilio', 'punto_recogida')),
    direccion_entrega   TEXT,
    es_urgente          BOOLEAN DEFAULT FALSE,
    tipo_plaga          VARCHAR(100),
    total_pedido        DECIMAL(10,2),
    costo_envio         DECIMAL(10,2) DEFAULT 0.0,
    notas               TEXT
);

CREATE TABLE IF NOT EXISTS pedido_tracking (
    id_tracking SERIAL PRIMARY KEY,
    id_pedido   INT NOT NULL REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    estado      VARCHAR(30) NOT NULL CHECK (estado IN ('confirmado', 'preparando', 'en_ruta', 'entregado', 'cancelado')),
    "timestamp" TIMESTAMP DEFAULT NOW(),
    notas       TEXT
);

CREATE INDEX IF NOT EXISTS idx_pedido_tracking_id_pedido_timestamp
    ON pedido_tracking (id_pedido, "timestamp");

CREATE TABLE IF NOT EXISTS inventario_distribuidor (
    id_inventario       SERIAL PRIMARY KEY,
    id_distribuidor     INT NOT NULL REFERENCES distribuidor(id_distribuidor),
    id_producto         INT NOT NULL REFERENCES producto(id_producto),
    precio              DECIMAL(10,2) NOT NULL,
    stock_disponible    INT DEFAULT 0,
    unidad_medida       VARCHAR(30),
    tiempo_entrega_dias INT,  
    ultima_actualizacion TIMESTAMP DEFAULT NOW(),
    UNIQUE (id_distribuidor, id_producto)
);

CREATE TABLE IF NOT EXISTS detalle_pedido (
    id_detalle      SERIAL PRIMARY KEY,
    id_pedido       INT NOT NULL REFERENCES pedido(id_pedido),
    id_inventario   INT NOT NULL REFERENCES inventario_distribuidor(id_inventario),
    cantidad        INT NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED
);

CREATE TABLE IF NOT EXISTS pago (
    id_pago             SERIAL PRIMARY KEY,
    id_pedido           INT UNIQUE NOT NULL REFERENCES pedido(id_pedido),
    metodo_pago         VARCHAR(30) CHECK (metodo_pago IN ('contra_entrega', 'tigo_money', 'banrural_movil', 'tarjeta')),
    monto               DECIMAL(10,2) NOT NULL,

    estado_pago         VARCHAR(20) DEFAULT 'pendiente'
                        CHECK (estado_pago IN ('pendiente', 'processing', 'completado', 'failed')),

    proveedor           VARCHAR(30),
    created_at          TIMESTAMP DEFAULT NOW(),
    fecha_pago          TIMESTAMP,
    referencia_transaccion VARCHAR(100),
    fecha_confirmacion  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resena (
    id_resena       SERIAL PRIMARY KEY,
    id_agricultor   INT NOT NULL REFERENCES agricultor(id_agricultor),
    id_producto     INT NOT NULL REFERENCES producto(id_producto),
    id_pedido       INT NOT NULL REFERENCES pedido(id_pedido),
    calificacion    INT NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario      TEXT,
    fecha_resena    TIMESTAMP DEFAULT NOW(),
    UNIQUE (id_agricultor, id_producto)
);

CREATE TABLE IF NOT EXISTS reporte_calidad (
    id_reporte          SERIAL PRIMARY KEY,
    id_agricultor       INT NOT NULL REFERENCES agricultor(id_agricultor),
    id_pedido           INT NOT NULL REFERENCES pedido(id_pedido),
    id_producto         INT NOT NULL REFERENCES producto(id_producto),
    descripcion_problema TEXT NOT NULL,
    estado_reporte      VARCHAR(20) DEFAULT 'abierto' CHECK (estado_reporte IN ('abierto', 'en_revision', 'resuelto', 'cerrado')),
    fecha_reporte       TIMESTAMP DEFAULT NOW(),
    resolucion          TEXT
);

CREATE TABLE IF NOT EXISTS carrito (
  id_carrito SERIAL PRIMARY KEY,
  id_agricultor INTEGER NOT NULL REFERENCES agricultor(id_agricultor) ON DELETE CASCADE,
  fecha_creacion TIMESTAMP DEFAULT NOW(),
  fecha_actualizacion TIMESTAMP DEFAULT NOW(),
  UNIQUE(id_agricultor)
);

CREATE TABLE IF NOT EXISTS item_carrito (
  id_item SERIAL PRIMARY KEY,
  id_carrito INTEGER NOT NULL REFERENCES carrito(id_carrito) ON DELETE CASCADE,
  id_inventario INTEGER NOT NULL REFERENCES inventario_distribuidor(id_inventario),
  cantidad INTEGER NOT NULL CHECK (cantidad > 0),
  UNIQUE(id_carrito, id_inventario)
);

CREATE TABLE IF NOT EXISTS producto_seguido (
    id SERIAL PRIMARY KEY,
    id_agricultor INT NOT NULL REFERENCES agricultor(id_agricultor) ON DELETE CASCADE,
    id_producto INT NOT NULL REFERENCES producto(id_producto) ON DELETE CASCADE,
    precio_al_seguir DECIMAL(10,2) NOT NULL,
    fecha TIMESTAMP DEFAULT NOW(),
    UNIQUE (id_agricultor, id_producto)
);

CREATE INDEX IF NOT EXISTS idx_producto_seguido_agricultor_producto
    ON producto_seguido (id_agricultor, id_producto);

CREATE INDEX IF NOT EXISTS idx_producto_seguido_producto
    ON producto_seguido (id_producto);

CREATE TABLE IF NOT EXISTS notificacion (
    id_notificacion SERIAL PRIMARY KEY,
    id_agricultor INT REFERENCES agricultor(id_agricultor) ON DELETE CASCADE,
    tipo VARCHAR(40),
    contenido JSONB DEFAULT '{}'::jsonb,
    leida BOOLEAN DEFAULT FALSE,
    fecha TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notificacion_agricultor_fecha
    ON notificacion (id_agricultor, fecha DESC);

CREATE INDEX IF NOT EXISTS idx_notificacion_tipo
    ON notificacion (tipo);

CREATE TABLE IF NOT EXISTS resena_distribuidor (
    id_resena SERIAL PRIMARY KEY,
    id_agricultor INTEGER NOT NULL REFERENCES agricultor(id_agricultor),
    id_distribuidor INTEGER NOT NULL REFERENCES distribuidor(id_distribuidor),
    calificacion INTEGER NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario TEXT,
    fecha_resena TIMESTAMP DEFAULT NOW(),
    UNIQUE (id_agricultor, id_distribuidor)
);

CREATE TABLE IF NOT EXISTS ciclo_cultivo (
    id_ciclo SERIAL PRIMARY KEY,
    cultivo VARCHAR(80) NOT NULL,
    fase VARCHAR(100) NOT NULL,
    mes_inicio SMALLINT NOT NULL CHECK (mes_inicio BETWEEN 1 AND 12),
    mes_fin SMALLINT NOT NULL CHECK (mes_fin BETWEEN 1 AND 12),
    descripcion TEXT NOT NULL,
    productos_recomendados TEXT[] NOT NULL,
    CONSTRAINT ciclo_cultivo_cultivo_no_vacio CHECK (BTRIM(cultivo) <> ''),
    CONSTRAINT ciclo_cultivo_fase_no_vacia CHECK (BTRIM(fase) <> ''),
    CONSTRAINT ciclo_cultivo_productos_no_vacios
        CHECK (CARDINALITY(productos_recomendados) > 0),
    CONSTRAINT ciclo_cultivo_cultivo_fase_key UNIQUE (cultivo, fase)
);

CREATE INDEX IF NOT EXISTS idx_ciclo_cultivo_busqueda
    ON ciclo_cultivo (LOWER(cultivo), mes_inicio);

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

INSERT INTO categoria (nombre, descripcion) VALUES
    ('Fertilizantes', 'Productos para nutrición del suelo y cultivos'),
    ('Pesticidas', 'Control de plagas e insectos'),
    ('Herbicidas', 'Control de malezas'),
    ('Semillas', 'Semillas certificadas para siembra'),
    ('Herramientas', 'Equipos y herramientas agrícolas')
ON CONFLICT DO NOTHING;

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

INSERT INTO usuario (nombre, telefono, email, contrasena_hash, tipo_usuario) VALUES
    ('Admin AgroConecta', '50200000000', 'admin@agroconecta.gt',
     crypt('admin123', gen_salt('bf', 10)), 'administrador')
ON CONFLICT DO NOTHING;

-- ─── Migraciones idempotentes para BDs existentes ───
-- Estas sentencias usan IF NOT EXISTS para que sea seguro re-ejecutarlas.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS apellido VARCHAR(100);
ALTER TABLE distribuidor
ADD COLUMN IF NOT EXISTS direccion TEXT;

-- =====================================================
-- Migración: Notificaciones para distribuidores
-- =====================================================

ALTER TABLE notificacion
ADD COLUMN IF NOT EXISTS id_distribuidor INT
REFERENCES distribuidor(id_distribuidor)
ON DELETE CASCADE;

ALTER TABLE notificacion
ADD COLUMN IF NOT EXISTS id_pedido INT
REFERENCES pedido(id_pedido)
ON DELETE CASCADE;
