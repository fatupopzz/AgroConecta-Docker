package com.uvg.agroconecta.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PestAlertPushPayloadTest {

    @Test
    fun `parses nearby pest alert data and builds notification copy`() {
        val payload = PestAlertPushPayload.from(
            mapOf(
                "type" to "pest_alert",
                "alert_id" to "29",
                "pest_type" to "mosca_blanca",
                "crop" to "Tomate",
                "distance_km" to "3.44"
            )
        )

        requireNotNull(payload)
        assertEquals(29, payload.alertId)
        assertEquals("Nueva alerta de mosca blanca", payload.notificationTitle())
        assertEquals(
            "Se reportó mosca blanca en Tomate a 3.4 km.",
            payload.notificationBody()
        )
    }

    @Test
    fun `accepts backend spanish keys and message overrides`() {
        val payload = PestAlertPushPayload.from(
            data = mapOf(
                "tipo" to "alerta_plaga",
                "id_alerta" to "41",
                "tipo_plaga" to "pulgon",
                "cultivo_afectado" to "Frijol",
                "distancia_km" to "0.75",
                "titulo" to "Plaga cerca de ti",
                "mensaje" to "Revisa los cultivos de tu zona"
            )
        )

        requireNotNull(payload)
        assertEquals("Plaga cerca de ti", payload.notificationTitle())
        assertEquals("Revisa los cultivos de tu zona", payload.notificationBody())
    }

    @Test
    fun `rejects unrelated or incomplete push data`() {
        assertNull(
            PestAlertPushPayload.from(
                mapOf(
                    "type" to "order_update",
                    "alert_id" to "29",
                    "pest_type" to "pulgon",
                    "crop" to "Frijol"
                )
            )
        )
        assertNull(
            PestAlertPushPayload.from(
                mapOf(
                    "type" to "pest_alert",
                    "pest_type" to "pulgon",
                    "crop" to "Frijol"
                )
            )
        )
    }
}
