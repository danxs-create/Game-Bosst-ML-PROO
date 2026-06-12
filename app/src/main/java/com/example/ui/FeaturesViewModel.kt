package com.example.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GameSession
import com.example.data.GameSessionRepository
import com.example.domain.AppUsageInfo
import com.example.domain.AppUsageManager
import com.example.domain.BatteryInfo
import com.example.domain.HardwareMonitor
import com.example.domain.NetworkInfo
import com.example.domain.DeviceGuideManager
import com.example.domain.DisplayInfo
import com.example.domain.GameRecommendation
import com.example.domain.VisualModeManager
import com.example.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeaturesViewModel(application: Application) : AndroidViewModel(application) {
    private val appUsageManager = AppUsageManager(application)
    private val hardwareMonitor = HardwareMonitor(application)
    
    private val repository = GameSessionRepository(AppDatabase.getDatabase(application).gameSessionDao())

    val deviceGuideManager = DeviceGuideManager(application)
    val visualModeManager = VisualModeManager(application)
    val settingsRepository = SettingsRepository(application)
    
    val sessionHistory = repository.allSessions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _displayInfo = MutableStateFlow(DisplayInfo(60f, 60f, listOf(60f)))
    val displayInfo: StateFlow<DisplayInfo> = _displayInfo.asStateFlow()

    private val _recommendations = MutableStateFlow<List<GameRecommendation>>(emptyList())
    val recommendations: StateFlow<List<GameRecommendation>> = _recommendations.asStateFlow()

    private val _todayUsage = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val todayUsage: StateFlow<List<AppUsageInfo>> = _todayUsage.asStateFlow()

    private val _weeklyUsage = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val weeklyUsage: StateFlow<List<AppUsageInfo>> = _weeklyUsage.asStateFlow()

    private val _batteryInfo = MutableStateFlow(BatteryInfo(0, 0f, false, "Unknown", "Unknown", "Unknown"))
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val _networkInfo = MutableStateFlow(NetworkInfo(-1, 0, 0f, "Unknown", "Testing..."))
    val networkInfo: StateFlow<NetworkInfo> = _networkInfo.asStateFlow()
    
    private var networkTestJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _batteryInfo.value = hardwareMonitor.getBatteryInfo(intent)
        }
    }

    init {
        application.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        _displayInfo.value = visualModeManager.getDisplayInfo()
        _recommendations.value = deviceGuideManager.getRecommendations()
    }

    fun loadUsageStats() {
        viewModelScope.launch {
            _todayUsage.value = appUsageManager.getTodayUsage()
            _weeklyUsage.value = appUsageManager.getWeeklyUsage()
        }
    }

    fun startNetworkTest() {
        networkTestJob?.cancel()
        networkTestJob = viewModelScope.launch {
            while (true) {
                _networkInfo.value = hardwareMonitor.performNetworkTest()
                delay(3000) // update every 3 seconds
            }
        }
    }

    fun stopNetworkTest() {
        networkTestJob?.cancel()
    }

    fun deleteSession(session: GameSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(batteryReceiver)
    }
}
