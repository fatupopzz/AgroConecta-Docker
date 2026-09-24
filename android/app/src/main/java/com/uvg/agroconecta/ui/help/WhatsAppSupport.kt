package com.uvg.agroconecta.ui.help

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

internal const val DEFAULT_SUPPORT_MESSAGE =
    "Hola, necesito ayuda con AgroConecta."

internal fun buildWhatsAppSupportUrl(
    phoneNumber: String,
    message: String = DEFAULT_SUPPORT_MESSAGE
): String {
    val normalizedPhone = phoneNumber.filter(Char::isDigit)
    val recipient = if (normalizedPhone.isBlank()) "" else normalizedPhone
    val encodedMessage = URLEncoder.encode(message, "UTF-8").replace("+", "%20")

    return "https://wa.me/$recipient?text=$encodedMessage"
}

internal fun openWhatsAppSupport(
    context: Context,
    phoneNumber: String
): Boolean = runCatching {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(buildWhatsAppSupportUrl(phoneNumber))
    ).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    context.startActivity(intent)
}.isSuccess
