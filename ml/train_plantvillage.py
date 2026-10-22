"""Entrena el clasificador de hojas que usa la app para el diagnóstico de plagas (KAN-110).

Usa PlantVillage (tensorflow_datasets "plant_village", dominio público) filtrado a
maíz, tomate, papa y chile, con transfer learning sobre MobileNetV2 preentrenado en
ImageNet. El resultado se exporta a TFLite con cuantización de rango dinámico.

Uso, desde la raíz del repositorio:
    python ml/train_plantvillage.py
    python ml/train_plantvillage.py --epochs 3 --fine-tune-epochs 2

Ver ml/README.md para preparar el entorno.
"""
import argparse
import json
import pathlib

import numpy as np
import tensorflow as tf
import tensorflow_datasets as tfds

IMAGE_SIZE = 224
# En PlantVillage el chile aparece como "Pepper,_bell".
CROPS = ("Corn", "Tomato", "Potato", "Pepper,_bell")
REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--data-dir", type=pathlib.Path, default=REPO_ROOT / "ml" / ".data")
    parser.add_argument(
        "--output-dir",
        type=pathlib.Path,
        default=REPO_ROOT / "android" / "app" / "src" / "main" / "assets" / "ml",
    )
    parser.add_argument("--epochs", type=int, default=5, help="Épocas con MobileNetV2 congelado.")
    parser.add_argument(
        "--fine-tune-epochs",
        type=int,
        default=0,
        help="Épocas extra ajustando las últimas capas de MobileNetV2. En CPU cada una "
        "tarda unas tres veces más que una época con la base congelada.",
    )
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--seed", type=int, default=42)
    return parser.parse_args()


def selected_labels(all_names):
    """Clases de los cuatro cultivos, en orden alfabético para que labels.txt sea estable."""
    return sorted(name for name in all_names if name.split("___")[0] in CROPS)


def load_split(split, data_dir, label_map):
    """Carga un tramo del dataset, deja solo los cultivos elegidos y reasigna las etiquetas.

    tfds baraja los ejemplos al preparar el dataset, así que cortar por porcentaje
    da tramos de entrenamiento y validación con todas las clases.
    """
    dataset = tfds.load("plant_village", split=split, data_dir=str(data_dir), as_supervised=True)
    lookup = tf.constant(label_map, dtype=tf.int64)
    dataset = dataset.map(lambda image, label: (image, tf.gather(lookup, label)))
    return dataset.filter(lambda image, label: label >= 0)


def resize(image, label):
    # Mismo tamaño y tipo que recibe el modelo en el teléfono: píxeles RGB 0-255.
    image = tf.image.resize(image, (IMAGE_SIZE, IMAGE_SIZE))
    return tf.cast(tf.clip_by_value(tf.round(image), 0, 255), tf.uint8), label


def augment(image, label):
    # La aumentación va en el pipeline y no en el modelo para que el .tflite no
    # cargue operaciones que solo sirven durante el entrenamiento.
    image = tf.image.random_flip_left_right(image)
    image = tf.image.random_flip_up_down(image)
    image = tf.image.random_brightness(tf.cast(image, tf.float32), max_delta=25.0)
    image = tf.image.random_contrast(image, 0.8, 1.2)
    return tf.cast(tf.clip_by_value(image, 0, 255), tf.uint8), label


def build_model(num_classes):
    # Entrada uint8 con la normalización dentro del modelo: Android le pasa los
    # bytes RGB de la foto escalada a 224x224, sin convertirlos ni normalizarlos.
    inputs = tf.keras.Input(shape=(IMAGE_SIZE, IMAGE_SIZE, 3), dtype="uint8", name="image")
    x = tf.keras.layers.Rescaling(1.0 / 127.5, offset=-1.0, name="mobilenet_v2_preprocess")(inputs)
    base = tf.keras.applications.MobileNetV2(
        input_shape=(IMAGE_SIZE, IMAGE_SIZE, 3), include_top=False, weights="imagenet", pooling="avg"
    )
    base.trainable = False
    x = base(x, training=False)
    x = tf.keras.layers.Dropout(0.2)(x)
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax", name="probabilities")(x)
    return tf.keras.Model(inputs, outputs, name="plant_disease_classifier"), base


