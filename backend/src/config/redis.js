const { createClient } = require("redis");

const redisUrl = process.env.REDIS_URL?.trim();
const connectTimeout = Number(process.env.REDIS_CONNECT_TIMEOUT_MS || 1500);

let redisClient;
let connectionPromise;

const getRedisClient = () => {
  if (!redisUrl) {
    return null;
  }

  if (!redisClient) {
    redisClient = createClient({
      url: redisUrl,
      socket: {
        connectTimeout,
        reconnectStrategy: (retries) =>
          retries < 3 ? Math.min(retries * 100, 500) : false,
      },
    });

    redisClient.on("error", (error) => {
      console.error("Error de Redis:", error.message);
    });
  }

  return redisClient;
};

const connectRedis = async () => {
  const client = getRedisClient();

  if (!client) {
    console.warn("Cache Redis deshabilitado: REDIS_URL no esta configurado.");
    return false;
  }

  if (client.isReady) {
    return true;
  }

  if (!connectionPromise) {
    connectionPromise = client
      .connect()
      .then(() => {
        console.log("Conectado a Redis");
        return true;
      })
      .catch((error) => {
        console.warn(
          `Redis no esta disponible; se continuara sin cache: ${error.message}`,
        );
        return false;
      })
      .finally(() => {
        connectionPromise = null;
      });
  }

  return connectionPromise;
};

const shutdownRedis = async () => {
  if (!redisClient?.isOpen) {
    return;
  }

  try {
    await redisClient.quit();
    console.log("Conexion a Redis cerrada.");
  } catch (error) {
    redisClient.disconnect();
    console.warn(`Redis se cerro de forma forzada: ${error.message}`);
  }
};

module.exports = {
  connectRedis,
  getRedisClient,
  shutdownRedis,
};
