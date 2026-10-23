# Modelo de diagnóstico de plagas (KAN-110)

Clasificador de hojas que corre en el teléfono, sin conexión. Reconoce enfermedades
de **maíz, tomate, papa y chile** (19 clases, incluidas las hojas sanas).

- Archivos que usa la app: `android/app/src/main/assets/ml/plant_disease.tflite`
  y `labels.txt`, con una etiqueta por línea en el mismo orden que la salida del modelo.
- Entrada: imagen RGB `uint8` de 224×224, forma `[1, 224, 224, 3]`. La
  normalización de MobileNetV2 va dentro del modelo; Android solo escala la foto.
- Salida: `[1, 19]` con probabilidades (softmax).

## Regenerar el modelo

Desde la raíz del repositorio, con Python 3.10 a 3.13:

```bash
python3 -m venv ml/.venv
ml/.venv/bin/pip install -r ml/requirements.txt
ml/.venv/bin/python ml/train_plantvillage.py --epochs 5 --fine-tune-epochs 2
```

- La primera vez descarga el dataset (unos 830 MB) a `ml/.data/`. Esa carpeta y
  `ml/.venv/` están en `.gitignore`.
- El script sobrescribe los dos archivos de `assets/ml/` e imprime la precisión.
- Para un entrenamiento más corto: `--fine-tune-epochs 0` deja la base de
  MobileNetV2 congelada y solo entrena la capa final.

## Resultado del modelo incluido

Entrenado el 22 de octubre de 2026 en CPU (4 núcleos, sin GPU), en unos 45 minutos,
con `--epochs 5 --fine-tune-epochs 2`. Se usaron 21 314 imágenes de entrenamiento y
5 323 de validación, con un corte de 80 % y 20 % sobre las clases de los cuatro cultivos.

| | Precisión de validación |
|---|---|
| Modelo Keras (float32) | 93,9 % |
| `plant_disease.tflite` cuantizado (2,44 MB) | **93,6 %** |
| Solo fotos con confianza ≥ 0,6, que es el umbral de la app | 96,3 % (cubre el 94,5 % de las fotos) |

Las clases más débiles, con menos del 80 % de aciertos, son: maíz con
*Cercospora* (74 %), tomate con tizón temprano (76 %) y papa sana (77 %). Las
tres tienen pocas imágenes en el dataset o se parecen a otra clase del mismo
cultivo. El resto supera el 87 %.

## Datos y licencias

- **Dataset:** PlantVillage, versión de `tensorflow_datasets` `plant_village` 1.0.2.
  Descarga "Data for: Identification of Plant Leaf Diseases Using a 9-layer Deep
  Convolutional Neural Network" (Arun Pandian J. y Geetharamani Gopal, Mendeley
  Data, DOI [10.17632/tywbtsjrjv.1](https://doi.org/10.17632/tywbtsjrjv.1)),
  publicada en dominio público (CC0).
- **Pesos base:** MobileNetV2 preentrenado en ImageNet, los oficiales de
  `keras.applications` (Apache 2.0).

## Limitaciones

- PlantVillage son fotos de laboratorio: una hoja sola, centrada y sobre fondo
  liso. Con fotos tomadas en el campo la precisión va a ser menor que la de
  validación, así que no hay que tomar ese número como precisión real.
- Si la foto no es de una hoja de estos cuatro cultivos, el modelo igual elige
  una de las 19 clases. Por eso la app descarta los resultados con poca confianza.
- Los nombres de `labels.txt` son los de PlantVillage, en inglés. La app los
  traduce para mostrarlos, y son los que hay que usar en `enfermedad_tratamiento`
  del backend para que GET `/api/diagnoses/:id/products` encuentre productos.
