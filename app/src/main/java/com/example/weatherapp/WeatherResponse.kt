package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class WeatherResponse (
    val coord: Coord,
    val weather : MutableList<Weather>,
    val base: String,
    @SerializedName("main") val weatherMain: WeatherMain,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Int,
    val sys: Sys,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int
)