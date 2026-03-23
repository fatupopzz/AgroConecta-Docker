CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario    SERIAL PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
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

CREATE TABLE IF NOT EXISTS inventario_distribuidor (
    id_inventario       SERIAL PRIMARY KEY,
    id_distribuidor     INT NOT NULL REFERENCES distribuidor(id_distribuidor),
    id_producto         INT NOT NULL REFERENCES producto(id_producto),
    precio              DECIMAL(10,2) NOT NULL,
    stock_disponible    INT DEFAULT 0,
    unidad_medida       VARCHAR(30),
    ultima_actualizacion TIMESTAMP DEFAULT NOW(),
    UNIQUE (id_distribuidor, id_producto)
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
    estado              VARCHAR(30) DEFAULT 'pendiente' CHECK (estado IN ('pendiente', 'confirmado', 'en_camino', 'entregado', 'cancelado')),
    tipo_entrega        VARCHAR(20) CHECK (tipo_entrega IN ('domicilio', 'punto_recogida')),
    direccion_entrega   TEXT,
    es_urgente          BOOLEAN DEFAULT FALSE,
    total_pedido        DECIMAL(10,2),
    costo_envio         DECIMAL(10,2) DEFAULT 0.0,
    notas               TEXT
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
    estado_pago         VARCHAR(20) DEFAULT 'pendiente' CHECK (estado_pago IN ('pendiente', 'completado', 'fallido', 'reembolsado')),
    fecha_pago          TIMESTAMP,
    referencia_transaccion VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS resena (
    id_resena       SERIAL PRIMARY KEY,
    id_agricultor   INT NOT NULL REFERENCES agricultor(id_agricultor),
    id_producto     INT NOT NULL REFERENCES producto(id_producto),
    id_pedido       INT NOT NULL REFERENCES pedido(id_pedido),
    calificacion    INT NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario      TEXT,
    fecha_resena    TIMESTAMP DEFAULT NOW(),
    UNIQUE (id_agricultor, id_producto, id_pedido)
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

INSERT INTO categoria (nombre, descripcion) VALUES
    ('Fertilizantes', 'Productos para nutrición del suelo y cultivos'),
    ('Pesticidas', 'Control de plagas e insectos'),
    ('Herbicidas', 'Control de malezas'),
    ('Semillas', 'Semillas certificadas para siembra'),
    ('Herramientas', 'Equipos y herramientas agrícolas')
ON CONFLICT DO NOTHING;

INSERT INTO usuario (nombre, telefono, email, contrasena_hash, tipo_usuario) VALUES
    ('Admin AgroConecta', '50200000000', 'admin@agroconecta.gt',
     crypt('admin123', gen_salt('bf', 10)), 'administrador')
ON CONFLICT DO NOTHING;
