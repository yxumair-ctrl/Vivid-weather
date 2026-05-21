package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.api.RetrofitClient
import com.example.data.db.WeatherDatabase
import com.example.data.repository.WeatherRepository
import com.example.ui.screens.WeatherVisualizerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WeatherViewModel
import com.example.ui.viewmodel.WeatherViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Database, API Service, and Repository
    val database = WeatherDatabase.getDatabase(this)
    val repository = WeatherRepository(database.weatherDao(), RetrofitClient.service)

    // Instantiate ViewModel
    val viewModelFactory = WeatherViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, viewModelFactory)[WeatherViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          WeatherVisualizerScreen(
              viewModel = viewModel,
              modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}

