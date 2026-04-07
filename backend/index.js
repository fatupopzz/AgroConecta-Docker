const express = require('express');
const app = express();
const PORT = process.env.PORT || 8080;

app.use(express.json());

const distributorRoutes = require('./routes/distributors');
app.use('/api/distributors', distributorRoutes);

app.get('/', (req, res) => {
  res.json({ status: 'AgroConecta Backend corriendo', version: '1.0.0' });
});

app.listen(PORT, () => {
  console.log(`Backend AgroConecta escuchando en puerto ${PORT}`);
});