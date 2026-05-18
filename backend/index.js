const express = require("express");
const { shutdownPool } = require("./src/config/db");

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

const app = express();
const PORT = process.env.PORT || 8080;

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
app.use("/api/orders", verifyToken, orderRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api", verifyToken, qualityReportRoutes);
app.use("/api/cart", verifyToken, cartRoutes);

app.get("/", (req, res) => {
  res.json({ status: "AgroConecta Backend corriendo", version: "1.0.0" });
});

app.listen(PORT, () => {
  console.log(`Backend AgroConecta escuchando en puerto ${PORT}`);
});

process.on("SIGTERM", async () => {
  await shutdownPool();
  process.exit(0);
});
