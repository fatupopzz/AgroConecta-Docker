const express = require("express");
const { shutdownPool } = require("./src/config/db");

const app = express();
const PORT = process.env.PORT || 8080;

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
