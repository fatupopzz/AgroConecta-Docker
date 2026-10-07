# Pruebas automatizadas e integración continua

Esta implementación comprueba el flujo real de autenticación de AgroConecta. Las pruebas envían solicitudes HTTP a la aplicación con Supertest, pasan por las rutas y controladores de Express y verifican el resultado directamente en una base de datos PostgreSQL real.

## Casos protegidos

### 1. Registro seguro de un agricultor

- **Componentes:** API de Express, controlador de autenticación, bcrypt y PostgreSQL.
- **Condición:** se envían todos los datos válidos de un agricultor nuevo.
- **Resultado esperado:** la API responde `201`, se crean las filas relacionadas en `usuario` y `agricultor`, la contraseña queda cifrada y el hash no aparece en la respuesta.
- **Regresión que detecta:** guardar la contraseña sin cifrar, dejar de crear el perfil o exponer el hash al cliente.

### 2. Inicio de sesión y consulta del perfil

- **Componentes:** API, PostgreSQL, bcrypt, generación de JWT y middleware de autorización.
- **Condición:** un agricultor registrado inicia sesión y usa su token en `GET /api/auth/me`.
- **Resultado esperado:** el inicio de sesión devuelve un token válido y el endpoint protegido devuelve el usuario y su perfil persistido.
- **Regresión que detecta:** generar o leer incorrectamente el token, romper el middleware o dejar de consultar el perfil relacionado.

### 3. Protección contra usuarios duplicados

- **Componentes:** API, validación del controlador, restricciones de PostgreSQL y relación usuario-agricultor.
- **Condición:** se intenta registrar dos veces el mismo correo.
- **Resultado esperado:** el segundo registro responde `400` y la base conserva exactamente un usuario y un perfil.
- **Regresión que detecta:** aceptar duplicados, crear perfiles huérfanos o modificar el registro original.

## Ejecución local

Las pruebas requieren PostgreSQL con el esquema de `backend/sql/init.sql` y estas variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET`.

Desde `backend`:

```bash
npm ci
npm test
npm run test:integration
```

## Proceso automatizado

El archivo `.github/workflows/automated-tests.yml` ejecuta dos trabajos separados ante un `push` a `develop` o `feature/**`, ante un pull request hacia `develop` y también de forma manual:

1. Instala las dependencias y ejecuta las pruebas unitarias y de regresión.
2. Inicia PostgreSQL 15, carga el esquema real y ejecuta las pruebas de integración.
3. Conserva durante 30 días los registros y el reporte JSON, incluso si una prueba falla.

## Evidencia de la regresión deliberada

La demostración utiliza la segunda prueba. El cambio deliberado altera la lectura del token `Bearer` en el middleware. GitHub Actions debe detectar que el inicio de sesión todavía entrega un token, pero la consulta autenticada ya no puede usarlo. Después se restaura la lectura correcta, se vuelve a ejecutar el flujo y la prueba aprueba nuevamente.

- Ejecución correcta inicial: [GitHub Actions #37560624741](https://github.com/fatupopzz/AgroConecta-Docker/actions/runs/37560624741).
- Regresión detectada: [GitHub Actions #37560847762](https://github.com/fatupopzz/AgroConecta-Docker/actions/runs/37560847762).
- Ejecución posterior a la corrección: [GitHub Actions #37560976010](https://github.com/fatupopzz/AgroConecta-Docker/actions/runs/37560976010).

## Guion sugerido para el video (5 a 8 minutos)

1. **0:00–0:45:** mostrar la rama y explicar que las pruebas cubren API, lógica de autenticación y PostgreSQL real.
2. **0:45–2:00:** abrir el archivo de pruebas y resumir los tres casos, sus datos y resultados esperados.
3. **2:00–3:00:** abrir el workflow y señalar los eventos, el servicio PostgreSQL, la ejecución y los artefactos.
4. **3:00–4:00:** mostrar la primera ejecución exitosa y sus dos trabajos en verde. Abrir el registro o el reporte descargable.
5. **4:00–5:15:** mostrar el commit de regresión, la línea modificada y la ejecución fallida. Abrir el detalle de la prueba que detectó el token inválido.
6. **5:15–6:30:** mostrar el commit de corrección y la ejecución posterior exitosa.
7. **6:30–7:15:** concluir que la estrategia detecta errores en rutas, autenticación, transacciones y persistencia, pero no reemplaza pruebas en dispositivos Android, servicios externos ni pruebas de carga.
