const bcrypt = require("bcrypt");
const pool = require("../db/connection");

const register = async (req, res) => {
  try {
    const { nombre, telefono, email, password } = req.body;

    // Validación básica
    if (!nombre || !telefono || !email || !password) {
      return res.status(400).json({ error: "Datos incompletos" });
    }

    // Verificar si ya existe
    const userExist = await pool.query(
      "SELECT * FROM usuario WHERE telefono = $1 OR email = $2",
      [telefono, email]
    );

    if (userExist.rows.length > 0) {
      return res.status(400).json({ error: "El usuario ya existe" });
    }

    // Hash contraseña
    const hash = await bcrypt.hash(password, 10);

    // Insertar usuario
    const result = await pool.query(
      `INSERT INTO usuario (nombre, telefono, email, contrasena_hash, tipo_usuario)
       VALUES ($1, $2, $3, $4, 'agricultor')
       RETURNING *`,
      [nombre, telefono, email, hash]
    );

    res.status(201).json({
      message: "Usuario creado",
      user: result.rows[0],
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Error en servidor" });
  }
};

module.exports = { register };