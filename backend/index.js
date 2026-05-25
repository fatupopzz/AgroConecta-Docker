const express = require("express");
const { shutdownPool } = require("./src/config/db");
const { pool } = require("./src/config/db");

const productRoutes = require("./src/routes/productRoutes");
const categoryRoutes = require("./src/routes/categoryRoutes");
const authRoutes = require("./src/routes/authRoutes");
const cartRoutes = require("./src/routes/cartRoutes");
const verifyToken = require("./src/middleware/authMiddleware");
const agricultorRoutes = require("./src/routes/agricultorRoutes");
const distribuidorRoutes = require("./src/routes/distribuidorRoutes");
const farmerRoutes = require("./src/routes/farmerRoutes");
const userRoutes = require("./src/routes/userRoutes");
const orderRoutes = require("./src/routes/orderRoutes");
const adminRoutes = require("./src/routes/adminRoutes");
const qualityReportRoutes = require("./src/routes/qualityReportRoutes");
const paymentRoutes = require("./src/routes/paymentRoutes");
const resenaRoutes = require("./src/routes/resenaRoutes");
const inventoryRoutes = require("./src/routes/inventoryRoutes");

const app = express();
const PORT = process.env.PORT || 8080;

const runStartupMigrations = async () => {
  const statements = [
    `ALTER TABLE reporte_calidad
     ADD COLUMN IF NOT EXISTS fecha_resolucion TIMESTAMP`,
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
  ];

  for (const statement of statements) {
    await pool.query(statement);
  }
};

app.use(express.json());

app.use("/api/auth", authRoutes);

app.get("/api/protected", verifyToken, (req, res) => {
  res.json({
    message: "Ruta protegida",
    user: req.user,
  });
});

app.use("/api/payments",verifyToken, paymentRoutes);
app.use("/api/products/:id/reviews", resenaRoutes);
app.use("/api/products", productRoutes);
app.use("/api/categories", categoryRoutes);
app.use("/api/agricultores", verifyToken, agricultorRoutes);
app.use("/api/distribuidores",verifyToken, distribuidorRoutes);
app.use("/api/usuarios",verifyToken, userRoutes);
app.use("/api/farmers", farmerRoutes);
app.use("/api/orders", orderRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api", verifyToken, qualityReportRoutes);
app.use("/api/cart", verifyToken, cartRoutes);
app.use("/api/inventory", inventoryRoutes);

app.get("/", (req, res) => {
  res.json({ status: "AgroConecta Backend corriendo", version: "1.0.0" });
});

const startServer = async () => {
  try {
    await runStartupMigrations();
    app.listen(PORT, () => {
      console.log(`Backend AgroConecta escuchando en puerto ${PORT}`);
    });
  } catch (error) {
    console.error("Error al iniciar el backend:", error);
    process.exit(1);
  }
};

startServer();

process.on("SIGTERM", async () => {
  await shutdownPool();
  process.exit(0);
});
