const express = require('express');
const app = express();
const PORT = 3000;

app.get('/', (req, res) => {
  res.send('<h1>AgroConecta Frontend</h1><p>Panel de pruebas corriendo correctamente.</p>');
});

app.listen(PORT, () => {
  console.log(`Frontend AgroConecta escuchando en puerto ${PORT}`);
});
