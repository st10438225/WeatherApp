package com.example.weatherapp

data class UserSettings(
    val tempUnit: String = "C",
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val favoriteCity: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)