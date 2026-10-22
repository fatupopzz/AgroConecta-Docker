# KAN-111 — Clima backend

## Extensión para tarjeta Android

La respuesta incorpora `forecast_3d`, tres días de calendario (hoy y dos días
siguientes) con `date` (YYYY-MM-DD), `temperature_min_c`, `temperature_max_c`,
`precipitation_mm` y `precipitation_probability_max`. `forecast_timezone` es
`America/Guatemala`; `data_time`, `fetched_at` y el resumen de 24 horas conservan
instantes ISO UTC. Los agregados diarios se solicitan al proveedor para esa zona
y se rechazan respuestas diarias incompletas. Contrato del proveedor:
[Open-Meteo](https://open-meteo.com/en/docs).

`GET /api/weather/farm` usa JWT y el mismo rol agricultor; obtiene exclusivamente
las coordenadas de su dirección de entrega predeterminada. Devuelve el mismo
contrato de clima, 404 si no tiene coordenadas guardadas, y no acepta IDs de otro
usuario. Android puede usar GPS mediante `GET /api/weather?lat=...&lng=...` cuando
recibe ese 404. Un fallo del proveedor conserva 503 o el último dato `stale`.

Las claves de caché llevan ahora el prefijo `daily3:` antes de las coordenadas
para no servir el contrato antiguo sin pronóstico diario. TTL y namespaces se
conservan. Verificación de esta extensión: 67 pruebas de clima aprobadas.

## Diagnóstico y alcance

Base: origin/develop `faada19`, con KAN-107 integrado mediante PR #101
(commit `9e904e7`), Redis y Vitest existentes. Se reutilizan `cacheService`,
`config/redis`, `verifyToken`, `asyncHandler` y `AppError`. No hay nuevas dependencias,
infraestructura Redis duplicada, cambios SQL, Android ni configuración de producción.

## Contrato

`GET /api/weather?lat=14.634&lng=-90.506`

Requiere `Authorization: Bearer <JWT>` de usuario con `tipo=agricultor`, siguiendo
la autorización de funcionalidades como favoritos. Respuestas: 401 sin JWT válido,
403 para otro rol, 400 con coordenadas inválidas, 503 si el proveedor falla y no hay
último dato disponible. Los errores tienen forma `{ "error": "mensaje público" }`.
Se aceptan coordenadas decimales finitas; no parámetros duplicados, objetos,
cadenas vacías, hexadecimal ni texto. Los límites se validan antes de redondear.

Ejemplo ilustrativo de respuesta 200 (no es un pronóstico real):

```json
{
  "coordinates": { "lat": 14.63, "lng": -90.51 },
  "current": { "temperature_c": 20, "wind_speed_kmh": 10, "precipitation_mm": 0, "weather_code": 0 },
  "forecast_24h": {
    "from": "2026-10-15T04:00:00.000Z",
    "to": "2026-10-16T04:00:00.000Z",
    "precipitation_mm": 1,
    "precipitation_probability_max": 60,
    "wind_speed_max_kmh": 15,
    "temperature_min_c": 15,
    "temperature_max_c": 24
  },
  "data_time": "2026-10-15T04:45:00.000Z",
  "fetched_at": "2026-10-15T04:45:00.000Z",
  "timezone": "UTC",
  "source": "Open-Meteo",
  "recommendations": [{ "code": "RAIN", "message": "Lluvia prevista: no aplicar herbicida ni fertilizante foliar." }],
  "cached": false,
  "stale": false
}
```

`data_time` es la fecha del clima actual del proveedor, `fetched_at` la consulta.
Se mantienen intactas al servir caché o fallback. `cached=true, stale=false`
indica caché fresca; `cached=true, stale=true` indica último dato ante fallo.
No se convierte un fallo en una lista vacía de recomendaciones.

## Decisiones técnicas

- Coordenadas redondeadas a dos decimales tanto en la consulta como en la clave:
  agrupa fincas próximas (aproximadamente 1 km de latitud), reduce llamadas y evita
  que el dato dependa de quién llenó primero la celda. No es precisión de parcela.
- Namespaces `weather:fresh` y `weather:last`, clave `14.63:-90.51`, sobre el
  prefijo/versionado del servicio existente. Versión inicial: claves
  `agroconecta:cache:weather:fresh:v0:14.63:-90.51` y
  `agroconecta:cache:weather:last:v0:14.63:-90.51`.
- TTL fresca exactamente 1800 segundos; último válido 86400 segundos (24 horas).
  Este último permite fallback tras vencer la fresca, limitando la antigüedad.
  Un fallback o un acierto no renueva ninguno de los TTL.
- Redis indisponible: se consulta al proveedor, se responde si funciona y se
  registra un aviso genérico si no se pudo guardar. Si ambos fallan y no hay
  dato accesible, se devuelve 503; no se promete fallback sin Redis disponible.
- Umbrales centralizados en `src/config/weather.js`: lluvia acumulada >=1 mm
  O probabilidad máxima >=60%; viento >=25 km/h; helada <=2 °C; calor >=35 °C.
  Viento y temperaturas consideran el estado actual y los extremos del resumen.
  Las cuatro reglas pueden activarse juntas; no son mutuamente excluyentes.
- Open-Meteo se consulta sin API key. `OPEN_METEO_URL` permite configurar el
  proveedor (predeterminado `https://api.open-meteo.com/v1/forecast`);
  `OPEN_METEO_TIMEOUT_MS` establece timeout positivo (predeterminado 5000 ms).
  AbortController cubre conexión y lectura JSON. `fetchImpl`, reloj, proveedor,
  caché y logger se pueden inyectar en tests.
- Unidades explícitas: °C, km/h y mm. Epoch del proveedor y salida ISO 8601 UTC,
  sin depender de la zona horaria del servidor. Se solicitan tres días para cubrir
  cambios de fecha. El resumen usa 24 intervalos horarios desde el inicio de la
  hora de consulta; `from`/`to` exponen esta aproximación horaria. La precipitación
  de cada marca horaria corresponde a la hora precedente. Se excluyen horas
  anteriores y posteriores a la ventana. No se usa el agregado del día calendario.
- Respuestas incompletas, valores nulos/no numéricos, probabilidades fuera de
  rango o clima actual con más de una hora de desfase se tratan como fallo del
  proveedor: no se cachean como resultado válido.
- Las reglas son orientativas; una lista vacía significa que no se activaron estos
  cuatro umbrales. Un cliente debe mostrar claramente `stale` y las fechas, sin
  presentar datos antiguos como una recomendación actual.

Referencia del contrato del proveedor: https://open-meteo.com/en/docs

## Pruebas reproducibles sin Internet ni Redis

Desde el worktree:

```sh
cd backend
npm ci
npm test -- tests/weather.test.js
npm test
```

`--runInBand` del AGENTS.md original es una opción de Jest incompatible con
Vitest. El comando vigente del repositorio es `npm test` (`vitest run`).
Los tests nuevos usan proveedor y caché simulados; cubren límites, reglas aisladas
simultáneas, autorización, fallback, TTL con reloj falso, Redis caído, respuestas
incompletas y timeout. La prueba HTTP caliente mide menos de 1000 ms con Supertest
y comprueba cero llamadas al proveedor (que nunca resolvería si fuera invocado).
No mide latencia real de infraestructura de producción.

## Prueba manual

1. Usar este worktree, Docker activo y una configuración local autorizada ya
   disponible con JWT_SECRET y `backend/.env`; no copiar secretos al informe.
2. En un entorno sin conflictos con otros contenedores del proyecto:

   ```sh
   docker compose config --quiet
   docker compose up --build -d db redis backend
   docker compose ps
   docker compose logs --tail=30 backend
   ```

   El Compose original fija nombres y puertos: no iniciarlo encima de otros
   entornos. Durante la verificación se usó `/tmp/kan111-compose.json`, derivado
   del original con nombres/volúmenes aislados y puerto `127.0.0.1:18089`, sin
   archivos `.env`. Es un artefacto temporal local, no configuración de producción.
3. Iniciar sesión como agricultor y proporcionar el JWT en la variable `TOKEN`
   sin imprimirlo. Ejecutar (usar 18089 para el entorno aislado):

   ```sh
   curl -sS -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/weather?lat=14.634&lng=-90.506'
   curl -sS -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/weather?lat=14.634&lng=-90.506'
   curl -sS -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/weather?lat=91&lng=0'
   ```

   Primera consulta no cacheada: `cached=false, stale=false`; segunda:
   `cached=true, stale=false`; inválida: HTTP 400. Sin JWT: 401; distribuidor: 403.
4. Consultar TTL sin mostrar contenido sensible:

   ```sh
   docker exec kan111-validation-redis redis-cli TTL agroconecta:cache:weather:fresh:v0:14.63:-90.51
   docker exec kan111-validation-redis redis-cli TTL agroconecta:cache:weather:last:v0:14.63:-90.51
   ```

   Tras una consulta nueva deben acercarse a 1800 y 86400 segundos.
5. Para fallback/Redis caído sin intervenir servicios ni esperar 30 minutos,
   ejecutar `npm test -- tests/weather.test.js`: simula fallos y vencimientos.
   La prueba manual con Open-Meteo real requiere Internet; no forma parte de la
   suite ni de la medición estable de rendimiento.
6. Detener solo los contenedores de validación cuando ya no se necesiten:
   `docker stop kan111-validation-backend kan111-validation-redis kan111-validation-db`.
   No eliminar volúmenes.

## Resultados de verificación

- `npm test -- tests/weather.test.js`: 62/62 correctas en Node 25 local.
- `npm test`: 231 correctas y 8 omitidas (27 archivos pasan, uno omitido).
  Las ocho pertenecen a `priceNotifications.integration.test.js`, opt-in mediante
  `KAN102_PG_PORT`, con PostgreSQL temporal específico; no pertenecen a clima.
- Mismo archivo de clima en Docker Node 20: 62/62 correctas.
- `node --check`: todos los archivos JavaScript añadidos/modificados pasan.
- `git diff --check`: sin errores.
- Compose original requiere configuración local no presente en el worktree.
  `config --quiet` pasó con el Compose temporal derivado, luego `up --build -d`
  levantó backend, PostgreSQL y Redis aislados. Logs confirman conexión a ambos.
- Prueba adicional con Redis real y proveedor simulado: endpoint HTTP 200,
  cached=true, stale=false, 66 ms; TTL observados 1800 y 86400 segundos.
  Se insertó una fixture únicamente para coordenadas 0,0 en Redis de validación;
  no es información meteorológica real. La prueba estable exigida es la de Vitest.
- Consulta real a través del endpoint local realizada el 14 de octubre de 2026
  a las 23:13 (Guatemala), para lat=14.634 y lng=-90.506. Primera petición:
  HTTP 200 en 1025 ms, source=Open-Meteo, cached=false, stale=false. Segunda:
  HTTP 200 en 10 ms, cached=true, stale=false, misma fecha de obtención.
  Se verificó la regla RAIN con 3.6 mm previstos en 24 horas. El criterio de
  menos de un segundo corresponde a caché caliente y se cumplió.
  Las pruebas automatizadas siguen sin depender de Internet.
- Docker informó siete vulnerabilidades en dependencias existentes durante
  `npm install` (dos críticas, tres altas, una moderada y una baja). No se añadieron
  dependencias ni se modificó el lockfile; su actualización queda fuera de KAN-111.
- Sin commit, push, merge, rebase, PR ni despliegue. Los contenedores de validación
  quedan activos para inspección; los entornos previos no se modificaron.
