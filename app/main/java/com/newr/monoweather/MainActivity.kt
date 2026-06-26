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
package com.newr.monoweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.animation.core.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.newr.monoweather.ui.theme.MyApplicationTheme
import com.newr.monoweather.ui.WeatherViewModel
import com.newr.monoweather.ui.WeatherState
import com.newr.monoweather.data.WeatherResponse
import kotlin.math.roundToInt

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.work.Constraints
import androidx.work.NetworkType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val workRequest = PeriodicWorkRequestBuilder<com.newr.monoweather.worker.WeatherWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weather_alerts",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 20)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }
        if (now.after(target)) {
            target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis

        val dailyRequest = PeriodicWorkRequestBuilder<com.newr.monoweather.worker.DailySummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_summary",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WeatherScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun WeatherScreen(modifier: Modifier = Modifier, viewModel: WeatherViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val permissionState = rememberMultiplePermissionsState(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    )
    var locationName by androidx.compose.runtime.remember { mutableStateOf("JAKARTA") }
    var isCurrentLocation by androidx.compose.runtime.remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }
    
    val locationPermissionGranted = permissionState.permissions.any { it.status.isGranted }
    
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    
                    try {
                        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                try {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val addresses = geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1)
                                    val bestAddress = addresses?.firstOrNull()
                                    locationName = bestAddress?.let { addr ->
                                        val main = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
                                        val secondary = addr.countryName
                                        
                                        listOfNotNull(
                                            main,
                                            if (main != secondary) secondary else null
                                        ).distinct().joinToString(", ").takeIf { it.isNotBlank() }?.uppercase()
                                    } ?: "UNKNOWN"
                                    isCurrentLocation = true
                                } catch (e: Exception) {
                                    locationName = "LOCATION"
                                    isCurrentLocation = true
                                }
                                viewModel.fetchWeather(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                viewModel.fetchWeather()
                            }
                        }.addOnFailureListener {
                            viewModel.fetchWeather()
                        }
                    } catch (e: SecurityException) {
                        viewModel.fetchWeather()
                    } catch (e: Exception) {
                        viewModel.fetchWeather()
                    }
                } else {
                    viewModel.fetchWeather()
                }
            } catch (e: Exception) {
                viewModel.fetchWeather()
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var newLocation by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    if (showSettingsDialog) {
        val prefs = remember { com.newr.monoweather.data.NotificationPrefs(context) }
        val rainAlertEnabled by prefs.rainAlertEnabled.collectAsState(initial = true)
        val tempAlertEnabled by prefs.tempAlertEnabled.collectAsState(initial = true)
        val dailySummaryEnabled by prefs.dailySummaryEnabled.collectAsState(initial = true)
        val hourlyUpdateEnabled by prefs.hourlyUpdateEnabled.collectAsState(initial = false)

        androidx.compose.ui.window.Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = com.newr.monoweather.ui.theme.Gray90,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.newr.monoweather.ui.theme.NothingBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "NOTIFICATION SETTINGS",
                        style = MaterialTheme.typography.labelMedium.copy(color = com.newr.monoweather.ui.theme.NothingRed),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Heavy Rain", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                            Text("Alerts when precipitation > 1.0mm", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f)))
                        }
                        androidx.compose.material3.Switch(
                            checked = rainAlertEnabled,
                            onCheckedChange = { scope.launch { prefs.setRainAlert(it) } },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = com.newr.monoweather.ui.theme.NothingRed,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = com.newr.monoweather.ui.theme.Gray90,
                                uncheckedBorderColor = com.newr.monoweather.ui.theme.NothingBorder
                            )
                        )
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Extreme Temp", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                            Text("Alerts when temp > 35°C or < 15°C", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f)))
                        }
                        androidx.compose.material3.Switch(
                            checked = tempAlertEnabled,
                            onCheckedChange = { scope.launch { prefs.setTempAlert(it) } },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = com.newr.monoweather.ui.theme.NothingRed,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = com.newr.monoweather.ui.theme.Gray90,
                                uncheckedBorderColor = com.newr.monoweather.ui.theme.NothingBorder
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Summary", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                            Text("Tomorrow's weather forecast", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f)))
                        }
                        androidx.compose.material3.Switch(
                            checked = dailySummaryEnabled,
                            onCheckedChange = { scope.launch { prefs.setDailySummary(it) } },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = com.newr.monoweather.ui.theme.NothingRed,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = com.newr.monoweather.ui.theme.Gray90,
                                uncheckedBorderColor = com.newr.monoweather.ui.theme.NothingBorder
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hourly Update", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                            Text("Regular hourly weather conditions", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f)))
                        }
                        androidx.compose.material3.Switch(
                            checked = hourlyUpdateEnabled,
                            onCheckedChange = { scope.launch { prefs.setHourlyUpdate(it) } },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = com.newr.monoweather.ui.theme.NothingRed,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = com.newr.monoweather.ui.theme.Gray90,
                                uncheckedBorderColor = com.newr.monoweather.ui.theme.NothingBorder
                            )
                        )
                    }

                    androidx.compose.material3.Button(
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                    ) {
                        Text("DONE", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    if (showDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = com.newr.monoweather.ui.theme.Gray90,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.newr.monoweather.ui.theme.NothingBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "SEARCH LOCATION",
                        style = MaterialTheme.typography.labelMedium.copy(color = com.newr.monoweather.ui.theme.NothingRed),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    OutlinedTextField(
                        value = newLocation,
                        onValueChange = { newLocation = it },
                        placeholder = { Text("Enter city name...", color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = com.newr.monoweather.ui.theme.NothingRed,
                            unfocusedBorderColor = com.newr.monoweather.ui.theme.NothingBorder,
                            cursorColor = com.newr.monoweather.ui.theme.NothingRed,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                showDialog = false
                                viewModel.setLoading()
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                        val addresses = geocoder.getFromLocationName(newLocation, 1)
                                        if (addresses != null && addresses.isNotEmpty()) {
                                            val lat = addresses[0].latitude
                                            val lon = addresses[0].longitude
                                            val addr = addresses[0]
                                            val main = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
                                            val secondary = addr.countryName
                                            locationName = listOfNotNull(
                                                main,
                                                if (main != secondary) secondary else null
                                            ).distinct().joinToString(", ").uppercase()
                                            if (locationName.isBlank()) locationName = newLocation.uppercase()
                                            isCurrentLocation = false
                                            viewModel.fetchWeather(lat, lon)
                                        }
                                    } catch (e: Exception) {
                                        // Ignored
                                    }
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = { 
                                keyboardController?.hide()
                                showDialog = false 
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.newr.monoweather.ui.theme.NothingBorder),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                        ) {
                            Text("CANCEL", style = MaterialTheme.typography.labelMedium)
                        }
                        
                        androidx.compose.material3.Button(
                            onClick = {
                                keyboardController?.hide()
                                showDialog = false
                                viewModel.setLoading()
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                        val addresses = geocoder.getFromLocationName(newLocation, 1)
                                        if (addresses != null && addresses.isNotEmpty()) {
                                            val lat = addresses[0].latitude
                                            val lon = addresses[0].longitude
                                            val addr = addresses[0]
                                            val main = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
                                            val secondary = addr.countryName
                                            locationName = listOfNotNull(
                                                main,
                                                if (main != secondary) secondary else null
                                            ).distinct().joinToString(", ").uppercase()
                                            if (locationName.isBlank()) locationName = newLocation.uppercase()
                                            isCurrentLocation = false
                                            viewModel.fetchWeather(lat, lon)
                                        }
                                    } catch (e: Exception) {
                                        // Ignored
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                        ) {
                            Text("SEARCH", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    var neonColor by remember { mutableStateOf(Color.Black) }
    if (!com.newr.monoweather.BuildConfig.IS_OFFICIAL) {
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                neonColor = Color(
                    red = (0..255).random() / 255f,
                    green = (0..255).random() / 255f,
                    blue = (0..255).random() / 255f
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (!com.newr.monoweather.BuildConfig.IS_OFFICIAL) neonColor else MaterialTheme.colorScheme.background)
    ) {
        when (val s = state) {
            is WeatherState.Loading -> {
                DotMatrixSkeletonLoading(modifier = Modifier.align(Alignment.TopCenter))
            }
            is WeatherState.Success -> {
                val finalLocationName = if (!com.newr.monoweather.BuildConfig.IS_OFFICIAL) "ERROR 404" else locationName
                WeatherContent(s.data, finalLocationName, isCurrentLocation, s.lastUpdated, currentTime, s.lat, s.lon, onLocationClick = {
                    showDialog = true
                }, onSettingsClick = {
                    showSettingsDialog = true
                }, onResetLocation = {
                    viewModel.setLoading()
                    // Force refresh current location by asking permissions again or just reloading
                    if (locationPermissionGranted) {
                        try {
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { lastLoc ->
                                if (lastLoc != null) {
                                    try {
                                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                        val addresses = geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1)
                                        val bestAddress = addresses?.firstOrNull()
                                        locationName = bestAddress?.let { addr ->
                                            val main = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
                                            val secondary = addr.countryName
                                            
                                            listOfNotNull(
                                                main,
                                                if (main != secondary) secondary else null
                                            ).distinct().joinToString(", ").takeIf { it.isNotBlank() }?.uppercase()
                                        } ?: "UNKNOWN"
                                        isCurrentLocation = true
                                    } catch (e: Exception) {
                                        locationName = "LOCATION"
                                        isCurrentLocation = true
                                    }
                                    viewModel.fetchWeather(lastLoc.latitude, lastLoc.longitude)
                                } else {
                                    viewModel.fetchWeather()
                                }
                            }.addOnFailureListener {
                                viewModel.fetchWeather()
                            }
                        } catch (e: Exception) {
                            viewModel.fetchWeather()
                        }
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                })
            }
            is WeatherState.Error -> {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "ERROR",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        s.message,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    OutlinedButton(onClick = { viewModel.fetchWeather() }) {
                        Text("RETRY", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
        
        if (!com.newr.monoweather.BuildConfig.IS_OFFICIAL) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
                Text(
                    "⚠️ UNVERIFIED CLONE DETECTED\nDownload official at github.com/NEWR-LABS/MonoWeather\nFor access or collaboration, contact @NEWROPLY or join Discord: discord.gg/vZU3KzEh6a",
                    color = Color.Red,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
            }
        }
    }
}

@Composable
fun WeatherContent(data: WeatherResponse, locationName: String, isCurrentLocation: Boolean, lastUpdated: Long, currentTimeMillis: Long, lat: Double, lon: Double, onLocationClick: () -> Unit, onSettingsClick: () -> Unit, onResetLocation: () -> Unit) {
    val current = data.current_weather
    if (current == null) return

    val borderColor = MaterialTheme.colorScheme.onBackground

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            HeaderSection(locationName, isCurrentLocation, lastUpdated, currentTimeMillis, onLocationClick, onSettingsClick, onResetLocation)
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            WeatherAlertSection(current.weathercode)
        }

        item {
            CurrentWeatherSection(current, data.hourly, data.utc_offset_seconds, currentTimeMillis)
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (data.hourly != null) {
            item {
                Text(
                    "HOURLY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                HourlySection(data.hourly, borderColor)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (data.daily != null) {
            item {
                Text(
                    "DAILY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                DailySection(data.daily, borderColor)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = { uriHandler.openUri("https://discord.gg/vZU3KzEh6a") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_discord),
                            contentDescription = "Discord",
                            tint = Color.White
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { uriHandler.openUri("https://instagram.com/newroply") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_instagram),
                            contentDescription = "Instagram",
                            tint = Color.White
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { uriHandler.openUri("https://x.com/newroply") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_x),
                            contentDescription = "X",
                            tint = Color.White
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { uriHandler.openUri("https://github.com/NEWR-LABS") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_github),
                            contentDescription = "GitHub",
                            tint = Color.White
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { uriHandler.openUri("https://tiktok.com/@newroply") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_tiktok),
                            contentDescription = "TikTok",
                            tint = Color.White
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { uriHandler.openUri("https://threads.net/@newroply") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_threads),
                            contentDescription = "Threads",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    "MonoWeather from NEWR",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun HeaderSection(locationName: String, isCurrentLocation: Boolean, lastUpdated: Long, currentTimeMillis: Long, onLocationClick: () -> Unit, onSettingsClick: () -> Unit, onResetLocation: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp).clickable(enabled = !isCurrentLocation) { onResetLocation() }
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isCurrentLocation) com.newr.monoweather.ui.theme.NothingRed else Color.Gray, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isCurrentLocation) "CURRENT LOCATION" else "SEARCH RESULT",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
            androidx.compose.material3.IconButton(onClick = { onSettingsClick() }) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Text(locationName.uppercase(), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.clickable { onLocationClick() })
        
        val timeDiffMillis = currentTimeMillis - lastUpdated
        val timeDiffSeconds = timeDiffMillis / 1000
        val timeDiffMinutes = timeDiffSeconds / 60
        val timeDiffHours = timeDiffMinutes / 60
        
        val isIndonesian = java.util.Locale.getDefault().language == "in" || java.util.Locale.getDefault().language == "id"
        val updateText = if (isIndonesian) {
            when {
                timeDiffSeconds < 60 -> "Baru saja"
                timeDiffMinutes < 60 -> "Diupdate $timeDiffMinutes menit yang lalu"
                timeDiffHours < 24 -> "Diupdate $timeDiffHours jam yang lalu"
                else -> {
                    val format = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                    "Terakhir update: ${format.format(java.util.Date(lastUpdated))}"
                }
            }
        } else {
            when {
                timeDiffSeconds < 60 -> "Updated just now"
                timeDiffMinutes < 60 -> "Updated $timeDiffMinutes minutes ago"
                timeDiffHours < 24 -> "Updated $timeDiffHours hours ago"
                else -> {
                    val format = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.ENGLISH)
                    "Last updated: ${format.format(java.util.Date(lastUpdated))}"
                }
            }
        }
        
        Text(updateText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }
}

@Composable
fun CurrentWeatherSection(current: com.newr.monoweather.data.CurrentWeather, hourly: com.newr.monoweather.data.HourlyWeather?, utcOffsetSeconds: Int?, tickTimer: Long = 0L) {
    val borderColor = com.newr.monoweather.ui.theme.NothingBorder
    
        // Using java.time.LocalDateTime.now with the timezone of the requested location
        val currentTime = if (utcOffsetSeconds != null) {
            val actualTime = if (tickTimer > 0) tickTimer else System.currentTimeMillis()
            java.time.Instant.ofEpochMilli(actualTime).atOffset(java.time.ZoneOffset.ofTotalSeconds(utcOffsetSeconds)).toLocalDateTime()
        } else {
            val actualTime = if (tickTimer > 0) tickTimer else System.currentTimeMillis()
            java.time.Instant.ofEpochMilli(actualTime).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        }
    
    // Use exact hour and day matched against the hourly data payload
    val currentHourIndex = hourly?.time?.indexOfFirst { timeStr -> 
        try {
            val dateTimeApi = java.time.LocalDateTime.parse(timeStr)
            val currentDateTime = java.time.LocalDateTime.parse(current.time)
            dateTimeApi.hour == currentDateTime.hour && dateTimeApi.toLocalDate() == currentDateTime.toLocalDate()
        } catch (e: Exception) { false }
    } ?: -1
    
    val humidity = if (currentHourIndex >= 0) hourly?.relative_humidity_2m?.getOrNull(currentHourIndex) else null
    val uvIndex = if (currentHourIndex >= 0) hourly?.uv_index?.getOrNull(currentHourIndex) else null
    val apparentTemperature = if (currentHourIndex >= 0) hourly?.apparent_temperature?.getOrNull(currentHourIndex) else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(com.newr.monoweather.ui.theme.Gray90, shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp))
            .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(40.dp))
            .drawBehind { // Dotted pattern
                val dotRadius = 1.dp.toPx()
                val spacing = 16.dp.toPx()
                val dotColor = Color.White.copy(alpha = 0.1f)
                var y = 0f
                while (y < size.height) {
                    var x = 0f
                    while (x < size.width) {
                        drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                        x += spacing
                    }
                    y += spacing
                }
            }
            .padding(24.dp)
    ) {
        val isIndonesian = java.util.Locale.getDefault().language == "in" || java.util.Locale.getDefault().language == "id"
        val currentHour = currentTime.hour
        val timeOfDay = when (currentHour) {
            in 5..10 -> if (isIndonesian) "PAGI" else "MORNING"
            in 11..14 -> if (isIndonesian) "SIANG" else "AFTERNOON"
            in 15..17 -> if (isIndonesian) "SORE" else "EVENING"
            else -> if (isIndonesian) "MALAM" else "NIGHT"
        }
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm:ss a", java.util.Locale.ENGLISH)
        val formattedTime = currentTime.format(formatter).uppercase()
        
        val shortDateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.getDefault())
        val formattedDate = currentTime.format(shortDateFormatter).uppercase()
        
        Text(
            text = "$timeOfDay / $formattedDate / $formattedTime",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.TopStart)
        )
        
        Column(
            modifier = Modifier.align(Alignment.Center).padding(bottom = 56.dp), // Increased padding to make room for bottom row and prevent crowding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeatherIcon(current.weathercode, fontSize = 64.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${current.temperature.roundToInt()}°",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 120.sp,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.White.copy(alpha = 0.5f),
                            blurRadius = 2f
                        )
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (apparentTemperature != null) {
                Text(
                    text = "Feels like ${apparentTemperature.roundToInt()}°",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                Box(modifier = Modifier.background(Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("NOW", style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color.Black)
                }
                Box(modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(getWeatherDescription(current.weathercode), style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 0.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_wind),
                    contentDescription = "Wind",
                    modifier = Modifier.size(24.dp).padding(bottom = 4.dp),
                    tint = Color.White
                )
                Text("${current.windspeed.roundToInt()} km/h", style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color.White)
                Text("WIND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
            if (humidity != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_humidity),
                        contentDescription = "Humidity",
                        modifier = Modifier.size(24.dp).padding(bottom = 4.dp),
                        tint = Color.White
                    )
                    Text("$humidity%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color.White)
                    Text("HUMIDITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
            if (uvIndex != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_uv_index),
                        contentDescription = "UV",
                        modifier = Modifier.size(24.dp).padding(bottom = 4.dp),
                        tint = Color.White
                    )
                    Text("${uvIndex.roundToInt()}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color.White)
                    Text("UV INDEX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
    }

}

@Composable
fun HourlySection(hourly: com.newr.monoweather.data.HourlyWeather, borderColor: Color) {
    val currentHourStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.getDefault()).format(java.util.Date())
    val skipHours = hourly.time.indexOfFirst { it >= currentHourStr }.takeIf { it >= 0 } ?: 0

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(24) { i ->
            val index = skipHours + i
            if (index < hourly.time.size) {
                val timeStr = hourly.time[index].substringAfter("T")
                val temp = hourly.temperature_2m[index].roundToInt()
                val code = hourly.weathercode[index]
                
                Column(
                    modifier = Modifier
                        .background(com.newr.monoweather.ui.theme.Gray90, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .border(1.dp, com.newr.monoweather.ui.theme.NothingBorder, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .width(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(timeStr, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))
                    WeatherIcon(code)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${temp}°", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun WeatherIcon(code: Int, fontSize: androidx.compose.ui.unit.TextUnit = 24.sp) {
    val sizeDp = with(androidx.compose.ui.platform.LocalDensity.current) { fontSize.toDp() }
    
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "weather_anim")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(15000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "sun_rotation"
    )
    
    val sway by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(3000, easing = androidx.compose.animation.core.EaseInOutSine),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "cloud_sway"
    )
    
    val bob by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.EaseInOutSine),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "rain_bob"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "storm_pulse"
    )
    
    val animatedModifier = Modifier.graphicsLayer {
        when (code) {
            0, 1 -> rotationZ = rotation
            2, 3, 45, 48 -> translationX = sway * density
            51, 53, 55, 61, 63, 65 -> translationY = bob * density
            71, 73, 75 -> {
                translationY = bob * density
                translationX = sway * 0.5f * density
            }
            95, 96, 99 -> {
                scaleX = pulse
                scaleY = pulse
            }
        }
    }
    
    val iconRes = when (code) {
        0, 1 -> R.drawable.ic_weather_sunny
        2 -> R.drawable.ic_weather_partly_cloudy
        3 -> R.drawable.ic_weather_cloudy
        45, 48 -> R.drawable.ic_weather_fog
        51, 53, 55 -> R.drawable.ic_weather_rainy
        61, 63, 65 -> R.drawable.ic_weather_rainy
        71, 73, 75 -> R.drawable.ic_weather_snowy
        95, 96, 99 -> R.drawable.ic_weather_storm
        else -> R.drawable.ic_weather_sunny
    }
    
    androidx.compose.material3.Icon(
        painter = androidx.compose.ui.res.painterResource(id = iconRes),
        contentDescription = "Weather Icon",
        modifier = Modifier.size(sizeDp).then(animatedModifier),
        tint = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun DailySection(daily: com.newr.monoweather.data.DailyWeather, borderColor: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        daily.time.take(7).forEachIndexed { index, time ->
            val desc = getWeatherDescription(daily.weathercode[index])
            val max = daily.temperature_2m_max[index].roundToInt()
            val min = daily.temperature_2m_min[index].roundToInt()
            
            // Highlight the first item like "Tomorrow"
            val isFirstLine = index == 0
            val bg = if (isFirstLine) com.newr.monoweather.ui.theme.Gray90 else Color.Transparent
            val border = if (isFirstLine) Modifier.border(1.dp, com.newr.monoweather.ui.theme.NothingBorder, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) else Modifier
            
            Row(
                modifier = border
                    .fillMaxWidth()
                    .background(bg, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = if (isFirstLine) 16.dp else 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isFirstLine) "TOMORROW" else time.substring(5).replace("-", "/"),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFirstLine) Color.White else Color.White.copy(alpha = 0.6f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                        WeatherIcon(code = daily.weathercode[index], fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "$min° / $max°",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = if (isFirstLine) Color.White else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.width(76.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
            if (!isFirstLine && index < daily.time.lastIndex) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "CLEAR"
        1 -> "MOSTLY CLEAR"
        2 -> "PARTLY CLOUDY"
        3 -> "OVERCAST"
        45, 48 -> "FOG"
        51, 53, 55 -> "DRIZZLE"
        61, 63, 65 -> "RAIN"
        71, 73, 75 -> "SNOW"
        95, 96, 99 -> "THUNDER"
        else -> "UNKNOWN"
    }
}

@Composable
fun DotMatrixSkeletonLoading(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "SkeletonTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Shimmer"
    )

    // A pulsing black-and-white shimmer gradient (sejajar / parallel along x-axis)
    val shimmerBrush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            Color.Black,
            Color.White.copy(alpha = 0.4f),
            Color.Black
        ),
        start = androidx.compose.ui.geometry.Offset(translateAnim, 0f),
        end = androidx.compose.ui.geometry.Offset(translateAnim + 600f, 0f)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Header Skeleton (Matches HeaderSection)
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(shimmerBrush, androidx.compose.foundation.shape.CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(120.dp).height(12.dp).background(shimmerBrush, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.width(220.dp).height(40.dp).background(shimmerBrush, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.width(150.dp).height(12.dp).background(shimmerBrush, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Hero Weather Skeleton (Matches CurrentWeatherSection)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(shimmerBrush, shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(40.dp))
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Hourly Header Text
        Box(modifier = Modifier.width(60.dp).height(12.dp).background(shimmerBrush, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(8.dp))

        // Hourly Row Skeleton (Matches HourlySection)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(130.dp)
                        .background(shimmerBrush, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Daily Header Text
        Box(modifier = Modifier.width(60.dp).height(12.dp).background(shimmerBrush, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(8.dp))

        // Daily List Skeleton (Matches DailySection)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(shimmerBrush, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                )
            }
        }
    }
}


@Composable
fun WeatherAlertSection(weathercode: Int) {
    val alertDesc = when(weathercode) {
        95, 96, 99 -> "SEVERE THUNDERSTORM WARNING"
        65, 67 -> "HEAVY RAIN WARNING"
        75, 77 -> "HEAVY SNOW WARNING"
        else -> null
    }

    if (alertDesc != null) {
        val infiniteTransition = rememberInfiniteTransition(label = "AlertAnim")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowAlpha"
        )
        
        val alertColor = Color(0xFFFF2A55) // Neon Red

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                )
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
        ) {
            // Ambient glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                alertColor.copy(alpha = 0.15f * glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                // Icon Box
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .background(alertColor.copy(alpha = 0.1f), shape = androidx.compose.foundation.shape.CircleShape)
                        .border(1.dp, alertColor.copy(alpha = 0.3f), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Warning,
                        contentDescription = "Alert",
                        tint = alertColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WEATHER ALERT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = alertColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alertDesc,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        color = Color.White
                    )
                }
                
                // Pulsing dot indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .shadow(
                            elevation = if (glowAlpha > 0.5f) 8.dp else 0.dp,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            ambientColor = alertColor,
                            spotColor = alertColor
                        )
                        .background(alertColor.copy(alpha = glowAlpha), shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}