package com.uvg.agroconecta.ui.orders

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object OrderPdfOpener {
    fun intent(uri: Uri): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun open(context: Context, uri: Uri): Boolean = try {
        context.startActivity(intent(uri))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
