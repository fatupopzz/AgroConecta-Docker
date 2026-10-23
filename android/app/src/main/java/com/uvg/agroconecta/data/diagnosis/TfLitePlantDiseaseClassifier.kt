package com.uvg.agroconecta.data.diagnosis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TfLitePlantDiseaseClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : PlantDiseaseClassifier {

    // El intérprete no es seguro entre hilos: el mutex evita que dos fotos
    // seguidas usen el mismo buffer de salida a la vez.
    private val mutex = Mutex()
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    override suspend fun classify(jpegBytes: ByteArray): PlantDiagnosisResult =
        withContext(Dispatchers.Default) {
            val input = PlantImagePreprocessing.toModelInput(readSquarePixels(jpegBytes))
            mutex.withLock {
                val model = interpreter ?: load()
                val output = Array(1) { FloatArray(labels.size) }
                model.run(input, output)
                PlantDiagnosisRules.interpret(output[0], labels)
            }
        }

    private fun readSquarePixels(jpegBytes: ByteArray): IntArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "No se pudo leer la imagen" }

        val options = BitmapFactory.Options().apply {
            inSampleSize = PlantImagePreprocessing.sampleSizeFor(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)
            ?: throw IllegalArgumentException("No se pudo leer la imagen")

        val size = PlantImagePreprocessing.INPUT_SIZE
        val square = PlantImagePreprocessing.centerSquare(decoded.width, decoded.height)
        val cropped = Bitmap.createBitmap(decoded, square.left, square.top, square.side, square.side)
        // filter = true: bilineal, el mismo filtro de tf.image.resize con el que se entrenó.
        val scaled = Bitmap.createScaledBitmap(cropped, size, size, true)
        return IntArray(size * size).also { pixels ->
            scaled.getPixels(pixels, 0, size, 0, 0, size, size)
            listOf(decoded, cropped, scaled).distinct().forEach(Bitmap::recycle)
        }
    }

    private fun load(): Interpreter {
        labels = context.assets.open(LABELS_PATH).bufferedReader().useLines { lines ->
            lines.map(String::trim).filter(String::isNotEmpty).toList()
        }
        val loaded = Interpreter(mapModel(), Interpreter.Options().setNumThreads(NUM_THREADS))
        val outputClasses = loaded.getOutputTensor(0).shape().last()
        check(outputClasses == labels.size) {
            "El modelo tiene $outputClasses clases pero $LABELS_PATH tiene ${labels.size}"
        }
        interpreter = loaded
        return loaded
    }

    // Se mapea directo desde el APK en lugar de copiarlo a memoria; por eso el
    // build deja los .tflite sin comprimir (androidResources.noCompress).
    private fun mapModel(): MappedByteBuffer =
        context.assets.openFd(MODEL_PATH).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength)
            }
        }

    private companion object {
        const val MODEL_PATH = "ml/plant_disease.tflite"
        const val LABELS_PATH = "ml/labels.txt"
        // Dos hilos alcanzan para una sola foto y no compiten con la interfaz.
        const val NUM_THREADS = 2
    }
}
