const { createWeatherService, normalizeCoordinates } = require("../services/weatherService");
const AppError = require("../errors/AppError");

const createWeatherController = (service = createWeatherService()) => async (req, res) => {
  if (req.user?.tipo !== "agricultor") {
    throw new AppError(403, "El clima solo está disponible para agricultores");
  }
  const coordinates = normalizeCoordinates(req.query);
  return res.json(await service.getWeather(coordinates));
};

module.exports = { createWeatherController };
