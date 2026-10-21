const request = require("supertest");
const express = require("express");
const jwt = require("jsonwebtoken");
const { performance } = require("node:perf_hooks");
const { normalizeCoordinates, createWeatherService } = require("../src/services/weatherService");
const { getRecommendations } = require("../src/services/weatherRules");
const { createOpenMeteoService } = require("../src/services/openMeteoService");
const { createWeatherController, createFarmWeatherController } = require("../src/controllers/weatherController");
const verifyToken = require("../src/middleware/authMiddleware");
const asyncHandler = require("../src/middleware/asyncHandler");
const { errorHandler } = require("../src/middleware/errorHandler");

const coordinates = { lat: 14.63, lng: -90.51 };
const now = new Date("2026-10-15T04:45:00.000Z");
const start = Date.parse("2026-10-15T04:00:00Z") / 1000;
const weather = () => ({
  coordinates,
  current: { temperature_c: 20, wind_speed_kmh: 10, precipitation_mm: 0, weather_code: 0 },
  forecast_24h: { precipitation_mm: 0, precipitation_probability_max: 0, wind_speed_max_kmh: 10, temperature_min_c: 15, temperature_max_c: 24 },
  data_time: now.toISOString(), fetched_at: now.toISOString(), timezone: "UTC", source: "Open-Meteo",
});
const rawWeather = () => ({
  daily: {
    time: [1791957600, 1792044000, 1792130400],
    temperature_2m_min: [15, 16, 17], temperature_2m_max: [24, 25, 26],
    precipitation_sum: [1, 2, 3], precipitation_probability_max: [10, 20, 30],
  },
  current: { time: now.getTime() / 1000, temperature_2m: 20, wind_speed_10m: 10, precipitation: 0, weather_code: 0 },
  hourly: {
    time: Array.from({ length: 26 }, (_, i) => start + i * 3600),
    temperature_2m: Array(26).fill(20), precipitation: Array(26).fill(0),
    precipitation_probability: Array(26).fill(0), wind_speed_10m: Array(26).fill(10),
  },
});
const token = (tipo = "agricultor") => jwt.sign({ id: 1, tipo }, process.env.JWT_SECRET);
const appFor = (service) => {
  const app = express();
  app.get("/api/weather", verifyToken, asyncHandler(createWeatherController(service)));
  app.use(errorHandler);
  return app;
};
const fixture = () => {
  const cache = {
    get: vi.fn(async ns => ({ available: true, hit: false, key: ns })),
    set: vi.fn(async () => true),
  };
  const provider = { getWeather: vi.fn(async () => weather()) };
  const logger = { warn: vi.fn() };
  return { cache, provider, logger, service: createWeatherService({ cache, provider, logger }) };
};

describe('clima de dirección principal', () => {
  const farmApp = (service, database) => {
    const app = express();
    app.get('/api/weather/farm', verifyToken, asyncHandler(createFarmWeatherController(service, database)));
    app.use(errorHandler);
    return app;
  };
  test('resuelve solo la dirección principal del usuario autenticado', async () => {
    const database = { query: vi.fn().mockResolvedValue({ rows: [{ latitud: '14.634', longitud: '-90.506' }] }) };
    const service = { getWeather: vi.fn().mockResolvedValue(weather()) };
    const response = await request(farmApp(service, database)).get('/api/weather/farm?id_usuario=999').set('Authorization', `Bearer ${token()}`);
    expect(response.status).toBe(200);
    expect(database.query).toHaveBeenCalledWith(expect.stringContaining('d.es_predeterminada = TRUE'), [1]);
    expect(service.getWeather).toHaveBeenCalledWith(coordinates);
  });
  test('404 sin coordenadas guardadas permite fallback GPS en Android', async () => {
    const database = { query: vi.fn().mockResolvedValue({ rows: [] }) };
    const service = { getWeather: vi.fn() };
    const response = await request(farmApp(service, database)).get('/api/weather/farm').set('Authorization', `Bearer ${token()}`);
    expect(response.status).toBe(404);
    expect(service.getWeather).not.toHaveBeenCalled();
  });
  test('401 y 403 no consultan direcciones', async () => {
    const database = { query: vi.fn() };
    const app = farmApp({}, database);
    expect((await request(app).get('/api/weather/farm')).status).toBe(401);
    expect((await request(app).get('/api/weather/farm').set('Authorization', `Bearer ${token('distribuidor')}`)).status).toBe(403);
    expect(database.query).not.toHaveBeenCalled();
  });
});

afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); });

describe("coordenadas", () => {
  test.each([
    {}, { lat: "1" }, { lng: "1" }, { lat: "", lng: "0" }, { lat: " ", lng: "0" },
    { lat: "NaN", lng: "0" }, { lat: "Infinity", lng: "0" }, { lat: "1abc", lng: "0" },
    { lat: "90.001", lng: "0" }, { lat: "-90.001", lng: "0" },
    { lat: "0", lng: "180.001" }, { lat: "0", lng: "-180.001" },
    { lat: ["1", "2"], lng: "0" }, { lat: {}, lng: "0" },
    { lat: "0", lng: "" }, { lat: "0", lng: "NaN" }, { lat: "0x10", lng: "0" },
  ])("rechaza %j", query => {
    expect(() => normalizeCoordinates(query)).toThrow(expect.objectContaining({ statusCode: 400 }));
  });
  test.each([["90", "180"], ["-90", "-180"], ["0", "0"]])("acepta límites %s %s", (lat, lng) => {
    expect(normalizeCoordinates({ lat, lng })).toEqual({ lat: Number(lat), lng: Number(lng) });
  });
  test("redondea dos decimales y normaliza cero negativo", () => {
    expect(normalizeCoordinates({ lat: "14.634", lng: "-90.506" })).toEqual(coordinates);
    expect(normalizeCoordinates({ lat: "-0.001", lng: "0" })).toEqual({ lat: 0, lng: 0 });
  });
});

describe("reglas agronómicas", () => {
  test.each([
    ["precipitation_mm", 1, "RAIN"], ["precipitation_mm", 1.1, "RAIN"],
    ["precipitation_probability_max", 60, "RAIN"], ["precipitation_probability_max", 61, "RAIN"],
    ["wind_speed_max_kmh", 25, "STRONG_WIND"], ["wind_speed_max_kmh", 26, "STRONG_WIND"],
    ["temperature_min_c", 2, "FROST"], ["temperature_min_c", 1, "FROST"],
    ["temperature_max_c", 35, "EXTREME_HEAT"], ["temperature_max_c", 36, "EXTREME_HEAT"],
  ])("activa %s en %s", (field, value, code) => {
    const data = weather(); data.forecast_24h[field] = value;
    expect(getRecommendations(data.current, data.forecast_24h).map(rule => rule.code)).toEqual([code]);
  });
  test.each([
    ["precipitation_mm", 0.999], ["precipitation_probability_max", 59.99],
    ["wind_speed_max_kmh", 24.99], ["temperature_min_c", 2.01], ["temperature_max_c", 34.99],
  ])("no activa fuera del límite %s", (field, value) => {
    const data = weather(); data.forecast_24h[field] = value;
    expect(getRecommendations(data.current, data.forecast_24h)).toEqual([]);
  });
  test("varias reglas simultáneas", () => {
    const data = weather();
    Object.assign(data.forecast_24h, { precipitation_mm: 1, wind_speed_max_kmh: 25, temperature_min_c: 2, temperature_max_c: 35 });
    expect(getRecommendations(data.current, data.forecast_24h).map(rule => rule.code)).toEqual(["RAIN", "STRONG_WIND", "FROST", "EXTREME_HEAT"]);
  });
  test.each([["wind_speed_kmh", 25, "STRONG_WIND"], ["temperature_c", 2, "FROST"], ["temperature_c", 35, "EXTREME_HEAT"]])("considera clima actual %s", (field, value, code) => {
    const data = weather(); data.current[field] = value;
    expect(getRecommendations(data.current, data.forecast_24h).map(rule => rule.code)).toEqual([code]);
  });
});

