package com.example.weatherapp.utils

import android.content.Context
import com.example.weatherapp.LastWeatherCache
import com.example.weatherapp.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

data class WeatherResult(
    val data: WeatherResponse,
    val isOffline: Boolean = false
)

class WeatherRepository(context: Context) {
    private val cache = LastWeatherCache(context)

    suspend fun getWeatherByLocation(lat: Double, lon: Double): Result<WeatherResult> = withContext(Dispatchers.IO) {
        try {
            val weather = RetrofitClient.weatherApi.getWeatherDetails(
                lat, lon, Constants.APP_ID, Constants.METRIC_UNIT
            )
            cache.save(weather)
            Result.success(WeatherResult(weather, isOffline = false))
        } catch (e: Exception) {
            loadFromCacheOrFail(e)
        }
    }

    suspend fun getWeatherByCity(city: String): Result<WeatherResult> = withContext(Dispatchers.IO) {
        try {
            val weather = RetrofitClient.weatherApi.getWeatherByCity(
                city, Constants.APP_ID, Constants.METRIC_UNIT
            )
            cache.save(weather)
            Result.success(WeatherResult(weather, isOffline = false))
        } catch (e: Exception) {
            loadFromCacheOrFail(e)
        }
    }

    private fun loadFromCacheOrFail(originalException: Exception): Result<WeatherResult> {
        val cached = cache.load()
        return if (cached != null) {
            Result.success(WeatherResult(cached, isOffline = true))
        } else {
            Result.failure(originalException)
        }
    }
}