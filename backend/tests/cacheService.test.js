const { installCommonJsMock } = require("./helpers/commonJsMocks");

const redisConfigPath = require.resolve("../src/config/redis");
const cacheServicePath = require.resolve("../src/services/cacheService");

const loadCacheService = (client) => {
  delete require.cache[cacheServicePath];
  installCommonJsMock(redisConfigPath, {
    getRedisClient: () => client,
  });
  return require(cacheServicePath);
};

describe("cacheService", () => {
  afterEach(() => {
    delete require.cache[cacheServicePath];
    delete require.cache[redisConfigPath];
  });

  it("usa la version cero mientras el namespace no ha sido invalidado", async () => {
    const client = {
      isReady: true,
      get: vi.fn().mockResolvedValueOnce(null).mockResolvedValueOnce(null),
    };
    const cacheService = loadCacheService(client);

    const result = await cacheService.get("products", "/api/products:{}");

    expect(result).toEqual({
      available: true,
      hit: false,
      key: "agroconecta:cache:products:v0:/api/products:{}",
    });
  });

  it("serializa respuestas y respeta el TTL", async () => {
    const client = { isReady: true, set: vi.fn().mockResolvedValue("OK") };
    const cacheService = loadCacheService(client);

    await expect(
      cacheService.set("cache-key", { products: [1] }, 120),
    ).resolves.toBe(true);
    expect(client.set).toHaveBeenCalledWith(
      "cache-key",
      JSON.stringify({ products: [1] }),
      { EX: 120 },
    );
  });

  it("invalida cada namespace una sola vez incrementando su version", async () => {
    const client = { isReady: true, incr: vi.fn().mockResolvedValue(1) };
    const cacheService = loadCacheService(client);

    await expect(
      cacheService.invalidate(["products", "products", "distributors"]),
    ).resolves.toBe(true);
    expect(client.incr).toHaveBeenCalledTimes(2);
    expect(client.incr).toHaveBeenCalledWith(
      "agroconecta:cache:version:products",
    );
    expect(client.incr).toHaveBeenCalledWith(
      "agroconecta:cache:version:distributors",
    );
  });
});
