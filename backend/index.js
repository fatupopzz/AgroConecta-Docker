const express = require("express");
const { shutdownPool } = require("./src/config/db");

const productRoutes = require("./src/routes/productRoutes");
const categoryRoutes = require("./src/routes/categoryRoutes");
const authRoutes = require("./src/routes/authRoutes");
const cartRoutes = require("./src/routes/cart.routes");
const verifyToken = require("./src/middleware/authMiddleware");
const agricultorRoutes = require("./src/routes/agricultorRoutes");
const distribuidorRoutes = require("./src/routes/distribuidorRoutes");
const farmerRoutes = require("./src/routes/farmer.routes");
const userRoutes = require("./src/routes/userRoutes");
const orderRoutes = require("./src/routes/orderRoutes");
const adminRoutes = require("./src/routes/adminRoutes");
const qualityReportRoutes = require("./src/routes/qualityReportRoutes");
const paymentRoutes = require("./src/routes/paymentRoutes");
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


app.use("/api/payments", paymentRoutes);
app.use("/api/products", productRoutes);
app.use("/api/categories", categoryRoutes);
app.use("/api/agricultores", agricultorRoutes);
app.use("/api/distribuidores", distribuidorRoutes);
app.use("/api/usuarios", userRoutes);
app.use("/api/farmers", farmerRoutes);
app.use("/api/orders", orderRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api", qualityReportRoutes);
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
