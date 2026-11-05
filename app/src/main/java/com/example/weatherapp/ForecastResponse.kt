package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    val list: List<ForecastItem>?
)

data class ForecastItem(
    val dt: Long,
    @SerializedName("main") val weatherMain: WeatherMain,
    val weather: List<Weather>,
    @SerializedName("dt_txt") val dtTxt: String // Format: "2024-05-20 15:00:00"
)