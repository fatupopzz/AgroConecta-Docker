package com.uvg.agroconecta.ui.home.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceSearchPermissionTest {

    @Test
    fun `solicita permiso cuando el microfono aun no esta autorizado`() {
        assertEquals(
            VoiceSearchPermissionAction.REQUEST_PERMISSION,
            voiceSearchPermissionAction(isPermissionGranted = false)
        )
    }

    @Test
    fun `inicia reconocimiento cuando el microfono ya esta autorizado`() {
        assertEquals(
            VoiceSearchPermissionAction.START_RECOGNITION,
            voiceSearchPermissionAction(isPermissionGranted = true)
        )
    }
}
