package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.domain.BoosterManager
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class AutoBoostService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var boosterManager: BoosterManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsRepository = SettingsRepository(this)
        boosterManager = BoosterManager(this)
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.packageNames = arrayOf("com.mobile.legends")
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
        info.notificationTimeout = 100
        this.serviceInfo = info
        Log.d("AutoBoostService", "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName == "com.mobile.legends") {
                scope.launch {
                    val enabled = settingsRepository.autoBoostEnabled.first()
                    val overlayEnabled = settingsRepository.overlayEnabled.first()
                    
                    if (overlayEnabled) {
                        if (android.provider.Settings.canDrawOverlays(applicationContext)) {
                            val intent = android.content.Intent(applicationContext, OverlayService::class.java)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                applicationContext.startForegroundService(intent)
                            } else {
                                applicationContext.startService(intent)
                            }
                        }
                    }

                    if (enabled) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "ML Booster: Auto Boosting Game...", Toast.LENGTH_SHORT).show()
                        }
                        // Clear RAM
                        boosterManager.clearRam()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
