package com.uvg.agroconecta.notifications

import android.content.Context

internal object PestAlertInstallationStore {
    private const val PREFERENCES_NAME = "pest_alert_push"
    private const val INSTALLATION_ID_KEY = "firebase_installation_id"

    fun save(context: Context, installationId: String) {
        if (installationId.isBlank()) return
        preferences(context)
            .edit()
            .putString(INSTALLATION_ID_KEY, installationId)
            .apply()
    }

    fun get(context: Context): String? = preferences(context)
        .getString(INSTALLATION_ID_KEY, null)
        ?.takeIf(String::isNotBlank)

    fun clear(context: Context) {
        preferences(context)
            .edit()
            .remove(INSTALLATION_ID_KEY)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
