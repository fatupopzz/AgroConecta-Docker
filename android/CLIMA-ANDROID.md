# Clima Android: tarjeta del inicio y avisos

Juan Jose Rivas Alvarez — carnet 24856.

## Comportamiento

La tarjeta aparece debajo del encabezado del inicio del agricultor, antes de
productos y alertas. Muestra temperatura actual (°C), precipitación actual (mm),
pronóstico de hoy y los dos días siguientes (mínima/máxima, precipitación y
probabilidad de lluvia), los consejos literales del backend y la hora del dato.
Las fechas y horas se presentan en America/Guatemala. El proveedor se acredita
en la tarjeta.

Primero consulta la dirección de entrega principal mediante `GET weather/farm`.
Si responde 404, utiliza las coordenadas que HomeViewModel ya obtiene con el
permiso de ubicación del dashboard y llama a `GET weather?lat=...&lng=...`.
El ViewModel de clima no solicita permisos ni inicia una segunda lectura GPS.
Sin coordenadas guardadas ni GPS disponible, muestra una instrucción y permite
reintentar. No se geocodifican direcciones de texto ni se inventa una ubicación.
Si el usuario concede el permiso y llegan coordenadas, la tarjeta carga sola.

La autenticación utiliza el AuthInterceptor existente. La dirección se resuelve
en el backend por el JWT; el cliente no puede indicar un ID de otro agricultor.
Solo el rol agricultor carga o muestra el clima. Cambiar de cuenta o cerrar sesión
cancela la carga y descarta respuestas tardías y datos de la cuenta anterior.

## Antigüedad y errores

La tarjeta indica **Datos desactualizados** cuando el backend devuelve `stale`,
la consulta tiene al menos 30 minutos, el dato meteorológico tiene más de una
hora, el pronóstico comienza en otra fecha local o las marcas de tiempo son
inválidas/futuras. Se recalcula cada minuto mientras la pantalla está activa.
Al volver al primer plano, un dato antiguo se intenta actualizar.

Un error al refrescar conserva el último dato de esta pantalla, con aviso de
antigüedad y consejos rotulados como correspondientes al último dato disponible.
Un fallo inicial muestra error y botón Actualizar. HTTP 401/403 borra el dato
anterior y pide sesión de agricultor. No se presenta un fallo como cero lluvia.
Durante una solicitud se deshabilita Actualizar para evitar duplicados.

No se incorpora persistencia local del clima: el fallback remoto usa la caché
existente del backend y el fallback de pantalla conserva únicamente el dato que
ya estaba mostrado en ese ViewModel.

## Archivos y despliegue

- `data/models/WeatherModels.kt`: contrato de clima y pronóstico.
- `data/repository/WeatherRepository.kt`: Retrofit, validación y errores HTTP.
- `ui/home/WeatherViewModel.kt`: carga, prioridad de ubicación y antigüedad.
- `ui/home/WeatherCard.kt`: tarjeta Compose sin acceso directo a red ni GPS.
- `ui/navigation/AgroConectaNavHost.kt`: sesión, GPS y ciclo de vida.
- `backend/docs/weather.md`: ampliación del contrato, dirección y caché.

Desplegar el backend de esta rama antes de utilizar la nueva app: el contrato
anterior solo devolvía un resumen de 24 horas. La ampliación conserva ese resumen
y agrega `forecast_3d`. La caché usa `daily3:` para evitar resultados antiguos sin
ese campo. La app rechaza respuestas incompletas en lugar de inventar tres días.
No se agregan dependencias Android, servicios de pago ni claves de API.

## Pruebas

Desde `android`, con JDK 21 y SDK 34:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*WeatherRepositoryTest' --tests '*WeatherViewModelTest' --tests '*WeatherCardTest'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Desde `backend`: `npm test -- tests/weather.test.js`.

MockK usa `relaxed = false`, stubs por escenario, verificaciones de cantidades y
`confirmVerified`; el reloj es controlado. Las pruebas cubren prioridad de dirección,
GPS, ausencia de permiso/coordenadas, errores y reintento, antigüedad, cambios de
cuenta, respuesta tardía y duplicación de solicitudes. Las pruebas Compose con
Robolectric verifican tres días, temperatura/lluvia, consejo literal, aviso antiguo,
error y botón deshabilitado durante carga.

Para revisar manualmente: iniciar como agricultor con dirección principal con GPS;
comprobar su etiqueta. Sin dirección, conceder ubicación y comprobar el cambio a
ubicación actual. Sin ambas, comprobar la instrucción. Simular `stale=true` en un
backend de prueba y verificar el aviso junto a consejos históricos. Reintentar y
cambiar a otra cuenta para comprobar que los datos anteriores desaparecen.

## Resultado de validación — 21 de octubre de 2026

- Backend: 67 pruebas de clima aprobadas.
- Android: las 31 pruebas nuevas pasan (7 de repositorio, 20 del ViewModel con
  MockK estricto y 4 de Compose/Robolectric).
- Suite Android completa: 383 ejecutadas, 382 aprobadas y una fallida.
  La prueba existente `OrderPdfRepositoryTest.Android anterior a Q guarda URI
  de contenido segura` falla porque FileProvider no reconoce la raíz temporal
  `external-files` de Robolectric en Windows. No se modificaron exportación PDF,
  su FileProvider ni sus pruebas como parte de clima.
- La tarea `assembleDebug` finalizó y generó
  `app/build/outputs/apk/debug/app-debug.apk`. El comando combinado terminó con
  error por la prueba PDF mencionada; no se presenta la suite completa como verde.
- `git diff --check` sin errores.
- No se realizó una prueba manual en dispositivo/emulador ni un despliegue.
  Los flujos visuales se verificaron mediante Compose con Robolectric.

## Commits por parte

1. `edcdbf2`: backend de pronóstico diario y dirección principal.
2. `bad64bb`: contrato y repositorio Android con pruebas.
3. `20f75c7`: ViewModel, antigüedad, sesión y pruebas MockK estrictas.
4. `252162b`: tarjeta, integración en inicio/ciclo de vida y pruebas Compose.
5. Documentación y resultados de validación de esta entrega.
