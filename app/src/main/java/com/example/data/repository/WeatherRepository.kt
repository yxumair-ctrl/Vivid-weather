package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiApiService
import com.example.data.api.GeminiRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.ImageConfig
import com.example.data.api.Part
import com.example.data.api.WeatherResponse
import com.example.data.db.WeatherCache
import com.example.data.db.WeatherDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeatherRepository(private val weatherDao: WeatherDao, private val apiService: GeminiApiService) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val weatherAdapter = moshi.adapter(WeatherResponse::class.java)

    val allCached: Flow<List<WeatherCache>> = weatherDao.getAllCached()

    suspend fun getCachedWeather(city: String): WeatherCache? = withContext(Dispatchers.IO) {
        weatherDao.getByCity(city.lowercase().trim())
    }

    suspend fun deleteCity(city: String) = withContext(Dispatchers.IO) {
        weatherDao.deleteByCity(city.lowercase().trim())
    }

    /**
     * Tries to call Gemini 3.5 Flash to generate weather JSON.
     * Then tries to call Gemini 2.5 Flash Image to generate a custom background.
     * On success, inserts the record into Room cache and returns it.
     */
    suspend fun fetchAndCacheWeather(city: String): WeatherCache = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is missing. Please add your key to the Secrets panel in AI Studio.")
        }

        val normalizedCity = city.lowercase().trim()
        val textPrompt = """
            Generate detailed current weather conditions, an Air Quality Index (AQI), an hourly forecast (8 steps spaced through the 24-hour day), and a 5-day forecast for the city: "$city".
            Provide the response strictly in raw JSON format following this exact structure with no markdown blocks or surrounding triple backticks unless required by mime type, just valid parsed JSON:
            {
              "city": "Normalized City Name, Region/Country",
              "temperatureCelsius": 23.5,
              "condition": "Cloudy",
              "humidityPercent": 65,
              "windSpeedKph": 12.0,
              "uvIndex": 4.5,
              "aqi": 2,
              "summary": "Short 1-2 sentence description of the current weather.",
              "promptForBackground": "A gorgeous 3d miniature background cluster of landmarks and buildings of Paris in claymation style, tilt-shift lens miniature model, charming tiny cars and trees, detailed soft global illumination, cloudy grey atmospheric afternoon",
              "forecast": [
                {
                  "day": "Tomorrow",
                  "tempHighCelsius": 25,
                  "tempLowCelsius": 18,
                  "condition": "Sunny",
                  "summary": "Clear and warm"
                },
                {
                  "day": "Wed",
                  "tempHighCelsius": 24,
                  "tempLowCelsius": 17,
                  "condition": "Rainy",
                  "summary": "Afternoon showers"
                },
                {
                  "day": "Thu",
                  "tempHighCelsius": 22,
                  "tempLowCelsius": 15,
                  "condition": "Cloudy",
                  "summary": "Partly cloudy"
                },
                {
                  "day": "Fri",
                  "tempHighCelsius": 23,
                  "tempLowCelsius": 16,
                  "condition": "Sunny",
                  "summary": "Mostly sunny"
                },
                {
                  "day": "Sat",
                  "tempHighCelsius": 26,
                  "tempLowCelsius": 19,
                  "condition": "Sunny",
                  "summary": "Sunny and clear"
                }
              ],
              "hourlyForecast": [
                { "time": "6 AM", "tempCelsius": 18, "condition": "Cloudy" },
                { "time": "9 AM", "tempCelsius": 20, "condition": "Cloudy" },
                { "time": "12 PM", "tempCelsius": 23, "condition": "Sunny" },
                { "time": "3 PM", "tempCelsius": 24, "condition": "Sunny" },
                { "time": "6 PM", "tempCelsius": 22, "condition": "Cloudy" },
                { "time": "9 PM", "tempCelsius": 19, "condition": "Cloudy" },
                { "time": "12 AM", "tempCelsius": 18, "condition": "Windy" },
                { "time": "3 AM", "tempCelsius": 17, "condition": "Foggy" }
              ]
            }
            The "condition" values MUST be chosen strictly from: Sunny, Cloudy, Rainy, Snowy, Windy, Thunderstorm, Foggy.
            The "aqi" value MUST be an integer from 1 (Good) to 5 (Very Unhealthy).
            The "promptForBackground" must describe a beautiful, eye-catching, style-aligned 3D miniature background cluster of landmarks and buildings of "$city" in a claymation style, tilt-shift lens miniature model, charming tiny cars and trees, detailed soft global illumination, representing "$city" in "$city"'s active weather condition. Do not include people, text, faces, or UI elements in the background. Highlight depth, beautiful ambient shadow, mist, or pristine sunshine.
        """.trimIndent()

        val textRequest = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = textPrompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        // 1. Fetch current weather from Gemini 3.5 Flash
        val textResponse = apiService.generateContent(
            model = "gemini-3.5-flash",
            apiKey = apiKey,
            request = textRequest
        )

        var jsonText = textResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Failed to generate weather details for $city. Model returned empty analysis.")

        // Clean any markdown formatting if model returned ```json ``` blocks
        if (jsonText.startsWith("```")) {
            jsonText = jsonText.substringAfter("json").substringAfter("```").substringBeforeLast("```").trim()
        }

        val parsedWeather = weatherAdapter.fromJson(jsonText)
            ?: throw IllegalStateException("Failed to parse weather information. Raw response: $jsonText")

        Log.d("WeatherRepository", "Successfully fetched weather: $parsedWeather")

        // 2. Fetch custom generated graphic from Gemini 2.5 Flash Image Model
        var imageBase64: String? = null
        try {
            val visualStylePrompt = "${parsedWeather.promptForBackground}. 16:9 vertical format, gorgeous background painting style, no people, no text, atmospheric depth."
            val imageRequest = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = visualStylePrompt)))),
                generationConfig = GenerationConfig(
                    imageConfig = ImageConfig(aspectRatio = "9:16", imageSize = "1K"),
                    responseModalities = listOf("TEXT", "IMAGE")
                )
            )

            val imageResponse = apiService.generateContent(
                model = "gemini-2.5-flash-image",
                apiKey = apiKey,
                request = imageRequest
            )

            // Look for inline picture data in the response parts
            val inlinePartData = imageResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull {
                it.inlineData != null
            }?.inlineData?.data

            if (inlinePartData != null && inlinePartData.isNotEmpty()) {
                imageBase64 = inlinePartData
                Log.d("WeatherRepository", "Successfully generated image base64 for $city")
            } else {
                Log.w("WeatherRepository", "Image generation returned empty part or failed model response.")
            }
        } catch (e: Exception) {
            // Log and tolerate image generation failure to preserve the core weather data successfully
            Log.e("WeatherRepository", "Image generation failed for $city", e)
        }

        // 3. Save into Room Cache database and return consolidated Cache item
        val forecastType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.api.ForecastDay::class.java)
        val forecastJsonString = moshi.adapter<List<com.example.data.api.ForecastDay>>(forecastType).toJson(parsedWeather.forecast)
        
        val hourlyType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.api.HourlyForecast::class.java)
        val hourlyJsonString = moshi.adapter<List<com.example.data.api.HourlyForecast>>(hourlyType).toJson(parsedWeather.hourlyForecast)

        val weatherCacheItem = WeatherCache(
            city = normalizedCity,
            displayName = parsedWeather.city,
            temperatureCelsius = parsedWeather.temperatureCelsius,
            condition = parsedWeather.condition,
            humidityPercent = parsedWeather.humidityPercent,
            windSpeedKph = parsedWeather.windSpeedKph,
            uvIndex = parsedWeather.uvIndex,
            aqi = parsedWeather.aqi,
            summary = parsedWeather.summary,
            promptForBackground = parsedWeather.promptForBackground,
            forecastJson = forecastJsonString,
            hourlyJson = hourlyJsonString,
            imageBase64 = imageBase64,
            timestamp = System.currentTimeMillis()
        )

        weatherDao.insert(weatherCacheItem)
        return@withContext weatherCacheItem
    }
}
