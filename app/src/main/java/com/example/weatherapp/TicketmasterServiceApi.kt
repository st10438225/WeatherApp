package com.example.weatherapp

import retrofit2.http.GET
import retrofit2.http.Query

interface TicketmasterServiceApi {
    @GET("events.json")
    suspend fun getEventsNearby(
        @Query("apikey") apiKey: String,
        @Query("latlong") latlong: String,
        @Query("radius") radius: Int,
        @Query("unit") unit: String,
        @Query("size") size: Int,
        @Query("sort") sort: String,
        @Query("locale") locale: String = "*"
    ): TicketmasterResponse // Removed Call<>
}