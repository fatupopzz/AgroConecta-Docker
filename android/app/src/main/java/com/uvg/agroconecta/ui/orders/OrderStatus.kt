package com.uvg.agroconecta.ui.orders

import com.uvg.agroconecta.ui.components.TagTone

/**
 * Nombre legible de un estado de pedido. El backend manda identificadores
 * como "en_ruta"; mostrarlos tal cual confundía, sobre todo con el guion bajo.
 */
internal fun orderStatusLabel(estado: String?): String =
    when (estado) {
        null, "" -> "Pendiente"
        "pendiente" -> "Pendiente"
        "confirmado" -> "Confirmado"
        "preparando" -> "Preparando"
        "en_ruta" -> "En camino"
        "entregado" -> "Entregado"
        "cancelado" -> "Cancelado"
        else -> estado.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

/** Tono de la etiqueta de estado: verde al terminar, rojo si se canceló. */
internal fun orderStatusTone(estado: String?): TagTone =
    when (estado) {
        "entregado" -> TagTone.Success
        "cancelado" -> TagTone.Danger
        "en_ruta", "preparando" -> TagTone.Info
        else -> TagTone.Neutral
    }

private val progressSteps = listOf("confirmado", "preparando", "en_ruta", "entregado")

/**
 * Avance del pedido entre 0 y 1 para la barra de la tarjeta. Un pedido
 * pendiente muestra un poquito de avance para que no parezca olvidado.
 */
internal fun orderProgress(estado: String?): Float {
    val index = progressSteps.indexOf(estado)
    return when {
        estado == "cancelado" -> 0f
        index < 0 -> 0.1f
        else -> (index + 1) / progressSteps.size.toFloat()
    }
}

internal enum class OrderFilter(val label: String) {
    ALL("Todos"),
    ACTIVE("En curso"),
    DELIVERED("Entregados");

    fun matches(estado: String?): Boolean = when (this) {
        ALL -> true
        ACTIVE -> estado != "entregado" && estado != "cancelado"
        DELIVERED -> estado == "entregado"
    }
}
