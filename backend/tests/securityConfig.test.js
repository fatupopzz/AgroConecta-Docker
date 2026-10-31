const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const repositoryRoot = path.join(__dirname, "..", "..");

test("docker compose obtains JWT_SECRET from the environment", () => {
  const compose = fs.readFileSync(
    path.join(repositoryRoot, "docker-compose.yml"),
    "utf8"
  );

  assert.match(compose, /JWT_SECRET:\s*\$\{JWT_SECRET:\?/);
  assert.doesNotMatch(compose, /agroconecta_jwt_secret_dev_2026/);
});

test("environment files are ignored while the example remains versionable", () => {
  const gitignore = fs.readFileSync(
    path.join(repositoryRoot, ".gitignore"),
    "utf8"
  );
  const example = fs.readFileSync(
    path.join(repositoryRoot, ".env.example"),
    "utf8"
  );

  assert.match(gitignore, /^\.env$/m);
  assert.match(gitignore, /^!\.env\.example$/m);
  assert.match(example, /^JWT_SECRET=REEMPLAZAR_/m);
  assert.doesNotMatch(example, /agroconecta_jwt_secret_dev_2026/);
});

test("startup applies pending migrations, including pest alerts", () => {
  const backendEntryPoint = fs.readFileSync(
    path.join(repositoryRoot, "backend", "index.js"),
    "utf8"
  );
  const migration = fs.readFileSync(
    path.join(repositoryRoot, "backend", "sql", "009_hu029_pest_alerts.sql"),
    "utf8"
  );

  // El arranque ya no nombra cada archivo: aplica todas las pendientes.
  assert.match(backendEntryPoint, /runMigrations\(/);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS alerta_plaga/);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS instalacion_alerta_plaga/);
  assert.match(migration, /fcm_registration_token\s+TEXT PRIMARY KEY/);
  assert.match(migration, /RENAME COLUMN firebase_installation_id TO fcm_registration_token/);
});

test("distributor coordinates migration is versioned", () => {
  const migration = fs.readFileSync(
    path.join(repositoryRoot, "backend", "sql", "012_kan103_distributor_coordinates.sql"),
    "utf8",
  );

  assert.match(migration, /ADD COLUMN IF NOT EXISTS latitud DECIMAL\(10,7\)/);
  assert.match(migration, /CHECK \(latitud BETWEEN -90 AND 90\)/);
  assert.match(migration, /ADD COLUMN IF NOT EXISTS longitud DECIMAL\(10,7\)/);
  assert.match(migration, /CHECK \(longitud BETWEEN -180 AND 180\)/);
});

test("product images migration is versioned", () => {
  const migration = fs.readFileSync(
    path.join(repositoryRoot, "backend", "sql", "013_product_images.sql"),
    "utf8",
  );

  assert.match(migration, /ADD COLUMN IF NOT EXISTS imagen_url VARCHAR\(255\)/);
});

test("docker compose persists uploads and keeps redis off the host", () => {
  const compose = fs.readFileSync(
    path.join(repositoryRoot, "docker-compose.yml"),
    "utf8",
  );
  const redisService = compose.match(/^  redis:\n([\s\S]*?)(?=^  \S)/m)?.[1] ?? "";

  assert.match(compose, /- uploads_data:\/app\/uploads/);
  assert.match(compose, /^  uploads_data:/m);
  assert.ok(redisService.length > 0, "no se encontró el servicio redis");
  assert.doesNotMatch(redisService, /^\s+ports:/m);
  assert.doesNotMatch(compose, /6379:6379/);
});

test("caddy only runs with the https profile and proxies to the backend", () => {
  const compose = fs.readFileSync(
    path.join(repositoryRoot, "docker-compose.yml"),
    "utf8",
  );
  const caddyfile = fs.readFileSync(
    path.join(repositoryRoot, "caddy", "Caddyfile"),
    "utf8",
  );
  const caddyService = compose.match(/^  caddy:\n([\s\S]*?)(?=^  \S)/m)?.[1] ?? "";

  // Sin el perfil, cualquiera que levante el proyecto en local perdería los
  // puertos 80 y 443 por un proxy que no tiene dominio que certificar.
  assert.match(caddyService, /profiles: \["https"\]/);
  assert.match(caddyService, /- caddy_data:\/data/);
  assert.match(caddyfile, /\{\$AGROCONECTA_DOMAIN:localhost\}/);
  assert.match(caddyfile, /reverse_proxy backend:8080/);
});
