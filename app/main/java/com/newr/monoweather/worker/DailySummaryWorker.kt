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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.newr.monoweather.MainActivity
import com.newr.monoweather.R
import com.newr.monoweather.data.NotificationPrefs
import com.newr.monoweather.data.RetrofitInstance
import kotlinx.coroutines.flow.first

class DailySummaryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = NotificationPrefs(context)
        val dailySummaryEnabled = prefs.dailySummaryEnabled.first()

        if (!dailySummaryEnabled) {
            return Result.success()
        }

        return try {
            val lat = inputData.getDouble("lat", -6.2088)
            val lon = inputData.getDouble("lon", 106.8456)
            
            val response = RetrofitInstance.api.getWeather(lat = lat, lon = lon)
            
            val tomorrowCode = response.daily?.weathercode?.getOrNull(1)
            val tomorrowMax = response.daily?.temperature_2m_max?.getOrNull(1)
            val tomorrowMin = response.daily?.temperature_2m_min?.getOrNull(1)
            
            if (tomorrowCode != null && tomorrowMax != null && tomorrowMin != null) {
                val desc = getWeatherDescription(tomorrowCode)
                showNotification("Tomorrow's Forecast \uD83D\uDCC5", "$desc with highs of ${tomorrowMax.toInt()}°C and lows of ${tomorrowMin.toInt()}°C.")
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
        
        if (title.contains("Rain") || message.contains("Rain") || message.contains("Drizzle")) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_rainy)
        } else if (title.contains("Temp") || title.contains("Sunny") || message.contains("Sunny")) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_sunny)
        } else if (title.contains("Cloud") || title.contains("Fog") || message.contains("Cloud") || message.contains("Fog")) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_cloudy)
        } else if (title.contains("Thunder") || title.contains("Storm") || message.contains("Thunderstorm")) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.ic_weather_storm)
        } else if (title.contains("Snow") || message.contains("Snow")) {
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

        notificationManager.notify(2, builder.build())
    }
}
