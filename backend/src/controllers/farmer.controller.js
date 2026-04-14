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
         SET departamento        = COALESCE(EXCLUDED.departamento, agricultor.departamento),
             municipio           = COALESCE(EXCLUDED.municipio, agricultor.municipio),
             tipo_agricultor     = COALESCE(EXCLUDED.tipo_agricultor, agricultor.tipo_agricultor),
             tamano_terreno_ha   = COALESCE(EXCLUDED.tamano_terreno_ha, agricultor.tamano_terreno_ha),
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
    const isNew = perfil.id_agricultor !== undefined;

    return res.status(isNew ? 201 : 200).json({
      message: "Perfil guardado exitosamente.",
      perfil,
    });
  } catch (error) {
    console.error("Error en upsertFarmerProfile:", error);
    return res.status(500).json({ error: "Error interno del servidor" });
  }
};

module.exports = { upsertFarmerProfile };