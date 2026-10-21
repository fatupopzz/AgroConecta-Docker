const { createWeatherService, normalizeCoordinates } = require("../services/weatherService");
const AppError = require("../errors/AppError");
const { pool } = require("../config/db");

const createWeatherController = (service = createWeatherService()) => async (req, res) => {
  if (req.user?.tipo !== "agricultor") {
    throw new AppError(403, "El clima solo está disponible para agricultores");
  }
  const coordinates = normalizeCoordinates(req.query);
  return res.json(await service.getWeather(coordinates));
};

const createFarmWeatherController = (service = createWeatherService(), database = pool) => async (req, res) => {
  if (req.user?.tipo !== "agricultor") throw new AppError(403, "El clima solo está disponible para agricultores");
  const result = await database.query(
    `SELECT d.latitud, d.longitud FROM direccion_entrega d
     JOIN agricultor a ON a.id_agricultor = d.id_agricultor
     WHERE a.id_usuario = $1 AND d.es_predeterminada = TRUE
       AND d.latitud IS NOT NULL AND d.longitud IS NOT NULL
     ORDER BY d.id_direccion LIMIT 1`, [req.user.id],
  );
  if (!result.rows.length) throw new AppError(404, "La dirección principal no tiene coordenadas guardadas");
  const coordinates = normalizeCoordinates({ lat: String(result.rows[0].latitud), lng: String(result.rows[0].longitud) });
  return res.json(await service.getWeather(coordinates));
};

module.exports = { createWeatherController, createFarmWeatherController };
