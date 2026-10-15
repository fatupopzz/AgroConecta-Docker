package com.uvg.agroconecta.ui.components

import com.uvg.agroconecta.data.api.ApiClientFactory
import com.uvg.agroconecta.data.api.ProductImageUrl

/**
 * Decide qué imagen pide [ProductImage]. Las tarjetas (catálogo, inicio,
 * carrito, favoritos) mandan la miniatura de 400 px porque bajar la foto
 * original de hasta 5 MB para un recuadro de 130 dp gasta datos y memoria sin
 * que se note la diferencia. El detalle no la manda y sigue con la foto grande.
 *
 * El orden de respaldo es miniatura, foto original y foto de la categoría: las
 * fotos subidas antes de las miniaturas no tienen la suya hasta que se corre
 * el script del backend, y no deben quedarse con la genérica mientras tanto.
 */
internal object ProductImageSource {

    fun pick(
        imagenThumbUrl: String?,
        imagenUrl: String?,
        categoria: String?,
        apiBaseUrl: String = ApiClientFactory.baseUrl
    ): String? =
        ProductImageUrl.resolve(imagenThumbUrl, apiBaseUrl)
            ?: ProductImageUrl.resolve(imagenUrl, apiBaseUrl)
            ?: CategoryImages.assetUriFor(categoria)
}
