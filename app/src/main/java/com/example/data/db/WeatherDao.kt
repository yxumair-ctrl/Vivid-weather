package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache ORDER BY timestamp DESC")
    fun getAllCached(): Flow<List<WeatherCache>>

    @Query("SELECT * FROM weather_cache WHERE city = :city LIMIT 1")
    suspend fun getByCity(city: String): WeatherCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weather: WeatherCache)

    @Query("DELETE FROM weather_cache WHERE city = :city")
    suspend fun deleteByCity(city: String)

    @Query("DELETE FROM weather_cache")
    suspend fun clearAll()
}
