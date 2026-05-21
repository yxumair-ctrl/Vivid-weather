package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.WeatherCache
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WeatherUIState {
    object Idle : WeatherUIState
    object Loading : WeatherUIState
    data class Success(val weather: WeatherCache) : WeatherUIState
    data class Error(val message: String) : WeatherUIState
}

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    // List of previously searched and cached cities
    val searchHistory: StateFlow<List<WeatherCache>> = repository.allCached
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<WeatherUIState>(WeatherUIState.Idle)
    val uiState: StateFlow<WeatherUIState> = _uiState.asStateFlow()

    private val _isCelsius = MutableStateFlow(true)
    val isCelsius: StateFlow<Boolean> = _isCelsius.asStateFlow()

    // Keep track of what city name is currently selected to make refreshing easier
    private var activeCityName: String? = null

    fun toggleTempUnit() {
        _isCelsius.value = !_isCelsius.value
    }

    fun selectCity(cached: WeatherCache) {
        activeCityName = cached.city
        _uiState.value = WeatherUIState.Success(cached)
        // Refresh the city timestamp so it ranks at the top of history
        viewModelScope.launch {
            try {
                repository.fetchAndCacheWeather(cached.displayName)
            } catch (e: Exception) {
                // Ignore background refresh errors
            }
        }
    }

    fun searchCity(cityName: String) {
        if (cityName.isBlank()) return
        
        _uiState.value = WeatherUIState.Loading
        viewModelScope.launch {
            try {
                val weather = repository.fetchAndCacheWeather(cityName)
                activeCityName = weather.city
                _uiState.value = WeatherUIState.Success(weather)
            } catch (e: Exception) {
                // Check if we have an offline cached backup before displaying an error
                val cachedBackup = repository.getCachedWeather(cityName)
                if (cachedBackup != null) {
                    activeCityName = cachedBackup.city
                    _uiState.value = WeatherUIState.Success(cachedBackup)
                } else {
                    _uiState.value = WeatherUIState.Error(e.localizedMessage ?: "An unexpected error occurred.")
                }
            }
        }
    }

    fun refreshActive() {
        val city = activeCityName ?: return
        _uiState.value = WeatherUIState.Loading
        viewModelScope.launch {
            try {
                val weather = repository.fetchAndCacheWeather(city)
                _uiState.value = WeatherUIState.Success(weather)
            } catch (e: Exception) {
                // Fallback to cache
                val cached = repository.getCachedWeather(city)
                if (cached != null) {
                    _uiState.value = WeatherUIState.Success(cached)
                } else {
                    _uiState.value = WeatherUIState.Error(e.localizedMessage ?: "Failed to refresh weather data.")
                }
            }
        }
    }

    fun deleteHistoryCity(cityName: String) {
        viewModelScope.launch {
            repository.deleteCity(cityName)
            // If the deleted city was currently active, reset the UI state to Idle
            if (activeCityName == cityName.lowercase().trim()) {
                _uiState.value = WeatherUIState.Idle
                activeCityName = null
            }
        }
    }

    fun clearError() {
        _uiState.value = WeatherUIState.Idle
    }
}

class WeatherViewModelFactory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
