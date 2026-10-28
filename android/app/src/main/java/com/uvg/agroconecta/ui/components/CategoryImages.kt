package com.uvg.agroconecta.ui.components

import java.text.Normalizer

/**
 * Foto genérica por categoría para los productos que todavía no tienen la suya.
 *
 * Vive en assets y no en res/drawable a propósito: si una foto aún no se agregó,
 * Coil simplemente falla al cargarla y se ve la hoja de siempre, en vez de
 * romper el build por una referencia R.drawable que no existe. Para agregar o
 * cambiar una basta con dejar el archivo en assets/categorias/<categoria>.webp.
 */
internal object CategoryImages {

    private val KNOWN_CATEGORIES = setOf(
        "fertilizantes",
        "pesticidas",
        "herbicidas",
        "fungicidas",
        "semillas",
        "herramientas"
    )

    fun assetUriFor(categoria: String?): String? {
        val key = categoria?.let(::normalize) ?: return null
        if (key !in KNOWN_CATEGORIES) return null
        return "file:///android_asset/categorias/$key.webp"
    }

    // El catálogo puede traer "Fertilizantes", "fertilizantes " o "Semíllas";
    // todas deben llegar al mismo archivo.
    private fun normalize(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
}
