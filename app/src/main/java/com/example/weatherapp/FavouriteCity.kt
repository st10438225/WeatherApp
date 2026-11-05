package com.example.weatherapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_cities")
data class FavouriteCity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cityName: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val lastKnownTempC: Double,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)