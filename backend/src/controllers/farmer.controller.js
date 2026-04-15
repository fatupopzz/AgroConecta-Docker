/**
 * @file controllers/farmer.controller.js
 * @description Controladores para el módulo de agricultores.
 */

const { pool } = require("../config/db");

/**
 * POST /api/farmers/profile
 *
 * Crea o actualiza el perfil del agricultor autenticado.
 * Usa un upsert — si el agricultor ya tiene perfil lo actualiza,
 * si no existe lo crea.
 *
 * Solo el agricultor autenticado puede modificar su propio perfil.
 * El id_usuario se extrae del token JWT, no del body.
 *
 * @route   POST /api/farmers/profile
 * @access  Privado (agricultor autenticado)
 *
 * @body {string}  [departamento]         - Departamento donde opera (opcional)
 * @body {string}  [municipio]            - Municipio donde opera (opcional)
 * @body {string}  [tipo_agricultor]      - Escala: pequena_escala | mediana_escala | industrial
 * @body {number}  [tamano_terreno_ha]    - Tamaño del terreno en hectáreas (opcional)
 * @body {string}  [cultivos_principales] - Lista de cultivos principales (opcional)
 *
 * @returns {201} Perfil creado exitosamente
 * @returns {200} Perfil actualizado exitosamente
 * @returns {403} El usuario no es agricultor
 * @returns {500} Error interno del servidor
 */
const upsertFarmerProfile = async (req, res) => {
  const { id, tipo } = req.user;

  if (tipo !== "agricultor") {
    return res.status(403).json({
      error: "Solo los agricultores pueden crear o editar este perfil",
    });
  }

  const {
    departamento,
    municipio,
    tipo_agricultor,
    tamano_terreno_ha,
    cultivos_principales,
  } = req.body;

  const tiposValidos = ["pequena_escala", "mediana_escala", "industrial"];
  if (tipo_agricultor && !tiposValidos.includes(tipo_agricultor)) {
    return res.status(400).json({
      error: `tipo_agricultor inválido. Use: ${tiposValidos.join(" | ")}`,
    });
  }

  if (tamano_terreno_ha !== undefined) {
    const val = Number(tamano_terreno_ha);
    if (!Number.isFinite(val) || val < 0) {
      return res.status(400).json({ error: "tamano_terreno_ha inválido" });
    }
  }

  try {
    const result = await pool.query(
      `INSERT INTO agricultor (id_usuario, departamento, municipio, tipo_agricultor, tamano_terreno_ha, cultivos_principales)
       VALUES ($1, $2, $3, $4, $5, $6)
       ON CONFLICT (id_usuario) DO UPDATE
         SET departamento         = COALESCE(EXCLUDED.departamento, agricultor.departamento),
             municipio            = COALESCE(EXCLUDED.municipio, agricultor.municipio),
             tipo_agricultor      = COALESCE(EXCLUDED.tipo_agricultor, agricultor.tipo_agricultor),
             tamano_terreno_ha    = COALESCE(EXCLUDED.tamano_terreno_ha, agricultor.tamano_terreno_ha),
             cultivos_principales = COALESCE(EXCLUDED.cultivos_principales, agricultor.cultivos_principales)
       RETURNING id_agricultor, id_usuario, departamento, municipio,
                 tipo_agricultor, tamano_terreno_ha, cultivos_principales, tiene_membresia`,
      [
        id,
        departamento || null,
        municipio || null,
        tipo_agricultor || null,
        tamano_terreno_ha !== undefined ? Number(tamano_terreno_ha) : null,
        cultivos_principales || null,
      ]
    );

    const perfil = result.rows[0];

    return res.status(201).json({
      message: "Perfil guardado exitosamente.",
      perfil,
    });
  } catch (error) {
    console.error("Error en upsertFarmerProfile:", error);
    return res.status(500).json({ error: "Error interno del servidor" });
  }
};

/**
 * GET /api/farmers/profile/:id
 *
 * Retorna el perfil de un agricultor por su id_agricultor.
 * Solo el agricultor autenticado puede ver su propio perfil.
 * El id del token se compara contra el id_usuario del perfil encontrado.
 *
 * @route   GET /api/farmers/profile/:id
 * @access  Privado (agricultor autenticado)
 *
 * @param  {number} id - ID del agricultor (id_agricultor)
 *
 * @returns {200} Perfil del agricultor
 * @returns {400} ID inválido
 * @returns {403} No autorizado para ver este perfil
 * @returns {404} Perfil no encontrado
 * @returns {500} Error interno del servidor
 */
const getFarmerProfile = async (req, res) => {
  const { id: userId, tipo } = req.user;

  if (tipo !== "agricultor") {
    return res.status(403).json({
      error: "Solo los agricultores pueden acceder a este perfil",
    });
  }

  if (!/^[1-9]\d*$/.test(req.params.id)) {
    return res.status(400).json({ error: "ID de agricultor inválido" });
  }

  const id = Number(req.params.id);

  try {
    const result = await pool.query(
      `SELECT
         a.id_agricultor,
         a.departamento,
         a.municipio,
         a.tipo_agricultor,
         a.tamano_terreno_ha,
         a.cultivos_principales,
         a.tiene_membresia,
         u.nombre,
         u.telefono,
         u.email,
         u.fecha_registro
       FROM agricultor a
       JOIN usuario u ON u.id_usuario = a.id_usuario
       WHERE a.id_agricultor = $1`,
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Perfil no encontrado" });
    }

    const ownerCheck = await pool.query(
      `SELECT id_usuario FROM agricultor WHERE id_agricultor = $1`,
      [id]
    );

    if (ownerCheck.rows[0].id_usuario !== userId) {
      return res.status(403).json({
        error: "No autorizado para ver este perfil",
      });
    }

    return res.status(200).json({ perfil: result.rows[0] });
  } catch (error) {
    console.error("Error en getFarmerProfile:", error);
    return res.status(500).json({ error: "Error interno del servidor" });
  }
};

module.exports = { upsertFarmerProfile, getFarmerProfile };