package com.example.weatherapp

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherServiceApi {
    @GET("2.5/weather")
    suspend fun getWeatherDetails(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") appId: String,
        @Query("units") metric: String
    ): WeatherResponse // Removed Call<>

    @GET("2.5/weather")
    suspend fun getWeatherByCity(
        @Query("q") city: String,
        @Query("appid") appId: String,
        @Query("units") metric: String
    ): WeatherResponse

    @GET("2.5/forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") appId: String,
        @Query("units") metric: String
    ): ForecastResponse

    @GET("https://api.openweathermap.org/geo/1.0/direct")
    suspend fun getCitySuggestions(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") appId: String
    ): List<GeoResult>
}