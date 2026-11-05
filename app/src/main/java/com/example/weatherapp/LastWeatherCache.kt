package com.example.weatherapp

import android.content.Context
import com.google.gson.Gson

class LastWeatherCache(context: Context) {
    private val prefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(weather: WeatherResponse) {
        prefs.edit().putString("last_weather", gson.toJson(weather)).apply()
    }

    fun load(): WeatherResponse? {
        val json = prefs.getString("last_weather", null) ?: return null
        return try {
            gson.fromJson(json, WeatherResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
}