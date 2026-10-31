package com.uvg.agroconecta.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

// Robolectric porque org.json solo existe completo dentro de Android.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SessionStartTest {

    private val now = 1_800_000_000L

    @Test
    fun `reads expiration from a jwt payload`() {
        assertEquals(now + 3_600, jwtExpirationEpochSeconds(token(exp = now + 3_600)))
    }

    @Test
    fun `valid token with user type opens the app directly`() {
        assertTrue(hasUsableSession(token(exp = now + 3_600), "agricultor", now))
    }

    @Test
    fun `expired or about to expire token asks for login`() {
        assertFalse(hasUsableSession(token(exp = now - 10), "agricultor", now))
        assertFalse(hasUsableSession(token(exp = now + 30), "agricultor", now))
    }

    @Test
    fun `missing data asks for login`() {
        assertFalse(hasUsableSession(null, "agricultor", now))
        assertFalse(hasUsableSession(token(exp = now + 3_600), null, now))
        assertFalse(hasUsableSession("no-es-un-jwt", "agricultor", now))
        assertNull(jwtExpirationEpochSeconds("a.b.c"))
    }

    private fun token(exp: Long): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256"}""".toByteArray())
        val payload = encoder.encodeToString("""{"id":5,"exp":$exp}""".toByteArray())
        return "$header.$payload.firma"
    }
}