describe("caché y fallback", () => {
  test("caché caliente: responde sin proveedor ni escrituras en menos de un segundo", async () => {
    const f = fixture();
    f.cache.get.mockResolvedValue({ hit: true, value: weather() });
    // Un proveedor que nunca resuelve detecta cualquier llamada accidental.
    f.provider.getWeather.mockImplementation(() => new Promise(() => {}));
    const started = performance.now();
    const response = await request(appFor(f.service)).get("/api/weather?lat=14.634&lng=-90.506").set("Authorization", `Bearer ${token()}`);
    expect(performance.now() - started).toBeLessThan(1000);
    expect(response.status).toBe(200);
    expect(response.body).toMatchObject({ cached: true, stale: false, coordinates });
    expect(f.provider.getWeather).not.toHaveBeenCalled();
    expect(f.cache.set).not.toHaveBeenCalled();
    expect(f.cache.get).toHaveBeenCalledExactlyOnceWith("weather:fresh", "daily3:14.63:-90.51");
  });
  test("éxito guarda ambas entradas con TTL exactos", async () => {
    const f = fixture(); const data = await f.service.getWeather(coordinates);
    expect(data).toMatchObject({ cached: false, stale: false, recommendations: [] });
    expect(f.cache.set).toHaveBeenCalledWith("weather:fresh", expect.objectContaining({ coordinates }), 1800);
    expect(f.cache.set).toHaveBeenCalledWith("weather:last", expect.objectContaining({ coordinates }), 86400);
    expect(f.cache.set).toHaveBeenCalledTimes(2);
  });
  test("fallo del proveedor devuelve último dato sin cambiar fecha ni renovar TTL", async () => {
    const f = fixture(); f.provider.getWeather.mockRejectedValue(new Error("secret"));
    f.cache.get.mockResolvedValueOnce({ hit: false }).mockResolvedValueOnce({ hit: true, value: weather() });
    expect(await f.service.getWeather(coordinates)).toEqual({ ...weather(), cached: true, stale: true });
    expect(f.cache.set).not.toHaveBeenCalled();
  });
  test("sin dato devuelve HTTP 503 controlado", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    const f = fixture(); f.provider.getWeather.mockRejectedValue(new Error("secret"));
    const response = await request(appFor(f.service)).get("/api/weather?lat=0&lng=0").set("Authorization", `Bearer ${token()}`);
    expect(response.status).toBe(503); expect(response.body.error).not.toContain("secret");
    expect(f.cache.set).not.toHaveBeenCalled();
  });
  test.each([false, true])("Redis falla, proveedor funciona (rechazo=%s)", async rejected => {
    const f = fixture();
    if (rejected) { f.cache.get.mockRejectedValue(new Error("redis secret")); f.cache.set.mockRejectedValue(new Error("secret")); }
    else { f.cache.get.mockResolvedValue({ available: false, hit: false }); f.cache.set.mockResolvedValue(false); }
    expect(await f.service.getWeather(coordinates)).toMatchObject({ cached: false, stale: false });
    expect(f.logger.warn).toHaveBeenCalledWith("No se pudo cachear el clima en Redis.");
  });
  test("al vencer 30 minutos conserva fallback hasta 24 horas", async () => {
    vi.useFakeTimers(); vi.setSystemTime(now);
    const entries = new Map();
    const cache = {
      get: async (ns, key) => { const k = `${ns}:${key}`; const e = entries.get(k); return { key: k, hit: !!e && e.expires > Date.now(), value: e?.value }; },
      set: async (key, value, ttl) => { entries.set(key, { value, expires: Date.now() + ttl * 1000 }); return true; },
    };
    const provider = { getWeather: vi.fn(async () => weather()) };
    const service = createWeatherService({ cache, provider });
    await service.getWeather(coordinates);
    vi.advanceTimersByTime(1800000 - 1);
    expect(await service.getWeather(coordinates)).toMatchObject({ cached: true, stale: false });
    provider.getWeather.mockRejectedValue(new Error("offline"));
    vi.advanceTimersByTime(1);
    expect(await service.getWeather(coordinates)).toMatchObject({ cached: true, stale: true });
    vi.advanceTimersByTime(86400000 - 1800000);
    await expect(service.getWeather(coordinates)).rejects.toMatchObject({ statusCode: 503 });
  });
});

