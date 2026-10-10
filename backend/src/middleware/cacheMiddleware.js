const defaultCacheService = require("../services/cacheService");

const configuredTtl = Number(process.env.CACHE_TTL_SECONDS || 300);
const DEFAULT_TTL_SECONDS =
  Number.isInteger(configuredTtl) && configuredTtl > 0 ? configuredTtl : 300;

const normalizeValue = (value) => {
  if (Array.isArray(value)) {
    return value.map(normalizeValue);
  }

  if (value && typeof value === "object") {
    return Object.keys(value)
      .sort()
      .reduce((normalized, key) => {
        normalized[key] = normalizeValue(value[key]);
        return normalized;
      }, {});
  }

  return value;
};

const buildRequestKey = (req) => {
  const path = `${req.baseUrl || ""}${req.path || ""}`;
  return `${path}:${JSON.stringify(normalizeValue(req.query || {}))}`;
};

const cacheResponse = (
  namespace,
  { cacheService = defaultCacheService, ttlSeconds = DEFAULT_TTL_SECONDS } = {},
) => async (req, res, next) => {
  if (req.method !== "GET") {
    next();
    return;
  }

  try {
    const cached = await cacheService.get(namespace, buildRequestKey(req));

    if (cached.hit) {
      res.set("X-Cache", "HIT");
      res.json(cached.value);
      return;
    }

    res.set("X-Cache", cached.available ? "MISS" : "BYPASS");

    if (cached.available && cached.key) {
      const originalJson = res.json.bind(res);
      res.json = (body) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          void cacheService.set(cached.key, body, ttlSeconds);
        }
        return originalJson(body);
      };
    }

    next();
  } catch (error) {
    console.warn(`Cache omitida para ${namespace}: ${error.message}`);
    res.set("X-Cache", "BYPASS");
    next();
  }
};

const invalidateCache = (...namespaces) => {
  const middleware = (req, res, next) => {
    res.once("finish", () => {
      if (res.statusCode >= 200 && res.statusCode < 300) {
        void middleware.cacheService.invalidate(namespaces);
      }
    });
    next();
  };

  middleware.cacheService = defaultCacheService;
  return middleware;
};

module.exports = {
  DEFAULT_TTL_SECONDS,
  buildRequestKey,
  cacheResponse,
  invalidateCache,
};
