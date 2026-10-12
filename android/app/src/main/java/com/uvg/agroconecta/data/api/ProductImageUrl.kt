package com.uvg.agroconecta.data.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Convierte la `imagen_url` que manda el backend en una URL que Coil pueda pedir.
 *
 * El backend guarda solo la ruta (/uploads/productos/<archivo>) para que los
 * registros no dependan de la IP de la VM. Se completa con el mismo host y
 * puerto que usa Retrofit, así el emulador, la VM y un futuro dominio con
 * HTTPS funcionan sin tocar nada más.
 */
internal object ProductImageUrl {

    fun resolve(
        imagenUrl: String?,
        apiBaseUrl: String = ApiClientFactory.baseUrl
    ): String? {
        val path = imagenUrl?.trim().orEmpty()
        if (path.isEmpty()) return null

        if (path.startsWith("http://") || path.startsWith("https://")) return path

        val base = apiBaseUrl.toHttpUrlOrNull() ?: return null
        // La URL base termina en /api/ pero los archivos se sirven en la raíz.
        // Resolver una ruta absoluta reemplaza ese prefijo; sin la barra inicial
        // quedaría /api/uploads/..., que no existe.
        val absolutePath = if (path.startsWith("/")) path else "/$path"
        return base.resolve(absolutePath)?.toString()
    }
}
