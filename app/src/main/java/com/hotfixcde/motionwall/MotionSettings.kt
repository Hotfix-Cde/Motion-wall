package com.hotfixcde.motionwall

import android.content.Context
import android.net.Uri

enum class ScaleMode {
    CROP,
    FIT
}

enum class OrientationMode {
    AUTO,
    VERTICAL,
    HORIZONTAL
}

data class MotionSettings(
    val videoUri: Uri? = null,
    val soundEnabled: Boolean = false,
    val scaleMode: ScaleMode = ScaleMode.CROP,
    val orientationMode: OrientationMode = OrientationMode.AUTO,
)

object MotionSettingsStore {
    const val PREFS_NAME = "motionwall"
    const val KEY_VIDEO = "video"
    const val KEY_SOUND = "sound"
    const val KEY_SCALE = "scale"
    const val KEY_ORIENTATION = "orientation"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): MotionSettings {
        val sharedPreferences = prefs(context)
        val uri = sharedPreferences.getString(KEY_VIDEO, null)?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val soundEnabled = sharedPreferences.getBoolean(KEY_SOUND, false)
        val scaleMode = when (sharedPreferences.getInt(KEY_SCALE, 0)) {
            1 -> ScaleMode.FIT
            else -> ScaleMode.CROP
        }
        val orientationMode = when (sharedPreferences.getInt(KEY_ORIENTATION, 0)) {
            1 -> OrientationMode.VERTICAL
            2 -> OrientationMode.HORIZONTAL
            else -> OrientationMode.AUTO
        }
        return MotionSettings(uri, soundEnabled, scaleMode, orientationMode)
    }

    fun saveVideoUri(context: Context, uri: Uri?) {
        prefs(context).edit().putString(KEY_VIDEO, uri?.toString()).apply()
    }

    fun saveSoundEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun saveScaleMode(context: Context, mode: ScaleMode) {
        prefs(context).edit().putInt(KEY_SCALE, if (mode == ScaleMode.FIT) 1 else 0).apply()
    }

    fun saveOrientationMode(context: Context, mode: OrientationMode) {
        prefs(context).edit().putInt(KEY_ORIENTATION, when (mode) {
            OrientationMode.VERTICAL -> 1
            OrientationMode.HORIZONTAL -> 2
            OrientationMode.AUTO -> 0
        }).apply()
    }
}
