package com.example.videowallpaper

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.app.WallpaperManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.videowallpaper.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            prefs.edit().putString(Keys.VIDEO_URI, uri.toString()).apply()
            binding.videoPathText.text = queryDisplayName(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(Keys.PREFS, MODE_PRIVATE)
        loadUi()

        binding.selectVideoButton.setOnClickListener {
            pickVideo.launch(arrayOf("video/*"))
        }

        binding.setWallpaperButton.setOnClickListener {
            launchLiveWallpaper()
        }

        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Keys.SOUND_ENABLED, isChecked).apply()
        }

        binding.fitModeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.cropRadio) ScaleMode.CROP.name else ScaleMode.FIT.name
            prefs.edit().putString(Keys.SCALE_MODE, mode).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUi()
    }

    private fun loadUi() {
        val uriText = prefs.getString(Keys.VIDEO_URI, null)
        binding.videoPathText.text = uriText?.let {
            runCatching { queryDisplayName(Uri.parse(it)) }.getOrNull() ?: it
        } ?: getString(R.string.no_video_selected)

        binding.soundSwitch.isChecked = prefs.getBoolean(Keys.SOUND_ENABLED, true)

        val scaleMode = prefs.getString(Keys.SCALE_MODE, ScaleMode.FIT.name) ?: ScaleMode.FIT.name
        binding.fitModeGroup.check(
            if (scaleMode == ScaleMode.CROP.name) R.id.cropRadio else R.id.fitRadio
        )
    }

    private fun launchLiveWallpaper() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, VideoWallpaperService::class.java)
            )
        }

        runCatching { startActivity(intent) }
            .recoverCatching {
                startActivity(
                    Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                )
            }
    }

    private fun queryDisplayName(uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex) ?: uri.lastPathSegment ?: uri.toString()
            } else {
                uri.lastPathSegment ?: uri.toString()
            }
        } ?: (uri.lastPathSegment ?: uri.toString())
    }
}
