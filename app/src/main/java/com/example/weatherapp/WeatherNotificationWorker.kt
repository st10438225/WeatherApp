package com.example.weatherapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherNotificationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val city = prefs.getString("last_city", null) ?: "Johannesburg"

            val weatherData = RetrofitClient.weatherApi.getWeatherByCity(
                city,
                Constants.APP_ID,
                Constants.METRIC_UNIT
            )

            val temp = weatherData.weatherMain.temp
            val desc = if (weatherData.weather.isNotEmpty()) {
                weatherData.weather[0].description
            } else {
                "Unknown conditions"
            }

            showNotification(city, temp, desc)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(city: String, temp: Double, desc: String) {
        val channelId = "weather_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Weather",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Weather in $city")
            .setContentText("${"%.1f".format(temp)}°C with $desc")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }
}