#!/usr/bin/env bash
# Ejecutar desde cualquier carpeta. Solo crea PostgreSQL efímero sin puertos publicados.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
bench="$root/backend/benchmarks/kan107"
output=$(mktemp -d "${TMPDIR:-/tmp}/kan107.XXXXXX")
container="kan107-benchmark-$$"
image="kan107-validation:local"
cleanup() { docker stop "$container" >/dev/null 2>&1 || true; }
trap cleanup EXIT
cp "$bench/seed.sql" "$output/seed.sql"
docker build -t "$image" "$root/backend"
docker run -d --rm --name "$container" --label task=KAN-107 \
  --tmpfs /var/lib/postgresql/data:rw \
  -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_DB=kan107_benchmark postgres:15-alpine >/dev/null
for attempt in {1..30}; do
  if docker exec "$container" pg_isready -U postgres -d kan107_benchmark >/dev/null 2>&1; then break; fi
  sleep 1
done
docker exec "$container" pg_isready -U postgres -d kan107_benchmark >/dev/null
run() {
 docker run --rm --tmpfs /repo/backend/node_modules --network "container:$container" -e NODE_PATH=/app/node_modules \
  -v "$root:/repo:ro" -v "$output:/bench" --entrypoint node "$image" \
  "/repo/backend/benchmarks/kan107/$1" "${2:-}"
}
run benchmark.cjs seed
docker exec "$container" psql -U postgres -d kan107_benchmark -v ON_ERROR_STOP=1 -c 'VACUUM ANALYZE' >/dev/null
run benchmark.cjs before
run benchmark.cjs after
run ablation.cjs
run category-check.cjs
run validate.cjs
run http-check.cjs before
run http-check.cjs after
run writes.cjs before
docker exec "$container" psql -U postgres -d kan107_benchmark -v ON_ERROR_STOP=1 -c 'VACUUM ANALYZE' >/dev/null
run writes.cjs after
printf 'Evidencia guardada en %s\n' "$output"
