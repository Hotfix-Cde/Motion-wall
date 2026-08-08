package com.hotfixcde.motionwall

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.TextureView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {
    private lateinit var previewController: VideoPreviewController
    private lateinit var previewPlaceholder: TextView
    private lateinit var soundSwitch: SwitchCompat
    private lateinit var orientationGroup: RadioGroup
    private lateinit var scaleGroup: RadioGroup

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
                // Some providers already grant stable read access.
            }
            MotionSettingsStore.saveVideoUri(this, uri)
            previewController.setVideo(uri)
            previewController.setSettings(MotionSettingsStore.load(this))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding = dp(20)
        val sectionSpacing = dp(16)

        val scrollView = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        scrollView.addView(root, ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(TextView(this).apply {
            text = "MotionWall"
            textSize = 30f
            setSingleLine(true)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(TextView(this).apply {
            text = "Your video. Your wallpaper. Small, fast, and offline."
            alpha = 0.78f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6)
        })

        val previewShell = FrameLayout(this).apply {
            setBackgroundColor(0xFF101214.toInt())
            minimumHeight = dp(280)
        }
        val previewTexture = TextureView(this).apply {
            setBackgroundColor(0xFF101214.toInt())
        }
        previewPlaceholder = TextView(this).apply {
            text = "Pick a video to preview it here"
            gravity = Gravity.CENTER
            alpha = 0.72f
        }
        previewShell.addView(previewTexture, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        previewShell.addView(previewPlaceholder, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(previewShell, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(280)).apply {
            topMargin = sectionSpacing
        })

        previewController = VideoPreviewController(this, previewTexture, previewPlaceholder)

        root.addView(TextView(this).apply {
            text = "Playback"
            textSize = 16f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = sectionSpacing
        })

        soundSwitch = SwitchCompat(this).apply {
            text = "Sound"
            isChecked = MotionSettingsStore.load(this@MainActivity).soundEnabled
            setOnCheckedChangeListener { _, enabled ->
                MotionSettingsStore.saveSoundEnabled(this@MainActivity, enabled)
                syncControlsAndPreview()
            }
        }
        root.addView(soundSwitch, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        orientationGroup = RadioGroup(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val autoId = addRadioButton(orientationGroup, "Auto", MotionSettingsStore.load(this).orientationMode == OrientationMode.AUTO)
        val verticalId = addRadioButton(orientationGroup, "Vertical", MotionSettingsStore.load(this).orientationMode == OrientationMode.VERTICAL)
        val horizontalId = addRadioButton(orientationGroup, "Horizontal", MotionSettingsStore.load(this).orientationMode == OrientationMode.HORIZONTAL)
        orientationGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                verticalId -> OrientationMode.VERTICAL
                horizontalId -> OrientationMode.HORIZONTAL
                else -> OrientationMode.AUTO
            }
            MotionSettingsStore.saveOrientationMode(this, mode)
            syncControlsAndPreview()
        }
        root.addView(TextView(this).apply {
            text = "Orientation"
            textSize = 16f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = sectionSpacing
        })
        root.addView(orientationGroup, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        orientationGroup.check(autoId)

        scaleGroup = RadioGroup(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val cropId = addRadioButton(scaleGroup, "Crop", MotionSettingsStore.load(this).scaleMode == ScaleMode.CROP)
        val fitId = addRadioButton(scaleGroup, "Fit", MotionSettingsStore.load(this).scaleMode == ScaleMode.FIT)
        scaleGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                fitId -> ScaleMode.FIT
                else -> ScaleMode.CROP
            }
            MotionSettingsStore.saveScaleMode(this, mode)
            syncControlsAndPreview()
        }
        root.addView(TextView(this).apply {
            text = "Video sizing"
            textSize = 16f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = sectionSpacing
        })
        root.addView(scaleGroup, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        scaleGroup.check(cropId)

        root.addView(Button(this).apply {
            text = "Choose video"
            setOnClickListener {
                picker.launch(arrayOf("video/*"))
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = sectionSpacing
        })

        root.addView(Button(this).apply {
            text = "Apply to Home & Lock screens"
            setOnClickListener {
                startActivity(
                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(this@MainActivity, MotionWallpaperService::class.java),
                        )
                    }
                )
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = sectionSpacing
        })

        setContentView(scrollView)
        syncControlsAndPreview()

        MotionSettingsStore.load(this).videoUri?.let { uri ->
            previewController.setVideo(uri)
        }
    }

    override fun onResume() {
        super.onResume()
        previewController.resume()
    }

    override fun onPause() {
        previewController.pause()
        super.onPause()
    }

    override fun onDestroy() {
        previewController.release()
        super.onDestroy()
    }

    private fun syncControlsAndPreview() {
        val settings = MotionSettingsStore.load(this)
        if (soundSwitch.isChecked != settings.soundEnabled) {
            soundSwitch.isChecked = settings.soundEnabled
        }
        val orientationId = when (settings.orientationMode) {
            OrientationMode.AUTO -> orientationGroup.getChildAt(0).id
            OrientationMode.VERTICAL -> orientationGroup.getChildAt(1).id
            OrientationMode.HORIZONTAL -> orientationGroup.getChildAt(2).id
        }
        if (orientationGroup.checkedRadioButtonId != orientationId) {
            orientationGroup.check(orientationId)
        }
        val scaleId = when (settings.scaleMode) {
            ScaleMode.CROP -> scaleGroup.getChildAt(0).id
            ScaleMode.FIT -> scaleGroup.getChildAt(1).id
        }
        if (scaleGroup.checkedRadioButtonId != scaleId) {
            scaleGroup.check(scaleId)
        }
        previewController.setSettings(settings)
        settings.videoUri?.let(previewController::setVideo)
        previewPlaceholder.visibility = if (settings.videoUri == null) View.VISIBLE else View.GONE
    }

    private fun addRadioButton(group: RadioGroup, label: String, checked: Boolean): Int {
        val button = RadioButton(this).apply {
            text = label
            id = View.generateViewId()
            isChecked = checked
        }
        group.addView(button)
        return button.id
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
