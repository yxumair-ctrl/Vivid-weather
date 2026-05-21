package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.example.data.api.ForecastDay
import com.example.data.api.HourlyForecast
import com.example.data.db.WeatherCache
import com.example.ui.viewmodel.WeatherUIState
import com.example.ui.viewmodel.WeatherViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Composable
fun WeatherVisualizerScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isCelsius by viewModel.isCelsius.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Set search field helper
    fun performSearch() {
        if (searchQuery.isNotBlank()) {
            viewModel.searchCity(searchQuery)
            focusManager.clearFocus()
            keyboardController?.hide()
            searchQuery = ""
        }
    }

    // Determine current active weather condition for local Canvas visualizer settings
    val currentCondition = when (val state = uiState) {
        is WeatherUIState.Success -> state.weather.condition
        else -> ""
    }

    // Capture the generated image background
    val currentImageBase64 = when (val state = uiState) {
        is WeatherUIState.Success -> state.weather.imageBase64
        else -> null
    }

    // Decode generated Base64 background if present
    val backgroundBitmap = remember(currentImageBase64) {
        if (currentImageBase64 != null) {
            try {
                val decodedBytes = Base64.decode(currentImageBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Dynamic Graphic Canvas Background
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = "Generative weather environment background art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Ambient Dimming Tint Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        } else {
            // High-Art Ambient Gradient Canvas fallback based on condition
            val backgroundGradient = remember(currentCondition) {
                getAmbientGradient(currentCondition)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient)
            )
        }

        // 2. Animated Weather Visualizer Overlays (Rain, Snow, Sunny glow, Fog, Wind)
        WeatherParticlesVisualizer(condition = currentCondition)

        // 3. User Interface Content Scroll Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp)) // Safe-area top spacer for edge-to-edge

            // Header block with elegant display styling
            Text(
                text = "AETHER WEATHER",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Search Bar Component
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search city (e.g. Kyoto, London...)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = Color.White.copy(alpha = 0.8f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("city_search_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // F vs C selector Chip
                IconButton(
                    onClick = { viewModel.toggleTempUnit() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .testTag("unit_toggle_button")
                ) {
                    Text(
                        text = if (isCelsius) "°C" else "°F",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (val state = uiState) {
                    is WeatherUIState.Idle -> {
                        IdleWeatherScreen(
                            history = searchHistory,
                            onHistorySelect = { viewModel.selectCity(it) },
                            onHistoryDelete = { viewModel.deleteHistoryCity(it) }
                        )
                    }
                    is WeatherUIState.Loading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Consulting the sky...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Generating atmospheric background artwork with Gemini",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is WeatherUIState.Error -> {
                        ErrorStateScreen(
                            message = state.message,
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                    is WeatherUIState.Success -> {
                        SuccessWeatherScreen(
                            weather = state.weather,
                            isCelsius = isCelsius,
                            history = searchHistory,
                            onRefresh = { viewModel.refreshActive() },
                            onHistorySelect = { viewModel.selectCity(it) },
                            onHistoryDelete = { viewModel.deleteHistoryCity(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun IdleWeatherScreen(
    history: List<WeatherCache>,
    onHistorySelect: (WeatherCache) -> Unit,
    onHistoryDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (history.isEmpty()) Arrangement.Center else Arrangement.Top
    ) {
        if (history.isEmpty()) {
            AnimatedWeatherIcon(
                condition = "sunny",
                modifier = Modifier
                    .size(96.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                text = "Vivid Air",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Enter any city above to visual weather current states and forecasts enriched with AI generated brush backgrounds.",
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .widthIn(max = 280.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp))
            SearchHistoryPanel(
                history = history,
                onSelect = onHistorySelect,
                onDelete = onHistoryDelete
            )
        }
    }
}

@Composable
fun ErrorStateScreen(
    message: String,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Error notification icon",
            tint = Color(0xFFFFB4AB),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Atmospheric Interruption",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(text = "Try Again", color = Color.White)
        }
    }
}

@Composable
fun SuccessWeatherScreen(
    weather: WeatherCache,
    isCelsius: Boolean,
    history: List<WeatherCache>,
    onRefresh: () -> Unit,
    onHistorySelect: (WeatherCache) -> Unit,
    onHistoryDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // A. Primary weather metrics inside glassmorphism styled panel
        Surface(
            color = Color.Black.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // City Name Location header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Pin icon",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = weather.displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Button",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Condition Emoji & Temperature text display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedWeatherIcon(
                        condition = weather.condition,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(end = 16.dp)
                    )

                    val displayTemp = if (isCelsius) {
                        "${weather.temperatureCelsius.toInt()}°"
                    } else {
                        "${(weather.temperatureCelsius * 1.8f + 32).toInt()}°"
                    }

                    Text(
                        text = displayTemp,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                }

                Text(
                    text = weather.condition.uppercase(),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = weather.summary,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // B. Secondary Diagnostics Grid (Wind, AQI, UV, Humidity Custom Animations)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                DiagnosticCard(
                    label = "Wind Rate",
                    value = "${weather.windSpeedKph.toInt()} kph",
                    modifier = Modifier.weight(1f)
                ) {
                    WindTurbineVisualizer(
                        windSpeedKph = weather.windSpeedKph,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                // Fetch AQI label status
                val aqiLabel = when (weather.aqi) {
                    1 -> "Good"
                    2 -> "Moderate"
                    3 -> "Sensitive"
                    4 -> "Unhealthy"
                    else -> "Very Bad"
                }
                DiagnosticCard(
                    label = "Air Quality",
                    value = aqiLabel,
                    modifier = Modifier.weight(1f)
                ) {
                    AqiIndexVisualizer(
                        aqi = weather.aqi,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                DiagnosticCard(
                    label = "UV Intensity",
                    value = "${weather.uvIndex}",
                    modifier = Modifier.weight(1f)
                ) {
                    UvIndexVisualizer(
                        uvIndex = weather.uvIndex,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                DiagnosticCard(
                    label = "Humidity",
                    value = "${weather.humidityPercent}%",
                    modifier = Modifier.weight(1f)
                ) {
                    HumidityWaveVisualizer(
                        humidityPercent = weather.humidityPercent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // C. Scrollable Hourly Forecast Bar
        val hours = remember(weather.hourlyJson, weather.temperatureCelsius, weather.condition) {
            val list = parseHourlyForecasts(weather.hourlyJson)
            if (list.isNotEmpty()) {
                list
            } else {
                listOf(
                    HourlyForecast("6 AM", (weather.temperatureCelsius - 2f).toInt(), weather.condition),
                    HourlyForecast("9 AM", weather.temperatureCelsius.toInt(), weather.condition),
                    HourlyForecast("12 PM", (weather.temperatureCelsius + 1f).toInt(), weather.condition),
                    HourlyForecast("3 PM", (weather.temperatureCelsius + 2f).toInt(), weather.condition),
                    HourlyForecast("6 PM", (weather.temperatureCelsius + 1f).toInt(), weather.condition),
                    HourlyForecast("9 PM", weather.temperatureCelsius.toInt(), weather.condition),
                    HourlyForecast("12 AM", (weather.temperatureCelsius - 1f).toInt(), weather.condition),
                    HourlyForecast("3 AM", (weather.temperatureCelsius - 2f).toInt(), weather.condition)
                )
            }
        }

        HourlyForecastBar(
            hours = hours,
            isCelsius = isCelsius,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // D. 5-Day Forecast strip label
        Text(
            text = "5-DAY ATMOSPHERIC FORECAST",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 4.dp, bottom = 12.dp)
        )

        // Decode forecast JSON lists
        val forecasts = remember(weather.forecastJson) {
            parseForecasts(weather.forecastJson)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(forecasts) { day ->
                ForecastStripCard(day = day, isCelsius = isCelsius)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // D. History short panels
        if (history.isNotEmpty()) {
            SearchHistoryPanel(
                history = history,
                onSelect = onHistorySelect,
                onDelete = onHistoryDelete
            )
        }
    }
}

@Composable
fun DiagnosticVial(
    label: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ForecastStripCard(
    day: ForecastDay,
    isCelsius: Boolean
) {
    val displayHigh = if (isCelsius) "${day.tempHighCelsius}°" else "${(day.tempHighCelsius * 1.8f + 32).toInt()}°"
    val displayLow = if (isCelsius) "${day.tempLowCelsius}°" else "${(day.tempLowCelsius * 1.8f + 32).toInt()}°"

    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.width(110.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.day,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedWeatherIcon(
                condition = day.condition,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$displayHigh $displayLow",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = day.summary,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SearchHistoryPanel(
    history: List<WeatherCache>,
    onSelect: (WeatherCache) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "SEARCH REGISTRY",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        history.forEach { cacheItem ->
            Surface(
                color = Color.Black.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(cacheItem) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedWeatherIcon(
                        condition = cacheItem.condition,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 12.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cacheItem.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${cacheItem.temperatureCelsius.toInt()}°C • ${cacheItem.condition}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { onDelete(cacheItem.city) },
                        modifier = Modifier.testTag("delete_city_${cacheItem.city}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove from history database",
                            tint = Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

// --- Helper Functions and Utilities ---

@Composable
fun AnimatedWeatherIcon(
    condition: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_icon_transition")
    
    // 1. Rotation animation for elements like Sun rays or spinning snowflake
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // 2. Pulse animation for sun center or lightning bolt
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // 3. Falling ticker for rain drops and snowflakes
    val dropTicker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drop_ticker"
    )

    // 4. Hover drift for clouds
    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_drift"
    )

    // 5. Lightning bolt flashing alpha ticker (high frequency flashing pulse)
    val lightningFlash by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lightning_flash"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        when (condition.trim().lowercase()) {
            "sunny" -> {
                // Shiny Sun
                // Pulse background glow
                val glowRadius = (width * 0.35f) * pulse
                drawCircle(
                    color = Color(0xFFFFEEA0).copy(alpha = 0.15f * (2f - pulse)),
                    radius = glowRadius,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )

                // Sun Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFEA79), Color(0xFFF5A623)),
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                        radius = width * 0.22f
                    ),
                    radius = width * 0.22f,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )

                // Rotated Sun Rays
                val rayCount = 8
                val rayLength = width * 0.12f
                val minDistance = width * 0.28f
                
                rotate(degrees = rotation, pivot = androidx.compose.ui.geometry.Offset(centerX, centerY)) {
                    for (i in 0 until rayCount) {
                        val angle = (i * 360f / rayCount) * (Math.PI / 180f)
                        val startX = centerX + kotlin.math.cos(angle).toFloat() * minDistance
                        val startY = centerY + kotlin.math.sin(angle).toFloat() * minDistance
                        val endX = centerX + kotlin.math.cos(angle).toFloat() * (minDistance + rayLength)
                        val endY = centerY + kotlin.math.sin(angle).toFloat() * (minDistance + rayLength)
                        
                        drawLine(
                            color = Color(0xFFFFCC00),
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = (width * 0.05f),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            "cloudy" -> {
                // Elegant fluffy overlapping drift clouds
                // Draw Back cloud first
                val backCloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(30f * scaleX, 60f * scaleY)
                    cubicTo(30f * scaleX, 48f * scaleY, 42f * scaleX, 42f * scaleY, 52f * scaleX, 45f * scaleY)
                    cubicTo(56f * scaleX, 32f * scaleY, 74f * scaleX, 32f * scaleY, 78f * scaleX, 40f * scaleY)
                    cubicTo(88f * scaleX, 40f * scaleY, 92f * scaleX, 50f * scaleY, 88f * scaleX, 60f * scaleY)
                    close()
                }

                translate(left = -cloudDrift * 0.3f, top = cloudDrift * 0.2f) {
                    drawPath(
                        path = backCloudPath,
                        color = Color(0xFF94A3B8).copy(alpha = 0.6f)
                    )
                }

                // Front Cloud
                val frontCloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(20f * scaleX, 65f * scaleY)
                    cubicTo(18f * scaleX, 52f * scaleY, 32f * scaleX, 44f * scaleY, 44f * scaleX, 50f * scaleY)
                    cubicTo(50f * scaleX, 36f * scaleY, 70f * scaleX, 38f * scaleY, 74f * scaleX, 48f * scaleY)
                    cubicTo(84f * scaleX, 50f * scaleY, 86f * scaleX, 62f * scaleY, 80f * scaleX, 68f * scaleY)
                    lineTo(20f * scaleX, 68f * scaleY)
                    close()
                }

                translate(left = cloudDrift, top = -cloudDrift) {
                    drawPath(
                        path = frontCloudPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFF1F5F9), Color(0xFFCBD5E1))
                        )
                    )
                }
            }

            "rainy" -> {
                // Cloud with falling raindrops
                // Render cloud
                val cloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(20f * scaleX, 55f * scaleY)
                    cubicTo(18f * scaleX, 42f * scaleY, 32f * scaleX, 34f * scaleY, 44f * scaleX, 40f * scaleY)
                    cubicTo(50f * scaleX, 26f * scaleY, 70f * scaleX, 28f * scaleY, 74f * scaleX, 38f * scaleY)
                    cubicTo(84f * scaleX, 40f * scaleY, 86f * scaleX, 52f * scaleY, 80f * scaleX, 58f * scaleY)
                    lineTo(20f * scaleX, 58f * scaleY)
                    close()
                }

                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                    )
                )

                // Staggered dripping animated raindrops under the cloud base
                val rainOffsets = listOf(
                    Pair(0.35f, 0.0f),
                    Pair(0.50f, 0.33f),
                    Pair(0.65f, 0.67f)
                )

                rainOffsets.forEach { (xRatio, shift) ->
                    val t = (dropTicker + shift) % 1f
                    val rx = width * xRatio
                    val startY = height * 0.58f
                    val endY = height * 0.90f
                    val currentY = startY + (endY - startY) * t
                    val alpha = 1f - t

                    drawLine(
                        color = Color(0xFF60A5FA).copy(alpha = alpha),
                        start = androidx.compose.ui.geometry.Offset(rx, currentY),
                        end = androidx.compose.ui.geometry.Offset(rx - (width * 0.02f), currentY + (height * 0.08f)),
                        strokeWidth = (width * 0.035f),
                        cap = StrokeCap.Round
                    )
                }
            }

            "snowy" -> {
                // Cloud with rotating spin snowflakes
                val cloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(20f * scaleX, 52f * scaleY)
                    cubicTo(18f * scaleX, 40f * scaleY, 32f * scaleX, 32f * scaleY, 44f * scaleX, 38f * scaleY)
                    cubicTo(50f * scaleX, 24f * scaleY, 70f * scaleX, 26f * scaleY, 74f * scaleX, 36f * scaleY)
                    cubicTo(84f * scaleX, 38f * scaleY, 86f * scaleX, 48f * scaleY, 80f * scaleX, 54f * scaleY)
                    lineTo(20f * scaleX, 54f * scaleY)
                    close()
                }

                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))
                    )
                )

                // Staggered spinning falling snowflakes
                val snowPositions = listOf(
                    Pair(0.35f, 0.0f),
                    Pair(0.50f, 0.35f),
                    Pair(0.65f, 0.70f)
                )

                snowPositions.forEach { (xRatio, shift) ->
                    val t = (dropTicker + shift) % 1f
                    val sx = width * xRatio
                    val startY = height * 0.54f
                    val endY = height * 0.92f
                    val currentY = startY + (endY - startY) * t
                    val alpha = 1f - t
                    val flakeRotation = (rotation * 2.5f) + (shift * 360f)

                    rotate(degrees = flakeRotation, pivot = androidx.compose.ui.geometry.Offset(sx, currentY)) {
                        val size = width * 0.07f
                        for (i in 0 until 4) {
                            val angle = (i * 45f) * (Math.PI / 180f)
                            val cos = kotlin.math.cos(angle).toFloat()
                            val sin = kotlin.math.sin(angle).toFloat()
                            drawLine(
                                color = Color.White.copy(alpha = alpha),
                                start = androidx.compose.ui.geometry.Offset(sx - cos * size, currentY - sin * size),
                                end = androidx.compose.ui.geometry.Offset(sx + cos * size, currentY + sin * size),
                                strokeWidth = (width * 0.02f),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            "windy" -> {
                // Cloud with sliding winds
                val cloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(20f * scaleX, 55f * scaleY)
                    cubicTo(18f * scaleX, 42f * scaleY, 32f * scaleX, 34f * scaleY, 44f * scaleX, 40f * scaleY)
                    cubicTo(50f * scaleX, 26f * scaleY, 70f * scaleX, 28f * scaleY, 74f * scaleX, 38f * scaleY)
                    cubicTo(84f * scaleX, 40f * scaleY, 86f * scaleX, 52f * scaleY, 80f * scaleX, 58f * scaleY)
                    lineTo(20f * scaleX, 58f * scaleY)
                    close()
                }

                rotate(degrees = cloudDrift * 0.6f, pivot = androidx.compose.ui.geometry.Offset(centerX, centerY)) {
                    drawPath(
                        path = cloudPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                        )
                    )
                }

                val windLines = listOf(
                    Triple(0.1f, 0.70f, 0.0f),
                    Triple(0.25f, 0.78f, 0.4f),
                    Triple(0.15f, 0.86f, 0.8f)
                )

                windLines.forEach { (xStartRatio, yRatio, delay) ->
                    val progress = (dropTicker * 1.5f + delay) % 1f
                    val totalWindWidth = width * 0.6f
                    val sx = (width * xStartRatio) + totalWindWidth * progress
                    val ex = sx + (width * 0.25f)
                    val wy = height * yRatio
                    
                    val alpha = if (progress < 0.2f) {
                        progress / 0.2f
                    } else if (progress > 0.8f) {
                        (1f - progress) / 0.2f
                    } else {
                        1.0f
                    }

                    if (sx < width) {
                        drawLine(
                            color = Color(0xFFE2E8F0).copy(alpha = alpha * 0.6f),
                            start = androidx.compose.ui.geometry.Offset(sx, wy),
                            end = androidx.compose.ui.geometry.Offset(kotlin.math.min(ex, width), wy),
                            strokeWidth = (width * 0.025f),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            "thunderstorm" -> {
                // Deep dark storm cloud with flashing lightning strike
                val cloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(20f * scaleX, 52f * scaleY)
                    cubicTo(18f * scaleX, 40f * scaleY, 32f * scaleX, 32f * scaleY, 44f * scaleX, 38f * scaleY)
                    cubicTo(50f * scaleX, 24f * scaleY, 70f * scaleX, 26f * scaleY, 74f * scaleX, 36f * scaleY)
                    cubicTo(84f * scaleX, 38f * scaleY, 86f * scaleX, 48f * scaleY, 80f * scaleX, 54f * scaleY)
                    lineTo(20f * scaleX, 54f * scaleY)
                    close()
                }

                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF475569), Color(0xFF1E293B))
                    )
                )

                val isFlash = (lightningFlash > 0.1f && lightningFlash < 0.18f) || 
                              (lightningFlash > 0.28f && lightningFlash < 0.35f)
                
                val flashAlpha = if (isFlash) 1f else 0.1f

                val lightningPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(52f * scaleX, 50f * scaleY)
                    lineTo(42f * scaleX, 70f * scaleY)
                    lineTo(54f * scaleX, 70f * scaleY)
                    lineTo(46f * scaleX, 94f * scaleY)
                    lineTo(62f * scaleX, 66f * scaleY)
                    lineTo(50f * scaleX, 66f * scaleY)
                    close()
                }

                drawPath(
                    path = lightningPath,
                    color = Color(0xFFFBBF24).copy(alpha = flashAlpha)
                )

                if (isFlash) {
                    drawCircle(
                        color = Color(0xFFFCD34D).copy(alpha = 0.2f),
                        radius = width * 0.42f,
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY * 1.2f)
                    )
                }
            }

            "cloudy", "foggy" -> {
                // Fog layers moving horizontally in opposite directions
                val waveAmplitude = height * 0.04f
                val scaleY = height / 100f

                val offset1 = (dropTicker * width)
                val offset2 = - (dropTicker * width)

                val wave1 = Path().apply {
                    moveTo(-width, 50f * scaleY)
                    for (x in -width.toInt()..width.toInt()*2 step 10) {
                        val rad = (x.toFloat() / width) * 2f * Math.PI.toFloat() + (rotation / 10f)
                        val y = 50f * scaleY + kotlin.math.sin(rad) * waveAmplitude
                        lineTo(x.toFloat(), y)
                    }
                }
                val wave2 = Path().apply {
                    moveTo(-width, 70f * scaleY)
                    for (x in -width.toInt()..width.toInt()*2 step 10) {
                        val rad = (x.toFloat() / width) * 2f * Math.PI.toFloat() + (rotation / 15f)
                        val y = 70f * scaleY + kotlin.math.cos(rad) * waveAmplitude
                        lineTo(x.toFloat(), y)
                    }
                }

                translate(left = offset1 % width) {
                    drawPath(
                        path = wave1,
                        color = Color(0xFFCBD5E1).copy(alpha = 0.5f),
                        style = Stroke(width = (width * 0.04f), cap = StrokeCap.Round)
                    )
                }

                translate(left = offset2 % width) {
                    drawPath(
                        path = wave2,
                        color = Color(0xFFE2E8F0).copy(alpha = 0.35f),
                        style = Stroke(width = (width * 0.04f), cap = StrokeCap.Round)
                    )
                }
            }

            else -> {
                // Partly Cloudy default visualizer fallback
                val backSunRadius = width * 0.18f
                drawCircle(
                    color = Color(0xFFF5A623),
                    radius = backSunRadius,
                    center = androidx.compose.ui.geometry.Offset(centerX + (width * 0.15f), centerY - (height * 0.11f))
                )

                val defaultCloud = Path().apply {
                    val sX = width / 100f
                    val sY = height / 100f
                    moveTo(20f * sX, 60f * sY)
                    cubicTo(18f * sX, 48f * sY, 32f * sX, 40f * sY, 44f * sX, 46f * sY)
                    cubicTo(50f * sX, 32f * sY, 70f * sX, 34f * sY, 74f * sX, 44f * sY)
                    cubicTo(84f * sX, 46f * sY, 86f * sX, 58f * sY, 80f * sX, 64f * sY)
                    lineTo(20f * sX, 64f * sY)
                    close()
                }

                translate(left = cloudDrift, top = -cloudDrift * 0.5f) {
                    drawPath(
                        path = defaultCloud,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White, Color(0xFFE2E8F0))
                        )
                    )
                }
            }
        }
    }
}

// --- Helper Functions and Utilities ---

fun getWeatherEmoji(condition: String): String {
    return when (condition.trim().lowercase()) {
        "sunny" -> "☀️"
        "cloudy" -> "☁️"
        "rainy" -> "🌧️"
        "snowy" -> "❄️"
        "windy" -> "💨"
        "thunderstorm" -> "⛈️"
        "foggy" -> "🌫️"
        else -> "⛅"
    }
}

// Visual fallbacks when BG image generation is unavailable/processing
fun getAmbientGradient(condition: String): Brush {
    val colors = when (condition.trim().lowercase()) {
        "sunny" -> listOf(Color(0xFF2B5876), Color(0xFF4E4376), Color(0xFFFFE066).copy(alpha = 0.15f))
        "rainy", "thunderstorm" -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        "snowy" -> listOf(Color(0xFF83A4D4), Color(0xFFB6FBFF))
        "windy" -> listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))
        "cloudy", "foggy" -> listOf(Color(0xFF3E5151), Color(0xFFDECBA4))
        else -> listOf(Color(0xFF111726), Color(0xFF1E283C), Color(0xFF0C0F17))
    }
    return Brush.verticalGradient(colors)
}

// Animated weather particle overlay implementation
@Composable
fun WeatherParticlesVisualizer(condition: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_particles")
    val ticker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "weather_ticker"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        when (condition.trim().lowercase()) {
            "rainy", "thunderstorm" -> {
                // Draw sliding rain droplets
                for (i in 0 until 50) {
                    val seedX = (i * 7919) % width.toInt()
                    val speed = 900f + (i % 5) * 150f
                    val length = 24f.dp.toPx()
                    
                    val x = seedX.toFloat()
                    val startY = ((i * 123) % height + ticker * speed) % height
                    val endY = startY + length
                    
                    drawLine(
                        color = Color(0xFFC2D9FF).copy(alpha = 0.45f),
                        start = androidx.compose.ui.geometry.Offset(x, startY),
                        end = androidx.compose.ui.geometry.Offset(x - 2f, endY),
                        strokeWidth = 1.2f.dp.toPx()
                    )
                }
            }
            "snowy" -> {
                // Draw sliding snowflakes with sway
                for (i in 0 until 35) {
                    val seedX = (i * 3571) % width.toInt()
                    val speed = 120f + (i % 5) * 40f
                    val waveAmp = 16f.dp.toPx()
                    
                    val startY = ((i * 347) % height + ticker * speed) % height
                    val swayX = seedX.toFloat() + kotlin.math.sin((startY / height) * 8f + i.toFloat()) * waveAmp
                    
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = (2.5f + (i % 3) * 1f).dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(swayX % width, startY)
                    )
                }
            }
            "sunny" -> {
                // Pulsing light beams and warm updrafts
                for (i in 0 until 12) {
                    val seedX = (i * 2113) % width.toInt()
                    val speed = 50f + (i % 3) * 15f
                    val radius = (10f + (i % 3) * 4f).dp.toPx()
                    val startY = ((i * 483) % height - ticker * speed + height) % height
                    
                    drawCircle(
                        color = Color(0xFFFFEEA0).copy(alpha = 0.08f),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(seedX.toFloat(), startY)
                    )
                }
            }
            "windy" -> {
                // Sweeping wind wisps
                for (i in 0 until 8) {
                    val seedY = (i * 1543) % height.toInt()
                    val speed = 400f + (i % 3) * 120f
                    val length = 100f.dp.toPx()
                    
                    val startX = ((i * 321) % width + ticker * speed) % width
                    val endX = startX + length
                    val y = seedY.toFloat() + kotlin.math.sin((startX / width) * 4f) * 10f.dp.toPx()
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = androidx.compose.ui.geometry.Offset(startX, y),
                        end = androidx.compose.ui.geometry.Offset(endX, y),
                        strokeWidth = 1.8f.dp.toPx()
                    )
                }
            }
            "cloudy", "foggy" -> {
                // Low density drifting fog layers
                for (i in 0 until 6) {
                    val seedY = (i * 1928) % height.toInt()
                    val speed = 20f + (i % 3) * 8f
                    val radius = (120f + (i % 3) * 40f).dp.toPx()
                    val startX = ((i * 721) % width + ticker * speed) % width
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFE2E8F0).copy(alpha = 0.08f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(startX, seedY.toFloat()),
                            radius = radius
                        ),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(startX, seedY.toFloat())
                    )
                }
            }
        }
    }
}

// Parses JSON string back into ForecastDay list safely
private fun parseForecasts(json: String): List<ForecastDay> {
    return try {
        val moshi = Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, ForecastDay::class.java)
        val adapter = moshi.adapter<List<ForecastDay>>(type)
        adapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

// Parses JSON string back into HourlyForecast list safely
private fun parseHourlyForecasts(json: String?): List<HourlyForecast> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
        val moshi = Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, HourlyForecast::class.java)
        val adapter = moshi.adapter<List<HourlyForecast>>(type)
        adapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun WindTurbineVisualizer(
    windSpeedKph: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "turbine_transition")
    val duration = when {
        windSpeedKph <= 0f -> 1000000
        windSpeedKph < 10f -> 4000
        windSpeedKph < 25f -> 2000
        windSpeedKph < 50f -> 1000
        else -> 500
    }
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "turbine_rotation"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.45f
        
        // Stand base pole
        val standWidth = w * 0.06f
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = androidx.compose.ui.geometry.Offset(cx, cy),
            end = androidx.compose.ui.geometry.Offset(cx, h * 0.95f),
            strokeWidth = standWidth,
            cap = StrokeCap.Round
        )

        // Overlapping rotating blades
        rotate(degrees = rotateAngle, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
            val bladeLength = w * 0.38f
            for (i in 0 until 3) {
                val angle = i * 120f
                val rad = (angle * (Math.PI / 180.0)).toFloat()
                val bx = cx + kotlin.math.cos(rad) * bladeLength
                val by = cy + kotlin.math.sin(rad) * bladeLength
                
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = androidx.compose.ui.geometry.Offset(cx, cy),
                    end = androidx.compose.ui.geometry.Offset(bx, by),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Hub center of turbine
        drawCircle(
            color = Color.White,
            radius = w * 0.12f,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )
    }
}

@Composable
fun AqiIndexVisualizer(
    aqi: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aqi_transition")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aqi_glow"
    )

    val color = when (aqi) {
        1 -> Color(0xFF10B981) // Good Green
        2 -> Color(0xFFFBBF24) // Moderate Yellow/Gold
        3 -> Color(0xFFF97316) // Sensitive Orange/Red
        4 -> Color(0xFFEF4444) // Bad Red
        else -> Color(0xFF8B5CF6) // Toxic Purple
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        
        val padding = w * 0.12f
        val arcSize = androidx.compose.ui.geometry.Size(w - padding *  2f, h - padding * 2f)
        val arcOffset = androidx.compose.ui.geometry.Offset(padding, padding)
        val r = w * 0.38f

        // Glow center element
        drawCircle(
            color = color.copy(alpha = 0.15f * (1.5f - glowScale)),
            radius = r * glowScale,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )

        // Arc tracker outline
        drawArc(
            color = Color.White.copy(alpha = 0.15f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = arcOffset,
            size = arcSize,
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )

        // Progress arc matching index
        val sweepFraction = (((aqi.coerceIn(1, 5) - 1) / 4f) * 270f).coerceAtLeast(12f)
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = sweepFraction,
            useCenter = false,
            topLeft = arcOffset,
            size = arcSize,
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )

        // Core dot indicator
        drawCircle(
            color = color,
            radius = r * 0.35f,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )
    }
}

@Composable
fun UvIndexVisualizer(
    uvIndex: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "uv_transition")
    val radiationPulsate by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "uv_laser"
    )

    val rayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ray_rotation"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val sunRadius = w * 0.18f

        // Radiating pulse aura
        drawCircle(
            color = Color(0xFFFFD166).copy(alpha = 0.2f),
            radius = sunRadius * 1.8f * radiationPulsate,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )

        // Rotational corona sunbeams
        rotate(degrees = rayRotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
            val rayLength = sunRadius * 0.6f * radiationPulsate
            val minR = sunRadius * 1.2f
            for (i in 0 until 6) {
                val angle = i * 60f
                val rad = (angle * (Math.PI / 180.0)).toFloat()
                val sx = cx + kotlin.math.cos(rad) * minR
                val sy = cy + kotlin.math.sin(rad) * minR
                val ex = cx + kotlin.math.cos(rad) * (minR + rayLength)
                val ey = cy + kotlin.math.sin(rad) * (minR + rayLength)
                drawLine(
                    color = Color(0xFFFBBF24),
                    start = androidx.compose.ui.geometry.Offset(sx, sy),
                    end = androidx.compose.ui.geometry.Offset(ex, ey),
                    strokeWidth = w * 0.05f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Inner core of solar visual
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = sunRadius,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )
    }
}

@Composable
fun HumidityWaveVisualizer(
    humidityPercent: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "humidity_transition")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val fillHeight = h * (1f - (humidityPercent.coerceIn(0, 100) / 100f))

        val clipPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        }

        drawContext.canvas.save()
        drawContext.canvas.clipPath(clipPath)

        // Blue background reservoir pool
        drawRect(
            color = Color(0xFF1D4ED8).copy(alpha = 0.12f),
            size = size
        )

        // Draw sine wave path
        val wavePath = Path().apply {
            moveTo(0f, fillHeight)
            for (x in 0..w.toInt() step 2) {
                val waveRad = (x.toFloat() / w) * (2f * Math.PI).toFloat() + waveOffset
                val y = fillHeight + kotlin.math.sin(waveRad) * (h * 0.08f)
                lineTo(x.toFloat(), y)
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = wavePath,
            color = Color(0xFF3B82F6)
        )

        drawContext.canvas.restore()
    }
}

@Composable
fun DiagnosticCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    graphicContent: @Composable () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.40f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                graphicContent()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun HourlyForecastBar(
    hours: List<HourlyForecast>,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "HOURLY WEATHER FLOW",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        Surface(
            color = Color.Black.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(hours) { hour ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(64.dp)
                    ) {
                        Text(
                            text = hour.time,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        AnimatedWeatherIcon(
                            condition = hour.condition,
                            modifier = Modifier.size(36.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val displayTemp = if (isCelsius) {
                            "${hour.tempCelsius}°"
                        } else {
                            "${(hour.tempCelsius * 1.8f + 32).toInt()}°"
                        }
                        
                        Text(
                            text = displayTemp,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = hour.condition,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
