package com.uvg.agroconecta.data.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class RecurringOrderModelsTest {
    @Test fun `maps KAN-89 values and instant without changing time zone`() {
        val dto = Gson().fromJson(
            """{"id":4,"id_usuario":7,"frecuencia":"quincenal","productos":[{"id_inventario":9,"cantidad":2}],"id_distribuidor":3,"direccion_entrega":"Parcela norte","metodo_pago":"contra_entrega","fecha_proximo":"2026-10-01T12:00:00.000Z","estado":"pausado"}""",
            RecurringOrderDto::class.java
        )
        val order = dto.toDomain()
        assertEquals(RecurringFrequency.BIWEEKLY, order.frequency)
        assertEquals(RecurringStatus.PAUSED, order.status)
        assertEquals(Instant.parse("2026-10-01T12:00:00Z"), order.nextAt)
        assertEquals("2026-10-01 06:00", RecurringDates.display(order.nextAt, ZoneId.of("America/Guatemala")))
        assertEquals(order.nextAt, RecurringDates.parseLocal("2026-10-01 06:00", ZoneId.of("America/Guatemala")))
    }

    @Test fun `request serializes exact backend names`() {
        val json = Gson().toJson(CreateRecurringOrderRequest(
            "semanal", listOf(RecurringProductDto(8, 2)), 3, "Parcela norte",
            nextDate = "2026-10-01T12:00:00Z"
        ))
        org.junit.Assert.assertTrue(json.contains("\"id_inventario\":8"))
        org.junit.Assert.assertTrue(json.contains("\"fecha_proximo\""))
        org.junit.Assert.assertTrue(json.contains("\"metodo_pago\":\"contra_entrega\""))
    }
}
