package com.example.weatherapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.weatherapp.utils.AuthRepository
import com.example.weatherapp.utils.SettingsRepository
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() { // 👈 Changed from ComponentActivity

    private val authRepository = AuthRepository()

    private lateinit var radioCelsius: RadioButton
    private lateinit var radioFahrenheit: RadioButton
    private lateinit var switchNotifications: Switch
    private lateinit var editFavoriteCity: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button
    private lateinit var spinnerLanguage: Spinner

    private val languageCodes  = listOf("en", "zu", "af")
    private val languageLabels = listOf("English", "isiZulu", "Afrikaans")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        radioCelsius        = findViewById(R.id.radioCelsius)
        radioFahrenheit     = findViewById(R.id.radioFahrenheit)
        switchNotifications = findViewById(R.id.switchNotifications)
        editFavoriteCity    = findViewById(R.id.editFavoriteCity)
        btnSave             = findViewById(R.id.btnSave)
        btnLogout           = findViewById(R.id.btnLogout)
        spinnerLanguage     = findViewById(R.id.spinnerLanguage)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languageLabels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        lifecycleScope.launch {
            val savedSettings = SettingsRepository.load()
            savedSettings?.let { populateUi(it) }
        }

        btnSave.setOnClickListener { saveSettings() }

        btnLogout.setOnClickListener {
            authRepository.logout()
            val i = Intent(this, AuthActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finish()
        }
    }

    private fun populateUi(s: UserSettings) {
        if (s.tempUnit == "F") radioFahrenheit.isChecked = true
        else                   radioCelsius.isChecked = true

        switchNotifications.isChecked = s.notificationsEnabled
        editFavoriteCity.setText(s.favoriteCity)

        val idx = languageCodes.indexOf(s.language).coerceAtLeast(0)
        spinnerLanguage.setSelection(idx)
    }

    private fun saveSettings() {
        val unit = if (radioFahrenheit.isChecked) "F" else "C"
        val enabled = switchNotifications.isChecked
        val city = editFavoriteCity.text.toString().trim()

        val selectedIndex = spinnerLanguage.selectedItemPosition.coerceIn(0, languageCodes.size - 1)
        val selectedLang = languageCodes[selectedIndex]

        val settings = UserSettings(
            tempUnit = unit,
            language = selectedLang,
            notificationsEnabled = enabled,
            favoriteCity = city,
            updatedAt = System.currentTimeMillis()
        )

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        lifecycleScope.launch {
            val success = SettingsRepository.save(settings)

            btnSave.isEnabled = true
            btnSave.text = "Save Settings"

            if (success) {
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("language", selectedLang)
                    .putBoolean("notifications_enabled", enabled)
                    .apply()

                if (enabled) {
                    scheduleDailyNotification()
                } else {
                    WorkManager.getInstance(this@SettingsActivity).cancelUniqueWork("daily_weather_notification")
                }

                Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()

                val appLocale = LocaleListCompat.forLanguageTags(selectedLang)
                AppCompatDelegate.setApplicationLocales(appLocale)

            } else {
                Toast.makeText(this@SettingsActivity, "Could not save settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleDailyNotification() {
        val workRequest = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_weather_notification",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}