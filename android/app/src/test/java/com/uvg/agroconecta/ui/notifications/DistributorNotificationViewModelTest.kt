package com.uvg.agroconecta.ui.notifications

import com.google.gson.Gson
import com.uvg.agroconecta.data.models.DistributorNotification
import com.uvg.agroconecta.data.models.DistributorNotificationContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistributorNotificationViewModelTest {

    @Test
    fun parsesUrgentNotificationReturnedByApi() {
        val json = """
            {
              "id_notificacion": 14,
              "tipo": "pedido_urgente",
              "contenido": {
                "mensaje": "Pedido urgente por detección de plaga",
                "agricultor": "Ana López",
                "esUrgente": true,
                "tipoPlaga": "Pulgón"
              },
              "id_pedido": 31,
              "leida": false
            }
        """.trimIndent()

        val notification = Gson().fromJson(json, DistributorNotification::class.java)

        assertEquals(14, notification.id)
        assertEquals(31, notification.idPedido)
        assertEquals("Pulgón", notification.contenido.tipoPlaga)
        assertEquals(true, notification.contenido.esUrgente)
    }

    @Test
    fun selectsFirstUnreadUrgentNotification() {
        val regular = notification(id = 1, type = "nuevo_pedido", read = false)
        val readUrgent = notification(id = 2, type = "pedido_urgente", read = true)
        val unreadUrgent = notification(id = 3, type = "pedido_urgente", read = false)

        val result = latestUnreadUrgentNotification(
            listOf(regular, readUrgent, unreadUrgent)
        )

        assertEquals(unreadUrgent, result)
    }

    @Test
    fun returnsNullWhenThereAreNoUnreadUrgentNotifications() {
        val result = latestUnreadUrgentNotification(
            listOf(notification(id = 1, type = "pedido_urgente", read = true))
        )

        assertNull(result)
    }

    private fun notification(
        id: Int,
        type: String,
        read: Boolean
    ) = DistributorNotification(
        id = id,
        tipo = type,
        contenido = DistributorNotificationContent(),
        leida = read
    )
}
