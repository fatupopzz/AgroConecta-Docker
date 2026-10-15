const { thresholds } = require("../config/weather");

const getRecommendations = (current, forecast, limits = thresholds) => {
  const recommendations = [];
  if (forecast.precipitation_mm >= limits.rainMm || forecast.precipitation_probability_max >= limits.rainProbability) {
    recommendations.push({ code: "RAIN", message: "Lluvia prevista: no aplicar herbicida ni fertilizante foliar." });
  }
  if (Math.max(current.wind_speed_kmh, forecast.wind_speed_max_kmh) >= limits.windKmh) {
    recommendations.push({ code: "STRONG_WIND", message: "Viento fuerte: no fumigar." });
  }
  if (Math.min(current.temperature_c, forecast.temperature_min_c) <= limits.frostC) {
    recommendations.push({ code: "FROST", message: "Advertencia de helada." });
  }
  if (Math.max(current.temperature_c, forecast.temperature_max_c) >= limits.heatC) {
    recommendations.push({ code: "EXTREME_HEAT", message: "Advertencia de calor extremo." });
  }
  return recommendations;
};

module.exports = { getRecommendations };
