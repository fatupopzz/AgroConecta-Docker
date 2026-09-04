const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

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
