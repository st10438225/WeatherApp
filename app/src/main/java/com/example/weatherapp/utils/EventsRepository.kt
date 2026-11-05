package com.example.weatherapp.utils

import com.example.weatherapp.TMEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class EventsRepository {
    suspend fun getEventsNearby(lat: Double, lon: Double): Result<List<TMEvent>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.ticketmasterApi.getEventsNearby(
                apiKey = Constants.TICKETMASTER_API_KEY,
                latlong = "$lat,$lon",
                radius = 25,
                unit = "miles",
                size = 5,
                sort = "date,asc"
            )
            Result.success(response.embedded?.events ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}