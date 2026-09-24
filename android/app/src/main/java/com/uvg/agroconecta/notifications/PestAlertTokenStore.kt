package com.uvg.agroconecta.notifications

import android.content.Context

internal object PestAlertTokenStore {
    private const val PREFERENCES_NAME = "pest_alert_push"
    private const val TOKEN_KEY = "fcm_registration_token"

    fun save(context: Context, token: String) {
        if (token.isBlank()) return
        preferences(context).edit().putString(TOKEN_KEY, token).apply()
    }

    fun get(context: Context): String? = preferences(context)
        .getString(TOKEN_KEY, null)
        ?.takeIf(String::isNotBlank)

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
