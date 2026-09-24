package com.uvg.agroconecta.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.uvg.agroconecta.data.api.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class OrderPdfException(message: String) : IOException(message)

class OrderPdfRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context
) {
    suspend fun download(): Uri = withContext(Dispatchers.IO) {
        val response = api.exportOrderHistoryPdf()
        if (!response.isSuccessful) {
            throw OrderPdfException("No se pudo descargar el PDF (${response.code()}).")
        }
        val body = response.body() ?: throw OrderPdfException("El servidor devolvió un archivo vacío.")
        body.use { pdf ->
            val input = pdf.byteStream()
            val signature = ByteArray(5)
            var bytesRead = 0
            while (bytesRead < signature.size) {
                val count = input.read(signature, bytesRead, signature.size - bytesRead)
                if (count == -1) break
                bytesRead += count
            }
            if (bytesRead != signature.size || !signature.contentEquals("%PDF-".toByteArray())) {
                throw OrderPdfException("El servidor devolvió un PDF vacío o inválido.")
            }

            val filename = "AgroConecta-pedidos-${System.currentTimeMillis()}-${UUID.randomUUID()}.pdf"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw OrderPdfException("No se pudo crear el archivo en Descargas.")
                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        output.write(signature)
                        if (input.copyTo(output) < 95) {
                            throw OrderPdfException("El servidor devolvió un PDF incompleto.")
                        }
                    } ?: throw OrderPdfException("No se pudo guardar el PDF.")
                    val ready = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                    if (resolver.update(uri, ready, null, null) != 1) {
                        throw OrderPdfException("No se pudo finalizar el PDF en Descargas.")
                    }
                    uri
                } catch (error: Exception) {
                    runCatching { resolver.delete(uri, null, null) }
                    throw error
                }
            } else {
                val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: throw OrderPdfException("No se pudo acceder al almacenamiento.")
                val file = File(directory, filename)
                try {
                    file.outputStream().use { output ->
                        output.write(signature)
                        if (input.copyTo(output) < 95) {
                            throw OrderPdfException("El servidor devolvió un PDF incompleto.")
                        }
                    }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (error: Exception) {
                    file.delete()
                    throw error
                }
            }
        }
    }
}
