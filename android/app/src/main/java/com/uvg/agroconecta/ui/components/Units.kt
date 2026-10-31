package com.uvg.agroconecta.ui.components

private val ACCENTED_ENDINGS = mapOf("ón" to "ones", "án" to "anes", "én" to "enes", "ín" to "ines", "ún" to "unes")

/**
 * Cantidad con su unidad bien escrita: "1 saco", "40 sacos", "3 galones".
 *
 * El distribuidor escribe la unidad a mano y casi siempre en singular, así
 * que antes salía "40 saco". Las abreviaturas (kg, L, lb, qq) no se tocan
 * porque en plural se escriben igual.
 */
fun formatQuantityWithUnit(quantity: Int, unit: String?): String {
    val clean = unit?.trim().orEmpty()
    if (clean.isEmpty()) return if (quantity == 1) "1 unidad" else "$quantity unidades"
    return "$quantity ${if (quantity == 1) clean else pluralizeUnit(clean)}"
}

internal fun pluralizeUnit(unit: String): String {
    val lower = unit.lowercase()
    val isAbbreviation = unit.length <= 3 || unit.endsWith(".")
    return when {
        isAbbreviation -> unit
        lower.endsWith("s") -> unit
        ACCENTED_ENDINGS.keys.any { lower.endsWith(it) } -> {
            val ending = ACCENTED_ENDINGS.keys.first { lower.endsWith(it) }
            unit.dropLast(ending.length) + ACCENTED_ENDINGS.getValue(ending)
        }
        lower.last() in "aeiouáéó" -> unit + "s"
        lower.endsWith("z") -> unit.dropLast(1) + "ces"
        else -> unit + "es"
    }
}
