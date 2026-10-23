package com.uvg.agroconecta.ui.navigation

import org.json.JSONObject
import java.util.Base64

/**
 * Decide si al abrir la app se puede entrar directo, sin pasar por el login.
 *
 * Se revisa la fecha de vencimiento del token (el campo `exp` del JWT) en lugar
 * de solo ver si existe: si estuviera vencido, la persona entraría a Inicio y
 * la primera petición la sacaría de golpe al login, que confunde más que pedir
 * la contraseña desde el principio. Se deja un margen para que no venza a
 * mitad de la primera carga.
 */
internal fun hasUsableSession(
    token: String?,
    tipoUsuario: String?,
    nowEpochSeconds: Long,
    marginSeconds: Long = SESSION_MARGIN_SECONDS
): Boolean {
    if (token.isNullOrBlank() || tipoUsuario.isNullOrBlank()) return false
    val expiresAt = jwtExpirationEpochSeconds(token) ?: return false
    return expiresAt - marginSeconds > nowEpochSeconds
}

internal fun jwtExpirationEpochSeconds(token: String): Long? {
    val payload = token.split(".").getOrNull(1) ?: return null
    return runCatching {
        val json = String(Base64.getUrlDecoder().decode(payload.padBase64()), Charsets.UTF_8)
        JSONObject(json).optLong("exp", -1L).takeIf { it > 0 }
    }.getOrNull()
}

// Los JWT van en base64url sin relleno; el decodificador de Java lo exige.
private fun String.padBase64(): String = this + "=".repeat((4 - length % 4) % 4)

private const val SESSION_MARGIN_SECONDS = 60L
