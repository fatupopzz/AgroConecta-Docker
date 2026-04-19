const { pool } = require("../config/db");
const bcrypt = require("bcrypt");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));


const getUsers = async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro
       FROM usuario`
    );

    res.json(result.rows);
  } catch (error) {
    console.error("Error en getUsers:", error);
    res.status(500).json({ error: "Error al obtener usuarios" });
  }
};


const getUserById = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      `SELECT id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro
       FROM usuario
       WHERE id_usuario = $1`,
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Usuario no encontrado" });
    }

    res.json(result.rows[0]);
  } catch (error) {
    console.error("Error en getUserById:", error);
    res.status(500).json({ error: "Error al obtener usuario" });
  }
};


const createUser = async (req, res) => {
  const { nombre, telefono, email, password, tipo_usuario } = req.body;

  if (!nombre || !telefono || !password || !tipo_usuario) {
    return res.status(400).json({
      error: "Campos obligatorios: nombre, telefono, password, tipo_usuario",
    });
  }

  const tiposValidos = ["agricultor", "distribuidor", "administrador"];

  if (!tiposValidos.includes(tipo_usuario)) {
    return res.status(400).json({ error: "tipo_usuario inválido" });
  }

  try {
    const hash = await bcrypt.hash(password, 10);

    const result = await pool.query(
      `INSERT INTO usuario
       (nombre, telefono, email, contrasena_hash, tipo_usuario)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro`,
      [
        nombre.trim(),
        telefono,
        email || null,
        hash,
        tipo_usuario,
      ]
    );

    res.status(201).json({
      message: "Usuario creado correctamente",
      usuario: result.rows[0],
    });

  } catch (error) {
    console.error("Error en createUser:", error);
    res.status(500).json({ error: "Error al crear usuario" });
  }
};


const updateUser = async (req, res) => {
  const { id } = req.params;
  const { nombre, telefono, email, activo } = req.body;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      `UPDATE usuario SET
        nombre = COALESCE($2, nombre),
        telefono = COALESCE($3, telefono),
        email = COALESCE($4, email),
        activo = COALESCE($5, activo)
       WHERE id_usuario = $1
       RETURNING id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro`,
      [id, nombre, telefono, email, activo]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Usuario no encontrado" });
    }

    res.json({
      message: "Usuario actualizado",
      usuario: result.rows[0],
    });

  } catch (error) {
    console.error("Error en updateUser:", error);
    res.status(500).json({ error: "Error al actualizar usuario" });
  }
};


const deleteUser = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      "DELETE FROM usuario WHERE id_usuario = $1 RETURNING *",
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Usuario no encontrado" });
    }

    res.json({ message: "Usuario eliminado" });

  } catch (error) {
    console.error("Error en deleteUser:", error);
    res.status(500).json({ error: "Error al eliminar usuario" });
  }
};

module.exports = {
  getUsers,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
};