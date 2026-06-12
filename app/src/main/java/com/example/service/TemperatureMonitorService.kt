package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TemperatureMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var settingsRepository: SettingsRepository
    private var lastWarningTime = 0L

    companion object {
        const val CHANNEL_ID = "temp_warning_channel"
        const val NOTIFICATION_ID = 200
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val tempVal = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (tempVal > 0) {
                    val currentTemp = tempVal / 10.0f
                    checkTemperature(currentTemp)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        createNotificationChannel()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        // Start as foreground to stay alive
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ML Booster")
            .setContentText("Monitoring Temperature...")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(199, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(199, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun checkTemperature(currentTemp: Float) {
        scope.launch {
            val limit = settingsRepository.tempThreshold.first()
            if (currentTemp >= limit) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastWarningTime > 60000) { // Warn every 1 minute max
                    lastWarningTime = currentTime
                    showWarningNotification(currentTemp)
                }
            }
        }
    }

    private fun showWarningNotification(temp: Float) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("High Temperature Alert!")
            .setContentText("Device temperature has reached $temp°C")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temperature Warnings",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
