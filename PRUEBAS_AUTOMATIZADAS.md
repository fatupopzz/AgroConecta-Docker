# Pruebas automatizadas e integración continua

Cada pull request hacia `develop` corre las pruebas del backend y de la app
Android en GitHub Actions (`.github/workflows/automated-tests.yml`). Si alguna
falla, el PR no se puede fusionar.

## Qué se ejecuta

El workflow corre ante un pull request hacia `develop`, ante cada push a
`develop` y también a mano (pestaña *Actions* → *Run workflow*). Tiene tres
trabajos que corren en paralelo:

| Check | Qué comprueba |
|---|---|
| **Backend · unitarias** | Todas las pruebas de Vitest que no necesitan base de datos (`npm run test:unit`). |
| **Backend · integración PostgreSQL** | Levanta PostgreSQL 15, arma el esquema con `scripts/migrate.js` en una base vacía (y lo corre dos veces para confirmar que no reaplica nada) y ejecuta las pruebas contra la base real (`npm run test:integration`). |
| **Android · unitarias** | `./gradlew testDebugUnitTest` con Java 17. |

Un commit nuevo en el mismo PR cancela la corrida anterior. Cada trabajo guarda
sus reportes como artefactos durante 30 días, aunque las pruebas fallen:
JUnit XML para el backend y el reporte HTML más los XML de Gradle para Android.
Se descargan desde la página de la corrida, en la sección *Artifacts*.

## Pruebas de integración con PostgreSQL

`tests/integration/authDatabase.integration.test.js` recorre el flujo real de
autenticación con Supertest:

1. **Registro seguro de un agricultor.** Responde `201`, crea las filas en
   `usuario` y `agricultor`, guarda la contraseña cifrada y no devuelve el hash.
2. **Inicio de sesión y perfil.** El token que entrega el login sirve para
   `GET /api/auth/me`, que devuelve el usuario y su perfil.
3. **Correo duplicado.** El segundo registro responde `400` y la base conserva
   un solo usuario y un solo perfil.

`tests/priceNotifications.integration.test.js` (KAN-102) prueba los avisos de
baja de precio contra PostgreSQL. Las dos solo corren con base de datos: fuera
de CI se omiten, así `npm test` funciona en cualquier máquina.

## Ejecutarlas en local

Desde `backend/`:

```bash
npm ci
npm run test:unit
```

Para las de integración hace falta un PostgreSQL con el esquema aplicado y
estas variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`,
`JWT_SECRET` y `DB_INTEGRATION=1`. Con la base de Docker del proyecto, usa una
base aparte para no mezclar datos de prueba:

```bash
node scripts/migrate.js
DB_INTEGRATION=1 npx vitest run tests/integration
```

App Android, desde `android/`:

```bash
./gradlew testDebugUnitTest
```

## Protección de la rama develop

En GitHub, *Settings* → *Rules* → *Rulesets*, la regla de `develop` exige:

- que los cambios entren por pull request;
- que pasen los tres checks: **Backend · unitarias**, **Backend · integración
  PostgreSQL** y **Android · unitarias**;
- que no se borre la rama ni se reescriba su historial.

Los nombres de los checks son los `name` de los trabajos del workflow. Si se
cambian ahí, hay que cambiarlos también en la regla; si no, GitHub espera un
check que ya no existe y ningún PR se puede fusionar.

## Comprobar que una prueba rota bloquea el merge

1. En una rama nueva, rompe a propósito una prueba (por ejemplo, cambia el
   `toBe(201)` del registro por `toBe(200)`) y abre un PR hacia `develop`.
2. El check correspondiente sale en rojo y el botón de merge queda bloqueado.
3. Revierte el cambio con otro commit: los checks vuelven a verde y el merge
   se habilita.

## Evidencia del Sprint 10

Antes de este workflow, la rama `feature/CI-pruebas-automatizadas` demostró la
detección de una regresión deliberada en la lectura del token `Bearer`:

- Ejecución correcta inicial: [#37560624741](https://github.com/fatupopzz/AgroConecta-Docker/actions/runs/37560624741).
- Regresión detectada: [#37560847762](https://github.com/fatupopzz/AgroConecta-Docker/actions/runs/37560847762).
- Ejecución posterior a la corrección: [#37560976010](https://github.com/fatupopzz/AgroConecta-Docker/actions/runs/37560976010).
