package com.uvg.agroconecta.data.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistributorResponseTest {

    private val gson = Gson()

    @Test
    fun `converts distributor rating response from backend`() {
        val json = """
            {
              "id_distribuidor": 1,
              "nombre_negocio": "Agro Distribuciones SA",
              "departamento": "Guatemala",
              "estado_verificacion": "verificado",
              "calificacion_promedio": 4.5,
              "cantidad_resenas": 2,
              "nombre": "Agro",
              "email": "agro@example.com",
              "telefono": "55550000"
            }
        """.trimIndent()

        val distributor = gson.fromJson(json, Distributor::class.java)

        assertEquals(4.5, distributor.calificacion ?: 0.0, 0.0)
        assertEquals(2, distributor.cantidadResenas ?: -1)
    }

    @Test
    fun `accepts null or absent rating fields`() {
        val distributor = gson.fromJson(
            """
                {
                  "id_distribuidor": 8,
                  "nombre_negocio": "Sin reseñas",
                  "departamento": null,
                  "estado_verificacion": "verificado",
                  "calificacion_promedio": null,
                  "nombre": null,
                  "email": null,
                  "telefono": null
                }
            """.trimIndent(),
            Distributor::class.java
        )

        assertNull(distributor.calificacion)
        assertNull(distributor.cantidadResenas)
    }
}
