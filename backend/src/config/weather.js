// TTL propios de clima: no dependen de CACHE_TTL_SECONDS del catálogo.
module.exports = Object.freeze({
  freshTtlSeconds: 1800,
  lastTtlSeconds: 86400,
  timeoutMs: 5000,
  thresholds: Object.freeze({ rainMm: 1, rainProbability: 60, windKmh: 25, frostC: 2, heatC: 35 }),
});
