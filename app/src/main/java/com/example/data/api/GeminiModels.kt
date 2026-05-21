package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Gemini Request Structure ---
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "imageConfig") val imageConfig: ImageConfig? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String,
    @Json(name = "imageSize") val imageSize: String
)

// --- Gemini Response Structure ---
@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

// --- Custom Weather Response Format ---
// Gemini generates this when asked for "application/json" response format.
@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "city") val city: String,
    @Json(name = "temperatureCelsius") val temperatureCelsius: Float,
    @Json(name = "condition") val condition: String, // Sunny, Cloudy, Rainy, Snowy, Windy, Thunderstorm, Foggy
    @Json(name = "humidityPercent") val humidityPercent: Int,
    @Json(name = "windSpeedKph") val windSpeedKph: Float,
    @Json(name = "uvIndex") val uvIndex: Float,
    @Json(name = "aqi") val aqi: Int = 3, // US EPA Air Quality Index (1-5 range: 1=Good, 2=Moderate, 3=Unhealthy for Sensitive Groups, 4=Unhealthy, 5=Very Unhealthy)
    @Json(name = "summary") val summary: String,
    @Json(name = "promptForBackground") val promptForBackground: String,
    @Json(name = "forecast") val forecast: List<ForecastDay>,
    @Json(name = "hourlyForecast") val hourlyForecast: List<HourlyForecast>
)

@JsonClass(generateAdapter = true)
data class HourlyForecast(
    @Json(name = "time") val time: String, // Hour label like "12 PM", "3 PM", "6 PM" etc
    @Json(name = "tempCelsius") val tempCelsius: Int,
    @Json(name = "condition") val condition: String // Sunny, Cloudy, Rainy, Snowy, Windy, Thunderstorm, Foggy
)

@JsonClass(generateAdapter = true)
data class ForecastDay(
    @Json(name = "day") val day: String,
    @Json(name = "tempHighCelsius") val tempHighCelsius: Int,
    @Json(name = "tempLowCelsius") val tempLowCelsius: Int,
    @Json(name = "condition") val condition: String,
    @Json(name = "summary") val summary: String
)
