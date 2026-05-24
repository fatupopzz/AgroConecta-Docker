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

const getDistributorRating = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const distributorExists = await pool.query(
      "SELECT id_distribuidor FROM distribuidor WHERE id_distribuidor = $1",
      [Number(id)]
    );

    if (distributorExists.rows.length === 0) {
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    const result = await pool.query(
      `SELECT
         COALESCE(ROUND(AVG(r.calificacion)::numeric, 1), 0) AS calificacion_promedio,
         COUNT(r.id_resena)::int AS total_resenas,
         COUNT(r.id_resena) FILTER (WHERE r.calificacion = 5)::int AS cinco_estrellas,
         COUNT(r.id_resena) FILTER (WHERE r.calificacion = 4)::int AS cuatro_estrellas,
         COUNT(r.id_resena) FILTER (WHERE r.calificacion = 3)::int AS tres_estrellas,
         COUNT(r.id_resena) FILTER (WHERE r.calificacion = 2)::int AS dos_estrellas,
         COUNT(r.id_resena) FILTER (WHERE r.calificacion = 1)::int AS una_estrella
       FROM producto p
       LEFT JOIN resena r ON r.id_producto = p.id_producto
       WHERE p.id_distribuidor = $1`,
      [Number(id)]
    );

    await pool.query(
      `UPDATE distribuidor
       SET calificacion_promedio = $2
       WHERE id_distribuidor = $1`,
      [Number(id), result.rows[0].calificacion_promedio]
    );

    return res.json({
      id_distribuidor: Number(id),
      calificacion_promedio: Number(result.rows[0].calificacion_promedio),
      total_resenas: Number(result.rows[0].total_resenas),
      distribucion: {
        5: Number(result.rows[0].cinco_estrellas),
        4: Number(result.rows[0].cuatro_estrellas),
        3: Number(result.rows[0].tres_estrellas),
        2: Number(result.rows[0].dos_estrellas),
        1: Number(result.rows[0].una_estrella),
      },
    });
  } catch (error) {
    console.error("Error en getDistributorRating:", error);
    return res.status(500).json({ error: "Error al obtener rating del distribuidor" });
  }
};

const getDistributorReviews = async (req, res) => {
  const { id } = req.params;
  const page = Number(req.query.page) || 1;
  const limit = Number(req.query.limit) || 10;
  const offset = (page - 1) * limit;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  if (!Number.isInteger(page) || page < 1 || !Number.isInteger(limit) || limit < 1 || limit > 50) {
    return res.status(400).json({ error: "Parámetros de paginación inválidos" });
  }

  try {
    const distributorExists = await pool.query(
      "SELECT id_distribuidor FROM distribuidor WHERE id_distribuidor = $1",
      [Number(id)]
    );

    if (distributorExists.rows.length === 0) {
      return res.status(404).json({ error: "Distribuidor no encontrado" });
    }

    const reviewsResult = await pool.query(
      `SELECT r.id_resena,
              r.calificacion,
              r.comentario,
              r.fecha_resena,
              p.id_producto,
              p.nombre AS producto_nombre,
              u.nombre AS agricultor_nombre
       FROM resena r
       JOIN producto p ON r.id_producto = p.id_producto
       JOIN agricultor a ON r.id_agricultor = a.id_agricultor
       JOIN usuario u ON a.id_usuario = u.id_usuario
       WHERE p.id_distribuidor = $1
       ORDER BY r.fecha_resena DESC
       LIMIT $2 OFFSET $3`,
      [Number(id), limit, offset]
    );

    const countResult = await pool.query(
      `SELECT COUNT(r.id_resena)::int AS total
       FROM resena r
       JOIN producto p ON r.id_producto = p.id_producto
       WHERE p.id_distribuidor = $1`,
      [Number(id)]
    );

    const total = Number(countResult.rows[0].total);

    return res.json({
      page,
      limit,
      total,
      total_pages: Math.ceil(total / limit),
      reviews: reviewsResult.rows,
    });
  } catch (error) {
    console.error("Error en getDistributorReviews:", error);
    return res.status(500).json({ error: "Error al obtener reseñas del distribuidor" });
  }
};

module.exports = {
  getDistributors,
  getDistributorById,
  createDistributor,
  updateDistributor,
  deleteDistributor,
  getDistributorRating,
  getDistributorReviews,
};