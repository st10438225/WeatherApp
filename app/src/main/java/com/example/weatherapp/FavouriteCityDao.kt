package com.example.weatherapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteCityDao {

    @Query("SELECT * FROM favourite_cities ORDER BY cityName ASC")
    fun observeAll(): Flow<List<FavouriteCity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(city: FavouriteCity)

    @Query("DELETE FROM favourite_cities WHERE cityName = :cityName")
    suspend fun delete(cityName: String)

    @Query("SELECT COUNT(*) FROM favourite_cities")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favourite_cities WHERE cityName = :cityName)")
    suspend fun isFavourite(cityName: String): Boolean
}