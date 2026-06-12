package com.example.ui

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GameSession
import com.example.data.GameSessionRepository
import com.example.data.SettingsRepository
import com.example.domain.BoosterManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val context: Context,
    val settingsRepository: SettingsRepository,
    private val boosterManager: BoosterManager
) : ViewModel() {

    private val _realtimeRam = MutableStateFlow(0L)
    val realtimeRam: StateFlow<Long> = _realtimeRam.asStateFlow()

    private val _totalRam = MutableStateFlow(0L)
    val totalRam: StateFlow<Long> = _totalRam.asStateFlow()

    private val _batteryTemp = MutableStateFlow(0f)
    val batteryTemp: StateFlow<Float> = _batteryTemp.asStateFlow()
    
    private val _batteryLevel = MutableStateFlow(0)

    private val _pingLatency = MutableStateFlow(-1)
    val pingLatency: StateFlow<Int> = _pingLatency.asStateFlow()

    private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val repository = GameSessionRepository(AppDatabase.getDatabase(context).gameSessionDao())

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val tempVal = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (tempVal > 0) {
                    _batteryTemp.value = tempVal / 10.0f
                }
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    _batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
                }
            }
        }
    }

    init {
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        _totalRam.value = memoryInfo.totalMem / (1024 * 1024)

        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        startRamMonitor()
    }

    private fun startRamMonitor() {
        viewModelScope.launch {
            while (true) {
                val memoryInfo = ActivityManager.MemoryInfo()
                am.getMemoryInfo(memoryInfo)
                _realtimeRam.value = memoryInfo.availMem / (1024 * 1024)
                delay(1000)
            }
        }
    }

    fun doNetworkTest() {
        viewModelScope.launch {
            _pingLatency.value = 0 // loading
            _pingLatency.value = boosterManager.pingTest()
        }
    }

    fun applyGamingMode() {
        viewModelScope.launch {
            val dnd = settingsRepository.dndEnabled.first()
            val brightness = settingsRepository.brightnessFix.first()
            if (dnd) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                }
            }
            if (brightness && Settings.System.canWrite(context)) {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 200) // approx 80%
            }
        }
    }

    fun launchGame() {
        viewModelScope.launch {
            if (settingsRepository.overlayEnabled.first() && Settings.canDrawOverlays(context)) {
                val intent = Intent(context, com.example.service.OverlayService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
        boosterManager.launchMobileLegends()
        monitorGameSession()
    }

    private fun monitorGameSession() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val startBattery = _batteryLevel.value
            val startTemp = _batteryTemp.value
            val startPing = _pingLatency.value.let { if (it > 0) it else boosterManager.pingTest() }
            
            delay(10000) // wait for game to launch
            
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            var isPlaying = true
            
            while(isPlaying) {
                delay(5000) // check every 5 seconds
                val time = System.currentTimeMillis()
                val events = usageStatsManager.queryEvents(time - 10000, time)
                val event = UsageEvents.Event()
                var latestEventApp = ""
                var latestEventType = 0
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    latestEventApp = event.packageName
                    latestEventType = event.eventType
                }
                
                if (latestEventApp != "com.mobile.legends" && latestEventType == UsageEvents.Event.ACTIVITY_PAUSED || latestEventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                    isPlaying = false
                }
                
                // fallback if usage events are empty or not reliable, we just watch if our app regains focus or something,
                // but actually, we can just observe if it's no longer the foreground app, but it's hard without queryEvents
                val recentStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)
                val isForeground = recentStats?.any { it.packageName == "com.mobile.legends" && (time - it.lastTimeUsed < 10000) } == true
                if (!isForeground && latestEventApp == "") {
                    // isPlaying = false (too aggressive without proper event monitoring)
                }
            }
            
            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000
            if (duration > 30) { // save only if played for > 30 seconds
                val endTemp = _batteryTemp.value
                val avgTemp = (startTemp + endTemp) / 2
                val batteryConsumed = startBattery - _batteryLevel.value
                
                val session = GameSession(
                    gameName = "Mobile Legends",
                    startTime = startTime,
                    endTime = endTime,
                    duration = duration,
                    avgTemp = avgTemp,
                    avgPing = startPing,
                    batteryConsumed = if (batteryConsumed > 0) batteryConsumed else 0
                )
                repository.insertSession(session)
            }
        }
    }

    fun clearRam() {
        boosterManager.clearRam()
    }

    override fun onCleared() {
        super.onCleared()
        context.unregisterReceiver(batteryReceiver)
    }
}
