package com.uvg.agroconecta.data.images

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Las mismas reglas que valida el backend en POST /api/products/:id/image.
 * Se aplican al elegir la foto, antes de crear el producto, para no enterarse
 * del problema cuando ya quedó publicado sin imagen.
 */
object ProductImageRules {
    const val MAX_BYTES: Int = 5 * 1024 * 1024

    val EXTENSIONS_BY_MIME_TYPE: Map<String, String> = mapOf(
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp"
    )

    const val TOO_LARGE_MESSAGE = "La imagen supera el máximo de 5 MB"
    const val UNREADABLE_MESSAGE = "No se pudo usar esa imagen. Prueba con otra foto"

    /**
     * Motivo legible de un rechazo de POST /products/:id/image. Lo usan publicar
     * y "Mis productos" para que el mismo error se explique igual en las dos.
     */
    fun uploadFailureReason(httpCode: Int): String = when (httpCode) {
        413 -> "supera el máximo de 5 MB"
        400 -> "el formato no es válido"
        403 -> "no tienes permiso sobre este producto"
        else -> "el servidor respondió $httpCode"
    }
}

/**
 * Foto ya leída en memoria. Se guardan los bytes al momento de elegirla porque
 * el permiso temporal que da el PhotoPicker sobre la Uri puede vencer antes de
 * que terminen de crearse el producto y el inventario. La Uri se conserva solo
 * para la vista previa.
 *
 * No es data class a propósito: comparar arreglos de varios MB en cada
 * emisión del StateFlow no aporta nada.
 */
class SelectedProductImage(
    val previewUri: Uri,
    val bytes: ByteArray,
    val mimeType: String
) {
    fun toMultipartPart(): MultipartBody.Part {
        val extension = ProductImageRules.EXTENSIONS_BY_MIME_TYPE[mimeType] ?: "jpg"
        // El nombre lo descarta el backend (guarda con un UUID), pero multer
        // necesita un filename para tratar la parte como archivo.
        return MultipartBody.Part.createFormData(
            name = "imagen",
            filename = "producto.$extension",
            body = bytes.toRequestBody(mimeType.toMediaType())
        )
    }
}

sealed interface ProductImageReadResult {
    class Success(val image: SelectedProductImage) : ProductImageReadResult
    data class Invalid(val message: String) : ProductImageReadResult
}

/**
 * Separa la lectura del ContentResolver del ViewModel para poder probar el flujo
 * de publicación sin Context ni Robolectric.
 */
interface ProductImageReader {
    suspend fun read(uri: Uri): ProductImageReadResult
}
