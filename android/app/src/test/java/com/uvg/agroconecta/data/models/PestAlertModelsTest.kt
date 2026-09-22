package com.uvg.agroconecta.data.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PestAlertModelsTest {

    private val gson = Gson()

    @Test
    fun `deserializes nearby alert with map and detail data`() {
        val response = gson.fromJson(
            """
                {
                  "alertas": [
                    {
                      "id_alerta": 29,
                      "tipo_plaga": "pulgon",
                      "cultivo_afectado": "Frijol",
                      "distancia_km": 3.4,
                      "latitud": 14.6349,
                      "longitud": -90.5069,
                      "fecha_reporte": "2026-09-21T15:30:00Z",
                      "activa": true,
                      "severidad": "alta",
                      "nombre_usuario": "Ana"
                    }
                  ]
                }
            """.trimIndent(),
            NearbyPestAlertsResponse::class.java
        )

        val alert = response.alertas.single()
        assertEquals(29, alert.id)
        assertEquals("pulgon", alert.pestType)
        assertEquals("Frijol", alert.cultivo)
        assertEquals(3.4, alert.distanceKm, 0.0)
        assertEquals(14.6349, alert.latitud, 0.0)
        assertEquals(-90.5069, alert.longitud, 0.0)
        assertTrue(alert.activa)
        assertEquals("alta", alert.severidad)
        assertEquals("Ana", alert.reporterName)
    }

    @Test
    fun `serializes report using backend field names`() {
        val request = PestAlertReportRequest(
            pestType = PestType.FALL_ARMYWORM.apiValue,
            cultivo = "Maíz",
            latitud = 14.6211,
            longitud = -90.5270
        )

        val json = gson.toJsonTree(request).asJsonObject

        assertEquals("gusano_cogollero", json.get("tipo_plaga").asString)
        assertEquals("Maíz", json.get("cultivo_afectado").asString)
        assertEquals(14.6211, json.get("latitud").asDouble, 0.0)
        assertEquals(-90.5270, json.get("longitud").asDouble, 0.0)
        assertFalse(json.has("distanceKm"))
        assertFalse(json.has("distancia_km"))
        assertFalse(json.has("cultivo"))
    }

    @Test
    fun `exposes stable pest values and localized labels`() {
        assertEquals("pulgon", PestType.APHID.apiValue)
        assertEquals("Pulgón", PestType.APHID.displayName)
        assertEquals("otra", PestType.OTHER.apiValue)
        assertEquals(7, PestType.entries.size)
    }
}
