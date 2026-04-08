const express = require('express');
const app = express();
const PORT = process.env.PORT || 8080;

const verifyToken = require("./src/middleware/authMiddleware");

app.use(express.json());
 
// Rutas
const authRoutes = require("./src/routes/authRoutes");
app.use("/api/auth", authRoutes);

app.get("/api/protected", verifyToken, (req, res) => {
  res.json({
    message: "Ruta protegida",
    user: req.user,
  });
});


// app.get("/health", (req, res) => {
//   res.send("OK");
// });

// app.get("/test-db", async (req, res) => {
//   try {
//     const result = await pool.query("SELECT NOW()");
//     res.json(result.rows);
//   } catch (err) {
//     console.error("ERROR DB:", err.message);
//     res.status(500).send(err.message);
//   }
// });


app.listen(PORT, () => {
  console.log(`Backend AgroConecta escuchando en puerto ${PORT}`);
});
