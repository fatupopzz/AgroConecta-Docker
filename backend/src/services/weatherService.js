const cacheService = require("./cacheService");
const { createOpenMeteoService } = require("./openMeteoService");
const { getRecommendations } = require("./weatherRules");
const { freshTtlSeconds, lastTtlSeconds } = require("../config/weather");
const AppError = require("../errors/AppError");

const normalizeCoordinates = ({ lat, lng }) => {
  const parse = (value, limit) => {
    if (typeof value !== "string" || !/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(value.trim())) {
      throw new AppError(400, "lat y lng deben ser coordenadas numéricas válidas");
    }
    const number = Number(value);
    if (!Number.isFinite(number) || Math.abs(number) > limit) {
      throw new AppError(400, "lat debe estar entre -90 y 90; lng entre -180 y 180");
    }
    return Number(number.toFixed(2)) || 0;
  };
  return { lat: parse(lat, 90), lng: parse(lng, 180) };
};

const createWeatherService = ({ cache = cacheService, provider = createOpenMeteoService(), logger = console } = {}) => {
  const read = async (namespace, key) => {
    try { return await cache.get(namespace, key); }
    catch { return { available: false, hit: false }; }
  };
  const write = async (entry, data, ttl) => {
    try { return await cache.set(entry.key, data, ttl); }
    catch { return false; }
  };
  return {
    async getWeather(coordinates) {
      const key = `${coordinates.lat.toFixed(2)}:${coordinates.lng.toFixed(2)}`;
      const fresh = await read("weather:fresh", key);
      if (fresh.hit) return { ...fresh.value, cached: true, stale: false };
      const last = await read("weather:last", key);
      let data;
      try {
        data = await provider.getWeather(coordinates);
      } catch {
        if (last.hit) return { ...last.value, cached: true, stale: true };
        throw new AppError(503, "El clima no está disponible temporalmente. Intenta de nuevo más tarde.");
      }
      data = { ...data, recommendations: getRecommendations(data.current, data.forecast_24h) };
      const saved = await Promise.all([
        write(fresh, data, freshTtlSeconds), write(last, data, lastTtlSeconds),
      ]);
      if (saved.some(result => !result)) logger.warn("No se pudo cachear el clima en Redis.");
      return { ...data, cached: false, stale: false };
    },
  };
};

module.exports = { createWeatherService, normalizeCoordinates };
