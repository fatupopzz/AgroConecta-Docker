const config = require("../config/weather");

const HOUR_MS = 3600000;
const numeric = (value, min = -Infinity, max = Infinity) =>
  typeof value === "number" && Number.isFinite(value) && value >= min && value <= max;
const iso = (seconds) => new Date(seconds * 1000).toISOString();

// UTC y epoch evitan interpretar fechas del proveedor en la zona del servidor.
const normalizeForecast = (data, coordinates, now) => {
  const current = data?.current;
  const hourly = data?.hourly;
  const start = Math.floor(now.getTime() / HOUR_MS) * 3600;
  if (!numeric(current?.time) || Math.abs(current.time * 1000 - now.getTime()) > HOUR_MS ||
      !numeric(current.temperature_2m) || !numeric(current.wind_speed_10m, 0) ||
      !numeric(current.precipitation, 0) || !numeric(current.weather_code, 0) ||
      !Array.isArray(hourly?.time)) throw new Error("Invalid weather response");

  const rows = Array.from({ length: 24 }, (_, offset) => {
    const time = start + (offset + 1) * 3600;
    const index = hourly.time.indexOf(time);
    const temperature = hourly.temperature_2m?.[index];
    const rain = hourly.precipitation?.[index];
    const probability = hourly.precipitation_probability?.[index];
    const wind = hourly.wind_speed_10m?.[index];
    if (index < 0 || !numeric(temperature) || !numeric(rain, 0) ||
        !numeric(probability, 0, 100) || !numeric(wind, 0)) throw new Error("Incomplete weather response");
    return { temperature, rain, probability, wind };
  });

  return {
    coordinates,
    current: {
      temperature_c: current.temperature_2m,
      wind_speed_kmh: current.wind_speed_10m,
      precipitation_mm: current.precipitation,
      weather_code: current.weather_code,
    },
    forecast_24h: {
      from: iso(start), to: iso(start + 24 * 3600),
      precipitation_mm: Number(rows.reduce((sum, row) => sum + row.rain, 0).toFixed(3)),
      precipitation_probability_max: Math.max(...rows.map(row => row.probability)),
      wind_speed_max_kmh: Math.max(...rows.map(row => row.wind)),
      temperature_min_c: Math.min(...rows.map(row => row.temperature)),
      temperature_max_c: Math.max(...rows.map(row => row.temperature)),
    },
    data_time: iso(current.time),
    fetched_at: now.toISOString(),
    timezone: "UTC",
    source: "Open-Meteo",
  };
};

const createOpenMeteoService = ({
  fetchImpl = globalThis.fetch,
  baseUrl = process.env.OPEN_METEO_URL || "https://api.open-meteo.com/v1/forecast",
  timeoutMs = Number(process.env.OPEN_METEO_TIMEOUT_MS || config.timeoutMs),
  now = () => new Date(),
} = {}) => {
  if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) throw new Error("Invalid weather timeout");
  return {
    async getWeather(coordinates) {
      const url = new URL(baseUrl);
      Object.entries({
        latitude: coordinates.lat, longitude: coordinates.lng,
        current: "temperature_2m,precipitation,weather_code,wind_speed_10m",
        hourly: "temperature_2m,precipitation,precipitation_probability,wind_speed_10m",
        temperature_unit: "celsius", wind_speed_unit: "kmh", precipitation_unit: "mm",
        timezone: "GMT", timeformat: "unixtime", forecast_days: 3,
      }).forEach(([key, value]) => url.searchParams.set(key, value));
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), timeoutMs);
      try {
        const response = await fetchImpl(url, { signal: controller.signal });
        if (!response.ok) throw new Error("Weather provider unavailable");
        return normalizeForecast(await response.json(), coordinates, now());
      } finally {
        clearTimeout(timer);
      }
    },
  };
};

module.exports = { createOpenMeteoService, normalizeForecast };
