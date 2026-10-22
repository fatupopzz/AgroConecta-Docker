package com.uvg.agroconecta.ui.home.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class VoiceSearchRecognizerManagerTest {

    @Test
    fun `crea una sola sesion mientras la pantalla permanece activa`() {
        var creations = 0
        val session = FakeVoiceSearchRecognizerSession()
        val manager = VoiceSearchRecognizerManager {
            creations += 1
            session
        }

        assertSame(session, manager.getOrCreate())
        assertSame(session, manager.getOrCreate())
        assertEquals(1, creations)
    }

    @Test
    fun `destruye la sesion creada una sola vez al liberar la pantalla`() {
        val session = FakeVoiceSearchRecognizerSession()
        val manager = VoiceSearchRecognizerManager { session }

        manager.getOrCreate()
        manager.destroy()
        manager.destroy()

        assertEquals(1, session.destroyCalls)
    }

    @Test
    fun `no crea una sesion si la pantalla se libera antes de escuchar`() {
        var creations = 0
        val manager = VoiceSearchRecognizerManager {
            creations += 1
            FakeVoiceSearchRecognizerSession()
        }

        manager.destroy()

        assertEquals(0, creations)
    }

    @Test
    fun `inicia escucha reutilizando la sesion existente`() {
        val session = FakeVoiceSearchRecognizerSession()
        val manager = VoiceSearchRecognizerManager { session }

        manager.startListening()
        manager.startListening()

        assertEquals(2, session.startCalls)
    }

    @Test
    fun `crea una sesion nueva despues de liberar la anterior`() {
        val sessions = mutableListOf<FakeVoiceSearchRecognizerSession>()
        val manager = VoiceSearchRecognizerManager {
            FakeVoiceSearchRecognizerSession().also(sessions::add)
        }

        val firstSession = manager.getOrCreate()
        manager.destroy()
        val secondSession = manager.getOrCreate()

        assertEquals(2, sessions.size)
        assertEquals(1, sessions.first().destroyCalls)
        assertNotSame(firstSession, secondSession)
    }

    private class FakeVoiceSearchRecognizerSession : VoiceSearchRecognizerSession {
        var startCalls = 0
        var destroyCalls = 0

        override fun startListening() {
            startCalls += 1
        }

        override fun destroy() {
            destroyCalls += 1
        }
    }
}
