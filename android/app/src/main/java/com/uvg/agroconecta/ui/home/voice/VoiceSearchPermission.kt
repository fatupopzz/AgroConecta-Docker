package com.uvg.agroconecta.ui.home.voice

internal enum class VoiceSearchPermissionAction {
    REQUEST_PERMISSION,
    START_RECOGNITION
}

internal fun voiceSearchPermissionAction(
    isPermissionGranted: Boolean
): VoiceSearchPermissionAction = if (isPermissionGranted) {
    VoiceSearchPermissionAction.START_RECOGNITION
} else {
    VoiceSearchPermissionAction.REQUEST_PERMISSION
}
