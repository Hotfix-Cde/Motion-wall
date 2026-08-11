package com.example.motionwall

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object Keys {
    const val PREFS = "motion_wall_prefs"
    const val VIDEO_URI = "video_uri"
    const val SOUND_ENABLED = "sound_enabled"
    const val FIT_MODE = "fit_mode"
    const val APP_THEME = "app_theme"
}

/** How the video fills the wallpaper surface. */
enum class FitMode { AUTO, VERTICAL, HORIZONTAL, CROP }

/**
 * Small, predictable app appearance options. This controls MotionWall only;
 * it never changes the device-wide Android theme.
 */
enum class AppTheme {
    SYSTEM,
    DARK,
    LIGHT;

    fun nightMode(): Int = when (this) {
        SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    }

    companion object {
        fun fromStored(value: String?): AppTheme =
            runCatching { valueOf(value ?: SYSTEM.name) }.getOrDefault(SYSTEM)

        fun apply(context: Context) {
            val prefs = context.getSharedPreferences(Keys.PREFS, Context.MODE_PRIVATE)
            AppCompatDelegate.setDefaultNightMode(
                fromStored(prefs.getString(Keys.APP_THEME, SYSTEM.name)).nightMode()
            )
        }
    }
}
