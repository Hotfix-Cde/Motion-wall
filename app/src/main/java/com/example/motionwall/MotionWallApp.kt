package com.example.motionwall

import android.app.Application

class MotionWallApp : Application() {
    override fun onCreate() {
        // Apply the user's saved app appearance before the first activity draws.
        AppTheme.apply(this)
        super.onCreate()
    }
}
