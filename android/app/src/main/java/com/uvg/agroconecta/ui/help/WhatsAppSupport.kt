package com.uvg.agroconecta.ui.help

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

internal const val DEFAULT_SUPPORT_MESSAGE =
    "Hola, necesito ayuda con AgroConecta."
internal const val WHATSAPP_PACKAGE_NAME = "com.whatsapp"

internal sealed interface WhatsAppSupportTarget {
    data class DirectConversation(val url: String) : WhatsAppSupportTarget
    data class ContactPicker(val message: String) : WhatsAppSupportTarget
}

internal fun buildWhatsAppSupportUrl(
    phoneNumber: String,
    message: String = DEFAULT_SUPPORT_MESSAGE
): String {
    val normalizedPhone = phoneNumber.filter(Char::isDigit)
    require(normalizedPhone.isNotBlank()) {
        "A support phone number is required to build a WhatsApp URL."
    }
    val encodedMessage = URLEncoder.encode(message, "UTF-8").replace("+", "%20")

    return "https://wa.me/$normalizedPhone?text=$encodedMessage"
}

internal fun resolveWhatsAppSupportTarget(
    phoneNumber: String,
    message: String = DEFAULT_SUPPORT_MESSAGE
): WhatsAppSupportTarget {
    val normalizedPhone = phoneNumber.filter(Char::isDigit)

    return if (normalizedPhone.isBlank()) {
        WhatsAppSupportTarget.ContactPicker(message)
    } else {
        WhatsAppSupportTarget.DirectConversation(
            buildWhatsAppSupportUrl(normalizedPhone, message)
        )
    }
}

internal fun buildWhatsAppSupportIntent(
    phoneNumber: String,
    message: String = DEFAULT_SUPPORT_MESSAGE
): Intent {
    return when (val target = resolveWhatsAppSupportTarget(phoneNumber, message)) {
        is WhatsAppSupportTarget.ContactPicker -> {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(WHATSAPP_PACKAGE_NAME)
                putExtra(Intent.EXTRA_TEXT, target.message)
            }
        }

        is WhatsAppSupportTarget.DirectConversation -> {
            Intent(Intent.ACTION_VIEW, Uri.parse(target.url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
        }
    }
}

internal fun openWhatsAppSupport(
    context: Context,
    phoneNumber: String
): Boolean = runCatching {
    context.startActivity(buildWhatsAppSupportIntent(phoneNumber))
}.isSuccess
