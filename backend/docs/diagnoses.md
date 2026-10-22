# Diagnóstico de plagas backend

Autor: Juan Jose Rivas Alvarez, carnet 24856.

## Preparación de la base de datos

Para una base existente, ejecutar en orden `sql/hu029_pest_alerts_migration.sql`
y `sql/diagnostico_migration.sql`. Son idempotentes. Por ejemplo desde la raíz
del repositorio, en PowerShell:

```powershell
Get-Content backend/sql/hu029_pest_alerts_migration.sql -Raw | docker compose exec -T db psql -v ON_ERROR_STOP=1 -U agroconecta_user -d agroconecta
Get-Content backend/sql/diagnostico_migration.sql -Raw | docker compose exec -T db psql -v ON_ERROR_STOP=1 -U agroconecta_user -d agroconecta
```

El backend ejecuta ambas migraciones automáticamente al arrancar, tanto con
volúmenes nuevos como existentes, antes de aceptar peticiones.

`enfermedad_tratamiento` relaciona la etiqueta de enfermedad, cultivo, categoría,
ingredientes y una recomendación general. Cargar mapeos revisados por el equipo
agrícola usando las etiquetas exactas del modelo y las categorías del catálogo.
No se incluyen tratamientos inventados: el repositorio no contiene el modelo,
su lista de clases ni un listado aprobado de tratamientos por cultivo.
Sin mapeos el historial funciona y los productos devuelven lista vacía.

Ejemplo estructural para fixtures (valores ficticios, no tratamiento agrícola):

```sql
INSERT INTO enfermedad_tratamiento
    (enfermedad, cultivo, id_categoria, ingredientes_activos, recomendacion_general)
SELECT 'enfermedad-prueba', 'cultivo-prueba', id_categoria,
       ARRAY['ingrediente-prueba'], 'Recomendación de prueba'
FROM categoria WHERE nombre = 'Fungicidas'
ON CONFLICT DO NOTHING;
```

## Contrato HTTP

Todas las rutas requieren `Authorization: Bearer <JWT>` mediante `verifyToken`.
El usuario y la fecha se asignan en el servidor. Los resultados son reportados
por la app; no representan diagnósticos verificados por el servidor.

`POST /api/diagnoses` → 201, objeto diagnóstico guardado:

```json
{
  "cultivo": "Tomate",
  "enfermedad": "etiqueta-del-modelo",
  "confianza": 0.91,
  "foto": "/uploads/hoja.jpg",
  "latitud": 14.6349,
  "longitud": -90.5069
}
```

`foto` es una referencia (hasta 2048 caracteres), no bytes/base64 ni una carga
de imagen. Este endpoint no descarga la URL. Confianza es un número entre 0 y 1.
GPS es opcional; enviar ambas coordenadas numéricas o ambas nulas/omitidas.
`fecha` refleja la recepción del resultado en el backend.

`GET /api/diagnoses/mine` → 200, arreglo del usuario ordenado por fecha e ID
descendentes; sin historial devuelve `[]`.

`GET /api/diagnoses/:id/products` → 200:

```json
{
  "diagnostico_id": 1,
  "enfermedad": "etiqueta-del-modelo",
  "productos": [],
  "ingredientes_activos": [],
  "recomendacion_general": "No hay productos específicos disponibles. Confirme el diagnóstico con un técnico agrícola y aplique manejo integrado del cultivo antes de elegir un tratamiento."
}
```

Productos activos que coinciden en categoría e ingrediente en `composicion`,
según enfermedad y cultivo del mapeo. No recomienda todos los fungicidas ni busca
por el nombre del cultivo en la descripción. La composición sigue siendo texto
libre: los nombres de ingredientes del catálogo y del mapeo deben coincidir.
No filtra existencias ni calcula ofertas/precios por distribuidor.

`POST /api/diagnoses/:id/alert` con `{ "severidad": "media" }` (cuerpo opcional)
→ 201 `{ "alerta": { ... } }`. Usa enfermedad, cultivo y GPS del diagnóstico,
crea en `alerta_plaga` de HU-029 y llama al mismo servicio de notificaciones.
Acepta baja/media/alta/critica. Falta de GPS → 400. Repetir devuelve 200 y la misma
alerta, sin otro push. Un bloqueo de fila y una transacción evitan duplicados
simultáneos; si falla la vinculación se revierte la alerta.

Sin JWT o JWT inválido → 401. ID mal formado → 400. Diagnóstico ajeno o ausente
→ 404 para productos y publicación. Los errores de persistencia pasan por el
middleware central; un fallo de push no revierte una alerta ya confirmada.

## Plan B en VM, sin API de pago

`scripts/diagnose_onnx.js` permite ejecutar un clasificador ONNX en CPU con
`onnxruntime-node`. Su dependencia es optativa y se instala solo en el entorno
de inferencia: `npm install --no-save --package-lock=false onnxruntime-node`.
Basado en las APIs oficiales de [sesión](https://onnxruntime.ai/docs/api/js/interfaces/InferenceSession.html)
y [carga de modelo](https://onnxruntime.ai/docs/api/js/interfaces/InferenceSessionFactory.html).

Ejecutar en backend: `node scripts/diagnose_onnx.js /ruta/entrada.json`.
El JSON contiene `modelPath`, `dims`, `data` (tensor float32 preprocesado),
`labels` (orden de clases) y `outputKind` (`probabilities` o `logits`).
Puede especificar `inputName` y `outputName`. Admite un clasificador de una
entrada y un vector de clases de un solo ejemplo. Imprime únicamente
`{ "enfermedad": "...", "confianza": 0.9 }` para completar el POST habitual.

Antes de activarlo se necesita el modelo del teléfono exportado a ONNX, etiquetas,
dimensiones y el mismo preprocesamiento (resize, canales, normalización). No se
incluyen pesos ni se supone una normalización. Comparar predicciones Android/VM
con el mismo conjunto de imágenes etiquetadas antes de conectar la app a un
servicio de inferencia en la VM. El script es la vía local de contingencia;
esta entrega no añade un endpoint público que reciba fotos para inferencia.
Mover el mismo modelo de dispositivo no garantiza mejorar su precisión.

## Verificación

`npm test -- tests/diagnoses.test.js tests/diagnosisOnnx.test.js` usa Vitest,
Supertest, JWT real y mocks de PostgreSQL/push/runtime ONNX. Comprueba contratos,
validación, propietario, rollback, reintentos y traducción de salidas del modelo.
Los mocks no sustituyen una validación de migraciones en PostgreSQL ni de
precisión con los pesos reales.

Validación realizada el 21 de octubre de 2026:

- 38 pruebas nuevas aprobadas en las dos suites de Vitest.
- PostgreSQL 15 aislado: `init.sql`, HU-029, migración de diagnóstico ejecutada
  dos veces y `tests/diagnoses.smoke.sql` finalizaron sin errores. La prueba SQL
  verifica coincidencia de ingredientes, exclusión de productos inactivos y
  subcadenas, vínculo a alerta y restricciones de confianza/GPS. El contenedor
  desechable fue eliminado al terminar.
- Suite `test:unit`: 296 aprobadas, 24 fallidas y 8 omitidas. Los fallos están en
  pruebas existentes de imágenes (`EPERM` al borrar temporales en Windows) y
  dos pruebas de configuración que esperan LF al leer Compose con CRLF.
- No se ejecutó inferencia con pesos reales: no hay modelo en el repositorio.
