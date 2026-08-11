package com.example.motionwall

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.motionwall.databinding.ActivitySettingsBinding

/** Keeps non-essential controls out of the main wallpaper source screen. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(Keys.PREFS, MODE_PRIVATE)
        binding.backButton.setOnClickListener { finish() }

        binding.soundSwitch.setOnCheckedChangeListener { _, enabled ->
            prefs.edit().putBoolean(Keys.SOUND_ENABLED, enabled).apply()
        }
        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.systemThemeRadio -> AppTheme.SYSTEM
                R.id.darkThemeRadio -> AppTheme.DARK
                R.id.lightThemeRadio -> AppTheme.LIGHT
                else -> AppTheme.SYSTEM
            }
            prefs.edit().putString(Keys.APP_THEME, theme.name).apply()
            AppTheme.apply(this)
        }

        loadUi()
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) loadUi()
    }

    private fun loadUi() {
        binding.soundSwitch.isChecked = prefs.getBoolean(Keys.SOUND_ENABLED, true)


        val theme = AppTheme.fromStored(
            prefs.getString(Keys.APP_THEME, AppTheme.SYSTEM.name)
        )
        binding.themeGroup.check(
            when (theme) {
                AppTheme.SYSTEM -> R.id.systemThemeRadio
                AppTheme.DARK -> R.id.darkThemeRadio
                AppTheme.LIGHT -> R.id.lightThemeRadio
            }
        )
    }
}
