package com.hotfixcde.motionwall

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private lateinit var preview: TextureView
    private lateinit var sound: Switch
    private var player: MediaPlayer? = null
    private var renderSurface: Surface? = null
    private var selectedUri: Uri? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private val prefs by lazy { getSharedPreferences("motionwall", MODE_PRIVATE) }

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            selectedUri = uri
            prefs.edit().putString("video", uri.toString()).apply()
            preparePreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply { text = "MotionWall"; textSize = 28f }, LinearLayout.LayoutParams(-1, -2))
        root.addView(TextView(this).apply { text = "Your video. Your wallpaper."; textSize = 14f }, LinearLayout.LayoutParams(-1, -2))

        preview = TextureView(this).apply {
            setBackgroundColor(0xFF111111.toInt())
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                    if (selectedUri != null) preparePreview()
                }
                override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                    applyPreviewTransform()
                }
                override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                    releasePreview()
                    return true
                }
                override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) = Unit
            }
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = pad })

        root.addView(Button(this).apply {
            text = "Choose video"
            setOnClickListener { picker.launch(arrayOf("video/*")) }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad / 2 })

        sound = Switch(this).apply {
            text = "Sound"
            isChecked = prefs.getBoolean("sound", false)
        }
        root.addView(sound, LinearLayout.LayoutParams(-1, -2))
        sound.setOnCheckedChangeListener { _, on ->
            prefs.edit().putBoolean("sound", on).apply()
            applySound()
        }

        root.addView(TextView(this).apply { text = "Orientation"; textSize = 16f; setPadding(0, pad / 2, 0, 0) })
        val orientation = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Auto", "Vertical", "Horizontal"))
            setSelection(prefs.getInt("orientation", 0).coerceIn(0, 2))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    prefs.edit().putInt("orientation", position).apply()
                    applyPreviewTransform()
                }
            }
        }
        root.addView(orientation, LinearLayout.LayoutParams(-1, -2))

        root.addView(TextView(this).apply { text = "Video sizing"; textSize = 16f; setPadding(0, pad / 2, 0, 0) })
        val scale = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Crop to fill", "Fit entire video"))
            setSelection(prefs.getInt("scale", 0).coerceIn(0, 1))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    prefs.edit().putInt("scale", position).apply()
                    applyPreviewTransform()
                }
            }
        }
        root.addView(scale, LinearLayout.LayoutParams(-1, -2))

        root.addView(Button(this).apply {
            text = "Set as wallpaper"
            setOnClickListener {
                if (selectedUri == null) {
                    Toast.makeText(this@MainActivity, "Choose a video first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                runCatching {
                    startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, MotionWallpaperService::class.java))
                    })
                }.onFailure {
                    startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                }
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad / 2 })

        setContentView(root)
        prefs.getString("video", null)?.let { value ->
            runCatching { Uri.parse(value) }.getOrNull()?.let { selectedUri = it }
        }
        if (selectedUri != null && preview.isAvailable) preparePreview()
    }

    private fun preparePreview() {
        val uri = selectedUri ?: return
        val texture = preview.surfaceTexture ?: return
        releasePreview()
        renderSurface = Surface(texture)
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(this, uri)
            mp.setSurface(renderSurface)
            mp.isLooping = true
            mp.setOnPreparedListener { prepared ->
                videoWidth = prepared.videoWidth
                videoHeight = prepared.videoHeight
                applySound()
                applyPreviewTransform()
                runCatching { prepared.start() }
            }
            mp.setOnVideoSizeChangedListener { _, w, h ->
                videoWidth = w
                videoHeight = h
                applyPreviewTransform()
            }
            mp.setOnErrorListener { _, _, _ -> true }
            mp.prepareAsync()
        } catch (_: Exception) {
            releasePreview()
            Toast.makeText(this, "Unable to play this video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applySound() {
        val volume = if (prefs.getBoolean("sound", false)) 1f else 0f
        runCatching { player?.setVolume(volume, volume) }
    }

    private fun applyPreviewTransform() {
        if (!preview.isAvailable || videoWidth <= 0 || videoHeight <= 0) return
        val vw = preview.width.toFloat()
        val vh = preview.height.toFloat()
        if (vw <= 0 || vh <= 0) return
        val orientation = prefs.getInt("orientation", 0)
        val rotate = orientation == 1 && videoWidth > videoHeight || orientation == 2 && videoHeight > videoWidth
        val sw = if (rotate) videoHeight.toFloat() else videoWidth.toFloat()
        val sh = if (rotate) videoWidth.toFloat() else videoHeight.toFloat()
        val crop = prefs.getInt("scale", 0) == 0
        val scale = if (crop) max(vw / sw, vh / sh) else min(vw / sw, vh / sh)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((vw - sw * scale) / 2f, (vh - sh * scale) / 2f)
            if (rotate) postRotate(90f, vw / 2f, vh / 2f)
        }
        preview.setTransform(matrix)
    }

    private fun releasePreview() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        renderSurface?.release()
        renderSurface = null
        videoWidth = 0
        videoHeight = 0
    }

    override fun onPause() {
        runCatching { player?.pause() }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (selectedUri != null && player == null && preview.isAvailable) preparePreview()
        else runCatching { if (player?.isPlaying == false) player?.start() }
    }

    override fun onDestroy() {
        releasePreview()
        super.onDestroy()
    }
}
