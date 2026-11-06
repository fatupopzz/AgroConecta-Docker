package com.uvg.agroconecta.ui.auth

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginLayoutTest {

    @Test
    fun `con el teclado abierto la marca cede su espacio`() {
        assertTrue(isCompactLoginLayout(520.dp))
    }

    @Test
    fun `con la pantalla completa se muestra la marca`() {
        assertFalse(isCompactLoginLayout(600.dp))
        assertFalse(isCompactLoginLayout(840.dp))
    }
}
