const express = require("express");
const { shutdownPool } = require("./src/config/db");
const productRoutes = require("./src/routes/product.routes");
const categoryRoutes = require("./src/routes/category.routes");
const distributorRoutes = require("./src/routes/distributor.routes");
const adminRoutes = require("./src/routes/admin.routes");

const app = express();
const PORT = process.env.PORT || 8080;

app.use(express.json());
app.use("/api/admin", adminRoutes);

app.get("/", (req, res) => {
  res.json({ status: "AgroConecta Backend corriendo", version: "1.0.0" });
});

app.use("/api/products", productRoutes);
app.use("/api/categories", categoryRoutes);
app.use("/api/distributors", distributorRoutes);

app.listen(PORT, () => {
  console.log(`Backend AgroConecta escuchando en puerto ${PORT}`);
});

process.on("SIGTERM", async () => {
  await shutdownPool();
  process.exit(0);
});
