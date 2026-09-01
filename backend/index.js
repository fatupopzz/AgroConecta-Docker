require("dotenv").config();

const fs = require("node:fs/promises");
const path = require("node:path");
const app = require("./app");
const { shutdownPool, pool } = require("./src/config/db");

const PORT = process.env.PORT || 8080;

const runStartupMigrations = async () => {
  const statements = [
    `ALTER TABLE reporte_calidad
     ADD COLUMN IF NOT EXISTS fecha_resolucion TIMESTAMP`,
    `ALTER TABLE distribuidor
     ADD COLUMN IF NOT EXISTS direccion TEXT`,
    `ALTER TABLE pago
     ADD COLUMN IF NOT EXISTS estado_pago VARCHAR(20) DEFAULT 'pendiente'
       CHECK (estado_pago IN ('pendiente', 'processing', 'completado', 'failed'))`,
    `ALTER TABLE pago
     ADD COLUMN IF NOT EXISTS fecha_pago TIMESTAMP`,
    `ALTER TABLE pago
     ADD COLUMN IF NOT EXISTS referencia_transaccion VARCHAR(100)`,
    `ALTER TABLE pago
     ADD COLUMN IF NOT EXISTS proveedor VARCHAR(30)`,
    `ALTER TABLE pago
     ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW()`,
    `ALTER TABLE pago
     ADD COLUMN IF NOT EXISTS fecha_confirmacion TIMESTAMP`,
    `ALTER TABLE pedido
     DROP CONSTRAINT IF EXISTS pedido_estado_check`,
    `ALTER TABLE pedido
     ALTER COLUMN estado SET DEFAULT 'confirmado'`,
    `ALTER TABLE pedido
     ADD COLUMN IF NOT EXISTS es_urgente BOOLEAN NOT NULL DEFAULT FALSE`,
    `ALTER TABLE pedido
     ADD COLUMN IF NOT EXISTS tipo_plaga VARCHAR(100)`,
    `UPDATE pedido
     SET estado = 'confirmado'
     WHERE estado = 'pendiente'`,
    `UPDATE pedido
     SET estado = 'en_ruta'
     WHERE estado = 'en_camino'`,
    `ALTER TABLE pedido
     ADD CONSTRAINT pedido_estado_check
     CHECK (estado IN ('confirmado', 'preparando', 'en_ruta', 'entregado', 'cancelado'))`,
    `CREATE TABLE IF NOT EXISTS pedido_tracking (
       id_tracking SERIAL PRIMARY KEY,
       id_pedido INT NOT NULL REFERENCES pedido(id_pedido) ON DELETE CASCADE,
       estado VARCHAR(30) NOT NULL,
       "timestamp" TIMESTAMP DEFAULT NOW(),
       notas TEXT
     )`,
    `ALTER TABLE pedido_tracking
     DROP CONSTRAINT IF EXISTS pedido_tracking_estado_check`,
    `UPDATE pedido_tracking
     SET estado = 'confirmado'
     WHERE estado = 'pendiente'`,
    `UPDATE pedido_tracking
     SET estado = 'en_ruta'
     WHERE estado = 'en_camino'`,
    `ALTER TABLE pedido_tracking
     ADD CONSTRAINT pedido_tracking_estado_check
     CHECK (estado IN ('confirmado', 'preparando', 'en_ruta', 'entregado', 'cancelado'))`,
    `CREATE INDEX IF NOT EXISTS idx_pedido_tracking_id_pedido_timestamp
     ON pedido_tracking (id_pedido, "timestamp")`,
    `INSERT INTO pedido_tracking (id_pedido, estado, "timestamp", notas)
     SELECT p.id_pedido, p.estado, COALESCE(p.fecha_pedido, NOW()), 'Estado inicial del pedido'
     FROM pedido p
     WHERE NOT EXISTS (
       SELECT 1
       FROM pedido_tracking pt
       WHERE pt.id_pedido = p.id_pedido
     )`,
    `CREATE TABLE IF NOT EXISTS producto_seguido (
       id SERIAL PRIMARY KEY,
       id_agricultor INT NOT NULL REFERENCES agricultor(id_agricultor) ON DELETE CASCADE,
       id_producto INT NOT NULL REFERENCES producto(id_producto) ON DELETE CASCADE,
       precio_al_seguir DECIMAL(10,2) NOT NULL,
       fecha TIMESTAMP DEFAULT NOW(),
       UNIQUE (id_agricultor, id_producto)
     )`,
    `CREATE INDEX IF NOT EXISTS idx_producto_seguido_agricultor_producto
     ON producto_seguido (id_agricultor, id_producto)`,
    `CREATE INDEX IF NOT EXISTS idx_producto_seguido_producto
     ON producto_seguido (id_producto)`,
    `CREATE TABLE IF NOT EXISTS notificacion (
       id_notificacion SERIAL PRIMARY KEY,
       id_agricultor INT REFERENCES agricultor(id_agricultor) ON DELETE CASCADE,
       tipo VARCHAR(40),
       contenido JSONB DEFAULT '{}'::jsonb,
       leida BOOLEAN DEFAULT FALSE,
       fecha TIMESTAMP DEFAULT NOW()
     )`,
    `ALTER TABLE notificacion
     ADD COLUMN IF NOT EXISTS id_distribuidor INT
       REFERENCES distribuidor(id_distribuidor) ON DELETE CASCADE`,
    `ALTER TABLE notificacion
     ADD COLUMN IF NOT EXISTS id_pedido INT
       REFERENCES pedido(id_pedido) ON DELETE CASCADE`,
    `CREATE INDEX IF NOT EXISTS idx_notificacion_agricultor_fecha
     ON notificacion (id_agricultor, fecha DESC)`,
    `CREATE INDEX IF NOT EXISTS idx_notificacion_tipo
     ON notificacion (tipo)`,
    // Migracion de mensaje_asesoria
    `CREATE TABLE IF NOT EXISTS mensaje_asesoria (
       id_mensaje SERIAL PRIMARY KEY,
       id_pedido INT NOT NULL REFERENCES pedido(id_pedido) ON DELETE CASCADE,
       id_usuario_remitente INT NOT NULL
         REFERENCES usuario(id_usuario) ON DELETE CASCADE,
       mensaje TEXT NOT NULL
         CHECK (char_length(trim(mensaje)) BETWEEN 1 AND 1000),
       fecha_envio TIMESTAMP NOT NULL DEFAULT NOW()
     )`,
    `CREATE INDEX IF NOT EXISTS idx_mensaje_asesoria_pedido_fecha
     ON mensaje_asesoria (id_pedido, fecha_envio, id_mensaje)`,
  ];

  for (const statement of statements) {
    await pool.query(statement);
  }

  const cropCycleMigration = await fs.readFile(
    path.join(__dirname, "sql", "hu026_crop_cycles_migration.sql"),
    "utf8",
  );
  await pool.query(cropCycleMigration);
};

const startServer = async () => {
    try {
        await runStartupMigrations();

        app.listen(PORT, () => {
            console.log(`Backend AgroConecta escuchando en puerto ${PORT}`);
        });

    } catch (error) {
        console.error(error);
        process.exit(1);
    }
};

startServer();

process.on("SIGTERM", async () => {
    await shutdownPool();
    process.exit(0);
});
