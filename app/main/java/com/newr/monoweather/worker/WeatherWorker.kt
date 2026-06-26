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
package com.newr.monoweather.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.newr.monoweather.MainActivity
import com.newr.monoweather.R
import com.newr.monoweather.data.NotificationPrefs
import com.newr.monoweather.data.RetrofitInstance
import kotlinx.coroutines.flow.first

import android.widget.RemoteViews

class WeatherWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = NotificationPrefs(context)
        val rainEnabled = prefs.rainAlertEnabled.first()
        val tempEnabled = prefs.tempAlertEnabled.first()
        val hourlyUpdateEnabled = prefs.hourlyUpdateEnabled.first()

        if (!rainEnabled && !tempEnabled && !hourlyUpdateEnabled) {
            return Result.success()
        }

        return try {
            // Hardcoded to Jakarta if no location, ideally we'd pass location via DataStore or Worker input
            val lat = inputData.getDouble("lat", -6.2088)
            val lon = inputData.getDouble("lon", 106.8456)
            
            val response = RetrofitInstance.api.getWeather(lat = lat, lon = lon)
            
            // Checking rain
            if (rainEnabled) {
                val precipitation = response.hourly?.precipitation?.firstOrNull() ?: 0.0
                val weatherCode = response.hourly?.weathercode?.firstOrNull() ?: 0
                // If precipitation is high or weather code indicates rain
                if (precipitation > 1.0 || listOf(51, 53, 55, 61, 63, 65, 80, 81, 82).contains(weatherCode)) {
                    showNotification("Heavy Rain Alert \uD83C\uDF27\uFE0F", "Prepare your umbrella! Heavy rain is expected soon.")
                }
            }

            // Checking temp threshold
            if (tempEnabled) {
                val currentTemp = response.current_weather?.temperature ?: 0.0
                if (currentTemp > 35.0) {
                    showNotification("High Temp Alert \uD83E\uDD75", "It's getting really hot! Stay hydrated.")
                } else if (currentTemp < 15.0) {
                    showNotification("Low Temp Alert \uD83E\uDD76", "It's getting chilly! Bring a jacket.")
                }
            }
            
            // Hourly update
            if (hourlyUpdateEnabled) {
                val currentTemp = response.current_weather?.temperature ?: 0.0
                val weatherCode = response.current_weather?.weathercode ?: 0
                val desc = getWeatherDescription(weatherCode)
                showNotification("Hourly Weather \u231A", "$desc, ${currentTemp}°C right now.")
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0, 1 -> "Sunny"
            2 -> "Partly Cloudy"
            3 -> "Cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Sunny"
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "weather_alerts"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Weather Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteViews = RemoteViews(context.packageName, R.layout.custom_notification)
        remoteViews.setTextViewText(R.id.notification_title, title.uppercase())
        remoteViews.setTextViewText(R.id.notification_message, message)
        
        if (title.contains("Rain")) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_rainy)
        } else if (title.contains("Temp") || title.contains("Sunny")) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_sunny)
        } else if (title.contains("Cloud") || title.contains("Fog")) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_cloudy)
        } else if (title.contains("Thunder") || title.contains("Storm")) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_storm)
        } else if (title.contains("Snow")) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_snowy)
        } else {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_sunny)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_weather_sunny)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
