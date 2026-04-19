const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));


const getAgricultores = async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT a.*, u.nombre, u.telefono, u.email
       FROM agricultor a
       JOIN usuario u ON a.id_usuario = u.id_usuario`
    );

    res.json(result.rows);
  } catch (error) {
    console.error("Error en getAgricultores:", error);
    res.status(500).json({ error: "Error al obtener agricultores" });
  }
};


const getAgricultorById = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      `SELECT a.*, u.nombre, u.telefono, u.email
       FROM agricultor a
       JOIN usuario u ON a.id_usuario = u.id_usuario
       WHERE a.id_agricultor = $1`,
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Agricultor no encontrado" });
    }

    res.json(result.rows[0]);
  } catch (error) {
    console.error("Error en getAgricultorById:", error);
    res.status(500).json({ error: "Error al obtener agricultor" });
  }
};


const createAgricultor = async (req, res) => {
  const {
    id_usuario,
    departamento,
    municipio,
    tipo_agricultor,
    tamano_terreno_ha,
    cultivos_principales,
    tiene_membresia
  } = req.body;

  if (!id_usuario) {
    return res.status(400).json({ error: "id_usuario es obligatorio" });
  }

  if (!isPositiveInteger(id_usuario)) {
    return res.status(400).json({ error: "id_usuario inválido" });
  }

  const tiposValidos = ["pequena_escala", "mediana_escala", "industrial"];

  if (tipo_agricultor && !tiposValidos.includes(tipo_agricultor)) {
    return res.status(400).json({ error: "tipo_agricultor inválido" });
  }

  try {
    // verificar que usuario existe
    const user = await pool.query(
      "SELECT id_usuario FROM usuario WHERE id_usuario = $1",
      [id_usuario]
    );

    if (user.rows.length === 0) {
      return res.status(404).json({ error: "Usuario no existe" });
    }

    const result = await pool.query(
      `INSERT INTO agricultor
       (id_usuario, departamento, municipio, tipo_agricultor, tamano_terreno_ha, cultivos_principales, tiene_membresia)
       VALUES ($1, $2, $3, $4, $5, $6, COALESCE($7, false))
       RETURNING *`,
      [
        id_usuario,
        departamento || null,
        municipio || null,
        tipo_agricultor || null,
        tamano_terreno_ha || null,
        cultivos_principales || null,
        tiene_membresia
      ]
    );

    res.status(201).json({
      message: "Agricultor creado correctamente",
      agricultor: result.rows[0]
    });

  } catch (error) {
    console.error("Error en createAgricultor:", error);
    res.status(500).json({ error: "Error al crear agricultor" });
  }
};


const updateAgricultor = async (req, res) => {
  const { id } = req.params;
  const {
    departamento,
    municipio,
    tipo_agricultor,
    tamano_terreno_ha,
    cultivos_principales,
    tiene_membresia
  } = req.body;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  const tiposValidos = ["pequena_escala", "mediana_escala", "industrial"];

  if (tipo_agricultor && !tiposValidos.includes(tipo_agricultor)) {
    return res.status(400).json({ error: "tipo_agricultor inválido" });
  }

  try {
    const result = await pool.query(
      `UPDATE agricultor SET
        departamento = COALESCE($2, departamento),
        municipio = COALESCE($3, municipio),
        tipo_agricultor = COALESCE($4, tipo_agricultor),
        tamano_terreno_ha = COALESCE($5, tamano_terreno_ha),
        cultivos_principales = COALESCE($6, cultivos_principales),
        tiene_membresia = COALESCE($7, tiene_membresia)
       WHERE id_agricultor = $1
       RETURNING *`,
      [
        id,
        departamento,
        municipio,
        tipo_agricultor,
        tamano_terreno_ha,
        cultivos_principales,
        tiene_membresia
      ]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Agricultor no encontrado" });
    }

    res.json({
      message: "Agricultor actualizado",
      agricultor: result.rows[0]
    });

  } catch (error) {
    console.error("Error en updateAgricultor:", error);
    res.status(500).json({ error: "Error al actualizar agricultor" });
  }
};


const deleteAgricultor = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      "DELETE FROM agricultor WHERE id_agricultor = $1 RETURNING *",
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Agricultor no encontrado" });
    }

    res.json({ message: "Agricultor eliminado" });

  } catch (error) {
    console.error("Error en deleteAgricultor:", error);
    res.status(500).json({ error: "Error al eliminar agricultor" });
  }
};

module.exports = {
  getAgricultores,
  getAgricultorById,
  createAgricultor,
  updateAgricultor,
  deleteAgricultor
};