package com.hotfixcde.motionwall

import android.content.Context
import android.net.Uri

class SettingsManager(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var videoUri: Uri?
        get() = prefs.getString(KEY_VIDEO, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }
        set(value) = prefs.edit().putString(KEY_VIDEO, value?.toString()).apply()

    var audioEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUDIO, false)
        set(value) = prefs.edit().putBoolean(KEY_AUDIO, value).apply()

    // 0 = Auto, 1 = Vertical, 2 = Horizontal.
    var orientationMode: Int
        get() = prefs.getInt(KEY_ORIENTATION, 0).coerceIn(0, 2)
        set(value) = prefs.edit().putInt(KEY_ORIENTATION, value.coerceIn(0, 2)).apply()

    companion object {
        const val PREFS = "motionwall"
        const val KEY_VIDEO = "video"
        const val KEY_AUDIO = "sound"
        const val KEY_ORIENTATION = "orientation"
    }
}
