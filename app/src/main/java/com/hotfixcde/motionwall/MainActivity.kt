package com.hotfixcde.motionwall

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var preview: VideoView
    private lateinit var sound: Switch
    private var preparedPlayer: android.media.MediaPlayer? = null
    private var selectedUri: Uri? = null
    private val prefs by lazy { getSharedPreferences("motionwall", MODE_PRIVATE) }

    private val picker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            selectedUri = uri
            prefs.edit().putString("video", uri.toString()).apply()
            showPreview(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        root.addView(TextView(this).apply { text = "MotionWall"; textSize = 28f }, LinearLayout.LayoutParams(-1, -2))
        root.addView(TextView(this).apply { text = "Your video. Your wallpaper."; textSize = 14f }, LinearLayout.LayoutParams(-1, -2))

        preview = VideoView(this).apply {
            setBackgroundColor(0xFF111111.toInt())
            setOnPreparedListener { mp ->
                preparedPlayer = mp
                mp.isLooping = true
                applySound(mp)
                mp.start()
            }
            setOnCompletionListener { mp -> runCatching { mp.start() } }
            setOnErrorListener { _, _, _ -> true }
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = pad })

        root.addView(Button(this).apply { text = "Choose video"; setOnClickListener { picker.launch(arrayOf("video/*")) } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad / 2 })

        sound = Switch(this).apply { text = "Sound"; isChecked = prefs.getBoolean("sound", false) }
        root.addView(sound, LinearLayout.LayoutParams(-1, -2))
        sound.setOnCheckedChangeListener { _, on ->
            prefs.edit().putBoolean("sound", on).apply()
            preparedPlayer?.let { applySound(it) }
        }

        root.addView(TextView(this).apply { text = "Orientation"; textSize = 16f; setPadding(0, pad / 2, 0, 0) })
        val orientation = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Auto", "Vertical", "Horizontal"))
            setSelection(prefs.getInt("orientation", 0).coerceIn(0, 2))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { prefs.edit().putInt("orientation", position).apply() }
            }
        }
        root.addView(orientation, LinearLayout.LayoutParams(-1, -2))

        root.addView(TextView(this).apply { text = "Video sizing"; textSize = 16f; setPadding(0, pad / 2, 0, 0) })
        val scale = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Crop to fill", "Fit entire video"))
            setSelection(prefs.getInt("scale", 0).coerceIn(0, 1))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { prefs.edit().putInt("scale", position).apply() }
            }
        }
        root.addView(scale, LinearLayout.LayoutParams(-1, -2))

        root.addView(Button(this).apply {
            text = "Set as wallpaper"
            setOnClickListener { startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply { putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, MotionWallpaperService::class.java)) }) }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad / 2 })

        setContentView(root)
        prefs.getString("video", null)?.let { runCatching { Uri.parse(it) }.getOrNull()?.let { uri -> selectedUri = uri; showPreview(uri) } }
    }

    private fun applySound(mp: android.media.MediaPlayer) {
        val volume = if (prefs.getBoolean("sound", false)) 1f else 0f
        runCatching { mp.setVolume(volume, volume) }
    }

    private fun showPreview(uri: Uri) {
        preparedPlayer = null
        preview.setVideoURI(uri)
        preview.start()
    }

    override fun onPause() { preview.pause(); super.onPause() }
    override fun onResume() { super.onResume(); if (selectedUri != null) preview.start() }
    override fun onDestroy() { preparedPlayer = null; preview.stopPlayback(); super.onDestroy() }
}
