/*
 * =====================================================
 *  MONOWEATHER
 *  Weather App by NEWR Labs
 *  Copyright (c) 2026 NEWR Labs. All rights reserved.
 * =====================================================
 * 
 *  Connect with NEWR Labs:
 *  - X (Twitter)  : @NEWROPLY
 *  - Instagram    : @NEWROPLY
 *  - TikTok       : @NEWROPLY
 *  - Threads      : @NEWROPLY
 *  - Discord      : https://discord.gg/vZU3KzEh6a
 *  - GitHub       : https://github.com/NEWR-LABS
 * 
 *  "Just the weather. Nothing else."
 * =====================================================
 */
package com.newr.monoweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newr.monoweather.data.RetrofitInstance
import com.newr.monoweather.data.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val data: WeatherResponse, val lastUpdated: Long = System.currentTimeMillis(), val lat: Double = -6.2088, val lon: Double = 106.8456) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel : ViewModel() {
    private val _state = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val state: StateFlow<WeatherState> = _state

    init {
        fetchWeather()
    }

    fun setLoading() {
        _state.value = WeatherState.Loading
    }

    fun fetchWeather(lat: Double = -6.2088, lon: Double = 106.8456) {
        viewModelScope.launch {
            _state.value = WeatherState.Loading
            if (!com.newr.monoweather.BuildConfig.IS_OFFICIAL) {
                // CHAOS MODE
                kotlinx.coroutines.delay(1000)
                val randomCodes = listOf(0, 1, 2, 3, 45, 48, 51, 61, 71, 95)
                val randomTemp = (-100..100).random().toDouble()
                val randomWind = (0..9999).random().toDouble()
                
                val current = com.newr.monoweather.data.CurrentWeather(
                    temperature = randomTemp,
                    weathercode = randomCodes.random(),
                    windspeed = randomWind,
                    time = "2026-06-26T12:00"
                )
                
                val hourly = com.newr.monoweather.data.HourlyWeather(
                    time = List(24) { "2026-06-26T12:00" },
                    temperature_2m = List(24) { (-100..100).random().toDouble() },
                    relative_humidity_2m = List(24) { (0..1000).random() },
                    weathercode = List(24) { randomCodes.random() },
                    uv_index = List(24) { (0..20).random().toDouble() }
                )
                
                val daily = com.newr.monoweather.data.DailyWeather(
                    time = List(7) { "2026-06-26" },
                    weathercode = List(7) { randomCodes.random() },
                    temperature_2m_max = List(7) { (-100..100).random().toDouble() },
                    temperature_2m_min = List(7) { (-100..100).random().toDouble() }
                )
                
                val mockResponse = WeatherResponse(
                    current_weather = current,
                    hourly = hourly,
                    daily = daily,
                    timezone = "UNKNOWN",
                    utc_offset_seconds = 0
                )
                _state.value = WeatherState.Success(mockResponse, lat = lat, lon = lon)
                return@launch
            }
            try {
                val response = RetrofitInstance.api.getWeather(lat = lat, lon = lon)
                _state.value = WeatherState.Success(response, lat = lat, lon = lon)
            } catch (e: Exception) {
                _state.value = WeatherState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
