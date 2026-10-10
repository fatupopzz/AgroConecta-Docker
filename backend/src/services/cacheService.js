const { getRedisClient } = require("../config/redis");

const CACHE_PREFIX = "agroconecta:cache";

const versionKey = (namespace) => `${CACHE_PREFIX}:version:${namespace}`;
const responseKey = (namespace, version, requestKey) =>
  `${CACHE_PREFIX}:${namespace}:v${version}:${requestKey}`;

const get = async (namespace, requestKey) => {
  const client = getRedisClient();

  if (!client?.isReady) {
    return { available: false, hit: false };
  }

  try {
    const version = (await client.get(versionKey(namespace))) || "0";
    const key = responseKey(namespace, version, requestKey);
    const serializedValue = await client.get(key);

    if (serializedValue === null) {
      return { available: true, hit: false, key };
    }

    return {
      available: true,
      hit: true,
      key,
      value: JSON.parse(serializedValue),
    };
  } catch (error) {
    console.warn(`No se pudo leer la cache ${namespace}: ${error.message}`);
    return { available: false, hit: false };
  }
};

const set = async (key, value, ttlSeconds) => {
  const client = getRedisClient();

  if (!client?.isReady || !key) {
    return false;
  }

  try {
    await client.set(key, JSON.stringify(value), { EX: ttlSeconds });
    return true;
  } catch (error) {
    console.warn(`No se pudo guardar la respuesta en cache: ${error.message}`);
    return false;
  }
};

const invalidate = async (namespaces) => {
  const client = getRedisClient();

  if (!client?.isReady) {
    return false;
  }

  try {
    await Promise.all(
      [...new Set(namespaces)].map((namespace) =>
        client.incr(versionKey(namespace)),
      ),
    );
    return true;
  } catch (error) {
    console.warn(`No se pudo invalidar la cache: ${error.message}`);
    return false;
  }
};

module.exports = {
  get,
  set,
  invalidate,
};
