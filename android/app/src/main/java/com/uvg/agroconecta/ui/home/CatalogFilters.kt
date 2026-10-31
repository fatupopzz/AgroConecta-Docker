package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.data.models.Product

/**
 * Filtro de precio y marca sobre lo que ya llegó del servidor. El backend solo
 * filtra por nombre y categoría; sin esto el panel de filtros no hacía nada
 * mientras había conexión y la persona creía que la app estaba rota.
 *
 * Un producto sin precio (agotado) se deja fuera si se pidió un rango, porque
 * no se puede saber si cae dentro.
 */
internal fun applyLocalFilters(
    products: List<Product>,
    minPrice: Int?,
    maxPrice: Int?,
    brand: String
): List<Product> {
    val wantedBrand = brand.trim()
    if (minPrice == null && maxPrice == null && wantedBrand.isEmpty()) return products

    return products.filter { product ->
        val price = product.precioDesde
        val matchesMin = minPrice == null || (price != null && price >= minPrice)
        val matchesMax = maxPrice == null || (price != null && price <= maxPrice)
        val matchesBrand = wantedBrand.isEmpty() ||
            product.marca.orEmpty().contains(wantedBrand, ignoreCase = true)
        matchesMin && matchesMax && matchesBrand
    }
}

/** Texto corto de los filtros activos para mostrarlos como chips quitables. */
internal fun activeFilterLabels(minPrice: Int?, maxPrice: Int?, brand: String): List<String> =
    buildList {
        when {
            minPrice != null && maxPrice != null -> add("Q$minPrice – Q$maxPrice")
            minPrice != null -> add("Desde Q$minPrice")
            maxPrice != null -> add("Hasta Q$maxPrice")
        }
        brand.trim().takeIf { it.isNotEmpty() }?.let { add("Marca: $it") }
    }
