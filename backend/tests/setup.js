process.env.NODE_ENV = "test";
process.env.DB_HOST ||= "127.0.0.1";
process.env.DB_PORT ||= "5432";
process.env.DB_NAME ||= "agroconecta_test";
process.env.DB_USER ||= "agroconecta_test";
process.env.DB_PASSWORD ||= "agroconecta_test";
process.env.JWT_SECRET ||= "vitest-secret";
process.env.REDIS_URL = "";
