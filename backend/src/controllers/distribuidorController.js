const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));


const getDistributors = async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT d.*, u.nombre, u.telefono, u.email
       FROM distribuidor d
       JOIN usuario u ON d.id_usuario = u.id_usuario
       WHERE d.estado_verificacion = 'verificado'
       ORDER BY d.nombre_negocio ASC`
    );

    res.json(result.rows);
  } catch (error) {
    console.error("Error en getDistributors:", error);
    res.status(500).json({ error: "Error al obtener distribuidores" });
  }
};


const getDistributorById = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      `SELECT d.*, u.nombre, u.telefono, u.email
       FROM distribuidor d
       JOIN usuario u ON d.id_usuario = u.id_usuario
       WHERE d.id_distribuidor = $1`,
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    res.json(result.rows[0]);
  } catch (error) {
    console.error("Error en getDistributorById:", error);
    res.status(500).json({ error: "Error al obtener distribuidor" });
  }
};


const createDistributor = async (req, res) => {
  const { id_usuario, nombre_negocio, nit, departamento } = req.body;

  if (!id_usuario || !nombre_negocio) {
    return res.status(400).json({
      error: "id_usuario y nombre_negocio son obligatorios",
    });
  }

  if (!isPositiveInteger(id_usuario)) {
    return res.status(400).json({ error: "id_usuario inválido" });
  }

  if (typeof nombre_negocio !== "string" || nombre_negocio.trim().length < 2) {
    return res.status(400).json({ error: "Nombre de negocio inválido" });
  }

  try {
    // validar usuario
    const user = await pool.query(
      "SELECT id_usuario FROM usuario WHERE id_usuario = $1",
      [id_usuario]
    );

    if (user.rows.length === 0) {
      return res.status(404).json({ error: "Usuario no existe" });
    }

    const result = await pool.query(
      `INSERT INTO distribuidor
       (id_usuario, nombre_negocio, nit, departamento)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [
        id_usuario,
        nombre_negocio.trim(),
        nit || null,
        departamento || null,
      ]
    );

    res.status(201).json({
      message: "Distribuidor creado correctamente",
      distribuidor: result.rows[0],
    });

  } catch (error) {
    console.error("Error en createDistributor:", error);
    res.status(500).json({ error: "Error al crear distribuidor" });
  }
};

const updateDistributor = async (req, res) => {
  const { id } = req.params;
  const { nombre_negocio, nit, departamento } = req.body;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      `UPDATE distribuidor SET
        nombre_negocio = COALESCE($2, nombre_negocio),
        nit = COALESCE($3, nit),
        departamento = COALESCE($4, departamento)
       WHERE id_distribuidor = $1
       RETURNING *`,
      [id, nombre_negocio, nit, departamento]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    res.json({
      message: "Distribuidor actualizado",
      distribuidor: result.rows[0],
    });

  } catch (error) {
    console.error("Error en updateDistributor:", error);
    res.status(500).json({ error: "Error al actualizar distribuidor" });
  }
};


const deleteDistributor = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      "DELETE FROM distribuidor WHERE id_distribuidor = $1 RETURNING *",
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    res.json({ message: "Distribuidor eliminado" });

  } catch (error) {
    console.error("Error en deleteDistributor:", error);
    res.status(500).json({ error: "Error al eliminar distribuidor" });
  }
};

module.exports = {
  getDistributors,
  getDistributorById,
  createDistributor,
  updateDistributor,
  deleteDistributor,
};