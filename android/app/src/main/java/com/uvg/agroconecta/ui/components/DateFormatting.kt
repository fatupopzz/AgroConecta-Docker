package com.uvg.agroconecta.ui.components

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val SPANISH = Locale("es", "GT")
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", SPANISH)
private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM, h:mm a", SPANISH)

/**
 * Convierte la fecha ISO del backend ("2026-10-15T08:30:00Z") en algo que se
 * lee de corrido: "15 de octubre 2026". Si el formato no es el esperado se
 * devuelve tal cual; es preferible una fecha fea a ocultarla.
 */
fun formatFriendlyDate(isoDate: String?): String? {
    val raw = isoDate?.takeIf { it.length >= 10 } ?: return isoDate
    return try {
        LocalDate.parse(raw.take(10)).format(SHORT_DATE)
    } catch (_: DateTimeParseException) {
        raw
    }
}

/**
 * Fecha y hora para el seguimiento ("15 de octubre, 2:30 p. m."). Si el
 * backend manda zona horaria se pasa a la del teléfono; antes se mostraba la
 * hora UTC tal cual y parecía que el pedido se movía seis horas antes.
 */
fun formatFriendlyDateTime(isoDateTime: String?): String? {
    if (isoDateTime.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(isoDateTime)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DATE_TIME)
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(isoDateTime.take(19)).format(DATE_TIME)
        } catch (_: DateTimeParseException) {
            isoDateTime
        }
    }
}
