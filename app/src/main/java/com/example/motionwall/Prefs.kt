package com.example.motionwall

object Keys {
    const val PREFS = "motion_wall_prefs"
    const val VIDEO_URI = "video_uri"
    const val SOUND_ENABLED = "sound_enabled"
    const val FIT_MODE = "fit_mode"
}

/**
 * How the video fills the wallpaper surface.
 * - AUTO:    Fill the whole screen, keep aspect ratio, crop top/bottom
 *            OR left/right edges (whichever is smaller). This is the
 *            default desktop-wallpaper behavior.
 * - VERTICAL:  Fill the screen height, crop the left/right edges
 *            (video's top and bottom are always fully visible).
 * - HORIZONTAL: Fill the screen width, crop the top/bottom edges
 *            (video's left and right are always fully visible).
 * - CROP:    Legacy center-crop (same as AUTO for most phones).
 */
enum class FitMode { AUTO, VERTICAL, HORIZONTAL, CROP }