def train(model, base, train_ds, val_ds, args):
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    stop = tf.keras.callbacks.EarlyStopping(
        monitor="val_accuracy", patience=2, restore_best_weights=True
    )
    model.fit(train_ds, validation_data=val_ds, epochs=args.epochs, callbacks=[stop])

    if args.fine_tune_epochs > 0:
        base.trainable = True
        # Solo los últimos bloques: los primeros detectan bordes y texturas
        # genéricas que ya sirven tal cual para hojas.
        for layer in base.layers[:-30]:
            layer.trainable = False
        # BatchNormalization se deja congelada para no romper las estadísticas
        # de ImageNet con lotes chicos.
        for layer in base.layers:
            if isinstance(layer, tf.keras.layers.BatchNormalization):
                layer.trainable = False
        model.compile(
            optimizer=tf.keras.optimizers.Adam(1e-5),
            loss="sparse_categorical_crossentropy",
            metrics=["accuracy"],
        )
        model.fit(
            train_ds, validation_data=val_ds, epochs=args.fine_tune_epochs, callbacks=[stop]
        )


def export_tflite(model, path):
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    # Cuantización de rango dinámico: pesos en int8 y activaciones en float.
    # Baja el modelo a una cuarta parte sin necesitar un dataset de calibración.
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    path.write_bytes(converter.convert())


def tflite_accuracy(path, dataset):
    """Precisión del .tflite ya cuantizado, que es lo que de verdad corre en el teléfono."""
    interpreter = tf.lite.Interpreter(model_path=str(path))
    interpreter.allocate_tensors()
    input_detail = interpreter.get_input_details()[0]
    output_index = interpreter.get_output_details()[0]["index"]
    hits = total = 0
    for images, labels in dataset.unbatch().batch(1):
        interpreter.set_tensor(input_detail["index"], images.numpy().astype(input_detail["dtype"]))
        interpreter.invoke()
        hits += int(np.argmax(interpreter.get_tensor(output_index)[0]) == int(labels[0]))
        total += 1
    return hits / total


def main():
    args = parse_args()
    tf.keras.utils.set_random_seed(args.seed)

    info = tfds.builder("plant_village", data_dir=str(args.data_dir)).info
    all_names = info.features["label"].names
    labels = selected_labels(all_names)
    # -1 marca las clases de otros cultivos para descartarlas.
    label_map = [labels.index(name) if name in labels else -1 for name in all_names]
    print(f"{len(labels)} clases: {labels}")

    train_raw = load_split("train[:80%]", args.data_dir, label_map)
    val_raw = load_split("train[80%:]", args.data_dir, label_map)

    autotune = tf.data.AUTOTUNE
    train_ds = (
        train_raw.map(resize, num_parallel_calls=autotune)
        .shuffle(2048, seed=args.seed)
        .map(augment, num_parallel_calls=autotune)
        .batch(args.batch_size)
        .prefetch(autotune)
    )
    val_ds = val_raw.map(resize, num_parallel_calls=autotune).batch(args.batch_size).cache().prefetch(autotune)

    model, base = build_model(len(labels))
    train(model, base, train_ds, val_ds, args)
    _, keras_accuracy = model.evaluate(val_ds, verbose=0)

    args.output_dir.mkdir(parents=True, exist_ok=True)
    model_path = args.output_dir / "plant_disease.tflite"
    export_tflite(model, model_path)
    (args.output_dir / "labels.txt").write_text("\n".join(labels) + "\n", encoding="utf-8")

    quantized_accuracy = tflite_accuracy(model_path, val_ds)
    summary = {
        "classes": len(labels),
        "val_accuracy_keras": round(float(keras_accuracy), 4),
        "val_accuracy_tflite": round(quantized_accuracy, 4),
        "tflite_size_mb": round(model_path.stat().st_size / 1024 / 1024, 2),
        "epochs": args.epochs,
        "fine_tune_epochs": args.fine_tune_epochs,
    }
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
