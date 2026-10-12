package com.uvg.agroconecta.data.images

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentResolverProductImageReader @Inject constructor(
    @ApplicationContext private val context: Context
) : ProductImageReader {

    override suspend fun read(uri: Uri): ProductImageReadResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // El PhotoPicker devuelve también HEIC o GIF aunque se pida ImageOnly;
        // el backend los rechazaría, así que se cortan aquí con un mensaje claro.
        val mimeType = resolver.getType(uri)
        if (mimeType == null || mimeType !in ProductImageRules.EXTENSIONS_BY_MIME_TYPE) {
            return@withContext ProductImageReadResult.Invalid(ProductImageRules.INVALID_FORMAT_MESSAGE)
        }

        // La Uri puede venir de un proveedor externo que ya no la expone o que
        // revocó el permiso; eso no debe tumbar la pantalla de publicar.
        val bytes: ByteArray? = try {
            resolver.openInputStream(uri)?.use { readUpTo(it, ProductImageRules.MAX_BYTES + 1) }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }

        if (bytes == null) {
            return@withContext ProductImageReadResult.Invalid(ProductImageRules.UNREADABLE_MESSAGE)
        }
        if (bytes.size > ProductImageRules.MAX_BYTES) {
            return@withContext ProductImageReadResult.Invalid(ProductImageRules.TOO_LARGE_MESSAGE)
        }

        ProductImageReadResult.Success(
            SelectedProductImage(previewUri = uri, bytes = bytes, mimeType = mimeType)
        )
    }

    // Se lee con tope para no cargar en memoria una foto de 40 MB solo para
    // descubrir que pasa del límite; con un byte de más ya se sabe.
    private fun readUpTo(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (output.size() < limit) {
            val read = input.read(buffer, 0, minOf(buffer.size, limit - output.size()))
            if (read == -1) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
