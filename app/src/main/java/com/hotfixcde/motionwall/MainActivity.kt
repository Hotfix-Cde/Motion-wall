package com.hotfixcde.motionwall

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var preview: VideoView
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
        val title = TextView(this).apply { text = "MotionWall"; textSize = 28f; setTextAppearance(android.R.style.TextAppearance_Material_Headline) }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        root.addView(TextView(this).apply { text = "Your video. Your wallpaper."; textSize = 14f }, LinearLayout.LayoutParams(-1, -2))

        preview = VideoView(this).apply { setBackgroundColor(0xFF111111.toInt()); setOnPreparedListener { it.isLooping = true; it.setVolume(if (prefs.getBoolean("sound", false)) 1f else 0f, if (prefs.getBoolean("sound", false)) 1f else 0f) } }
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = pad })

        val choose = Button(this).apply { text = "Choose video"; setOnClickListener { picker.launch(arrayOf("video/*")) } }
        root.addView(choose, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad / 2 })

        val sound = Switch(this).apply { text = "Sound"; isChecked = prefs.getBoolean("sound", false); setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("sound", on).apply(); preview.setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(if (on) 1f else 0f, if (on) 1f else 0f) }; preview.start() } }
        root.addView(sound, LinearLayout.LayoutParams(-1, -2))

        val orientation = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Auto", "Vertical", "Horizontal")); setSelection(prefs.getInt("orientation", 0)); onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(p: AdapterView<*>?) {} ; override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) { prefs.edit().putInt("orientation", pos).apply() } } }
        root.addView(orientation, LinearLayout.LayoutParams(-1, -2))

        val scale = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Crop", "Fit")); setSelection(prefs.getInt("scale", 0)); onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(p: AdapterView<*>?) {} ; override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) { prefs.edit().putInt("scale", pos).apply() } } }
        root.addView(scale, LinearLayout.LayoutParams(-1, -2))

        root.addView(Button(this).apply { text = "Set as wallpaper"; setOnClickListener { startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply { putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, MotionWallpaperService::class.java)) }) } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad / 2 })
        setContentView(root)

        prefs.getString("video", null)?.let { runCatching { Uri.parse(it) }.getOrNull()?.let { uri -> selectedUri = uri; showPreview(uri) } }
    }

    private fun showPreview(uri: Uri) { preview.setVideoURI(uri); preview.start() }
    override fun onPause() { super.onPause(); preview.pause() }
    override fun onResume() { super.onResume(); if (selectedUri != null) preview.start() }
}