describe("endpoint", () => {
  test.each(["", "?lat=1", "?lat=91&lng=0", "?lat=0&lng=-181", "?lat=1&lat=2&lng=0", "?lat[x]=1&lng=0"])('400 para %s', async query => {
    const f = fixture();
    const response = await request(appFor(f.service)).get(`/api/weather${query}`).set("Authorization", `Bearer ${token()}`);
    expect(response.status).toBe(400); expect(f.provider.getWeather).not.toHaveBeenCalled();
  });
  test("401 sin JWT y 403 para distribuidor", async () => {
    const f = fixture(); const app = appFor(f.service);
    expect((await request(app).get("/api/weather?lat=0&lng=0")).status).toBe(401);
    expect((await request(app).get("/api/weather?lat=0&lng=0").set("Authorization", `Bearer ${token("distribuidor")}`)).status).toBe(403);
    expect(f.cache.get).not.toHaveBeenCalled();
  });
  test("ruta registrada en app real", async () => {
    const app = require("../app");
    expect((await request(app).get("/api/weather?lat=91&lng=0").set("Authorization", `Bearer ${token()}`)).status).toBe(400);
  });
});

describe("Open-Meteo aislado", () => {
  test("solicita unidades explícitas y resume 24 intervalos UTC, cruzando medianoche", async () => {
    const raw = rawWeather(); raw.hourly.precipitation[0] = 99; raw.hourly.precipitation[25] = 99;
    raw.hourly.precipitation[1] = 1;
    const fetchImpl = vi.fn(async () => ({ ok: true, json: async () => raw }));
    const service = createOpenMeteoService({ fetchImpl, now: () => now });
    const data = await service.getWeather(coordinates);
    const url = fetchImpl.mock.calls[0][0];
    expect(url.searchParams.get("timeformat")).toBe("unixtime");
    expect(url.searchParams.get("wind_speed_unit")).toBe("kmh");
    expect(url.searchParams.has("apikey")).toBe(false);
    expect(data.forecast_24h).toMatchObject({ precipitation_mm: 1, from: "2026-10-15T04:00:00.000Z", to: "2026-10-16T04:00:00.000Z" });
    expect(data.data_time).toBe(now.toISOString());
    expect(data.forecast_3d).toHaveLength(3);
    expect(data.forecast_3d[0]).toMatchObject({ date: '2026-10-14', temperature_min_c: 15, precipitation_mm: 1 });
    expect(url.searchParams.get('timezone')).toBe('America/Guatemala');
  });
  test.each(["missing", "null", "old", "http", "json", "daily", "daily-null"])('rechaza respuesta %s', async type => {
    const raw = rawWeather();
    if (type === "missing") raw.hourly.time.pop(), raw.hourly.time.pop();
    if (type === "null") raw.hourly.temperature_2m[1] = null;
    if (type === "old") raw.current.time -= 7200;
    if (type === "daily") raw.daily.time.pop();
    if (type === "daily-null") raw.daily.precipitation_sum[1] = null;
    const fetchImpl = vi.fn(async () => ({ ok: type !== "http", json: async () => { if (type === "json") throw new Error("bad json"); return raw; } }));
    await expect(createOpenMeteoService({ fetchImpl, now: () => now }).getWeather(coordinates)).rejects.toThrow();
  });
  test("aborta al timeout y libera timer", async () => {
    vi.useFakeTimers();
    const fetchImpl = vi.fn((url, { signal }) => new Promise((resolve, reject) => signal.addEventListener("abort", () => reject(new Error("aborted")))));
    const pending = createOpenMeteoService({ fetchImpl, timeoutMs: 50 }).getWeather(coordinates);
    const assertion = expect(pending).rejects.toThrow("aborted");
    await vi.advanceTimersByTimeAsync(50); await assertion;
    expect(fetchImpl.mock.calls[0][1].signal.aborted).toBe(true);
    expect(vi.getTimerCount()).toBe(0);
  });
});
