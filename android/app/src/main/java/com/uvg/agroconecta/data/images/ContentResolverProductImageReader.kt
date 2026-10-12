package com.uvg.agroconecta.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lee la foto elegida y la deja lista para subir: reducida a [ImageScaling.MAX_SIDE_PX],
 * derecha según la orientación EXIF y recodificada como JPEG.
 *
 * Recodificar siempre, aunque la foto ya cumpla, resuelve tres cosas a la vez:
 * las fotos de 8-12 MB de un celular pasan holgadas el límite de 5 MB, las
 * HEIC (que el backend no acepta) llegan como JPEG, y se descartan los
 * metadatos EXIF, entre ellos la ubicación GPS de donde se tomó la foto, que
 * de otro modo quedaría pública en /uploads.
 */
@Singleton
class ContentResolverProductImageReader @Inject constructor(
    @ApplicationContext private val context: Context
) : ProductImageReader {

    override suspend fun read(uri: Uri): ProductImageReadResult = withContext(Dispatchers.IO) {
        // La Uri puede venir de un proveedor externo que ya no la expone o que
        // revocó el permiso; eso no debe tumbar la pantalla.
        val bytes: ByteArray? = try {
            encodeForUpload(uri)
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: OutOfMemoryError) {
            // El submuestreo lo evita casi siempre, pero un teléfono con poca
            // memoria y una foto enorme puede llegar aquí.
            null
        }

        if (bytes == null) {
            return@withContext ProductImageReadResult.Invalid(ProductImageRules.UNREADABLE_MESSAGE)
        }
        if (bytes.size > ProductImageRules.MAX_BYTES) {
            return@withContext ProductImageReadResult.Invalid(ProductImageRules.TOO_LARGE_MESSAGE)
        }

        ProductImageReadResult.Success(
            SelectedProductImage(previewUri = uri, bytes = bytes, mimeType = JPEG_MIME_TYPE)
        )
    }

    private fun encodeForUpload(uri: Uri): ByteArray? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageScaling.sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        // La orientación es un extra: si el formato no trae EXIF legible (un PNG
        // en Android 8, por ejemplo) la foto se usa tal cual en vez de rechazarla.
        val rotation = try {
            resolver.openInputStream(uri)?.use { readRotationDegrees(it) } ?: 0
        } catch (_: IOException) {
            0
        }
        val ready = flattenOnWhite(rotate(scaleDown(decoded), rotation))

        return ByteArrayOutputStream().use { output ->
            ready.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            ready.recycle()
            output.toByteArray()
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val (width, height) = ImageScaling.targetSize(bitmap.width, bitmap.height)
        if (width == bitmap.width && height == bitmap.height) return bitmap
        return Bitmap.createScaledBitmap(bitmap, width, height, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    // BitmapFactory ignora la orientación EXIF; sin esto una foto tomada con
    // el teléfono vertical se vería acostada en el catálogo.
    private fun readRotationDegrees(input: InputStream): Int =
        when (
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    // JPEG no tiene transparencia: un PNG con fondo transparente (un logo, por
    // ejemplo) quedaría con fondo negro si no se pinta sobre blanco primero.
    private fun flattenOnWhite(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap
        val flattened = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        bitmap.recycle()
        return flattened
    }

    private companion object {
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val JPEG_QUALITY = 85
    }
}
