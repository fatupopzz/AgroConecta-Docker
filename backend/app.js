const express = require("express");
const { UPLOADS_DIR } = require("./src/config/uploads");

const productRoutes = require("./src/routes/productRoutes");
const productRecommendationRoutes = require("./src/routes/productRecommendationRoutes");
const categoryRoutes = require("./src/routes/categoryRoutes");
const authRoutes = require("./src/routes/authRoutes");
const cartRoutes = require("./src/routes/cartRoutes");
const verifyToken = require("./src/middleware/authMiddleware");
const { errorHandler, notFoundHandler } = require("./src/middleware/errorHandler");
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
const distribuidorReviewRoutes = require("./src/routes/distribuitorReviewRoutes");
const notificationRoutes = require("./src/routes/notificationRoutes");
const cropCycleRoutes = require("./src/routes/cropCycleRoutes");
const pestAlertRoutes = require("./src/routes/pestAlertRoutes");
const favoriteRoutes = require("./src/routes/favoriteRoutes");

const weatherRoutes = require("./src/routes/weatherRoutes");
const plantingRoutes = require("./src/routes/plantingRoutes");

const app = express();

app.use(express.json());

// Sin token: las fotos son tan públicas como el catálogo y así la app puede
// cargarlas aunque no haya sesión. Cada subida genera un nombre nuevo, por lo
// que el contenido de una URL nunca cambia y se puede cachear como inmutable.
// nosniff evita que un navegador interprete como HTML algo servido aquí.
app.use(
  "/uploads",
  express.static(UPLOADS_DIR, {
    index: false,
    dotfiles: "ignore",
    maxAge: "7d",
    immutable: true,
    setHeaders: (res) => {
      res.set("X-Content-Type-Options", "nosniff");
    },
  }),
);

app.use("/api/auth", authRoutes);
app.use("/api/weather", weatherRoutes);

app.get("/api/protected", verifyToken, (req, res) => {
    res.json({
        message: "Ruta protegida",
        user: req.user,
    });
});

app.use("/api/payments", verifyToken, paymentRoutes);
app.use("/api/products/:id/reviews", resenaRoutes);
app.use("/api/products", productRoutes);
app.use("/api/productos", productRecommendationRoutes);
app.use("/api/categories", categoryRoutes);
app.use("/api/ciclos", cropCycleRoutes);
app.use("/api/agricultores", verifyToken, agricultorRoutes);
app.use("/api/distribuidores", verifyToken, distribuidorRoutes);
app.use("/api/usuarios", verifyToken, userRoutes);
app.use("/api/users", verifyToken, userRoutes);
app.use("/api/farmers", farmerRoutes);
app.use("/api/orders", orderRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api", verifyToken, qualityReportRoutes);
app.use("/api/cart", verifyToken, cartRoutes);
app.use("/api/inventory", inventoryRoutes);
app.use("/api/notifications", notificationRoutes);
app.use("/api/distribuidoresReview", distribuidorReviewRoutes);
app.use("/api/alerts/pests", verifyToken, pestAlertRoutes);
app.use("/api/favorites", verifyToken, favoriteRoutes);
app.use("/api/plantings", verifyToken, plantingRoutes);

app.get("/", (req, res) => {
    res.json({
        status: "AgroConecta Backend corriendo",
        version: "1.0.0",
    });
});

// Deben ir después de todas las rutas.
app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
