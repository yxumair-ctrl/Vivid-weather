package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey val city: String, // Normalized to lowercase
    val displayName: String,
    val temperatureCelsius: Float,
    val condition: String,
    val humidityPercent: Int,
    val windSpeedKph: Float,
    val uvIndex: Float,
    val aqi: Int = 3, // Safe default (1-5 range)
    val summary: String,
    val promptForBackground: String,
    val forecastJson: String,
    val hourlyJson: String, // Dynamic list of HourlyForecast elements serialized
    val imageBase64: String?, // Decoded in-app or used with Coil / standard Image
    val timestamp: Long = System.currentTimeMillis()
)
