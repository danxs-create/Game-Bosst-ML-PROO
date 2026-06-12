package com.example.domain

import android.content.Context
import android.os.Build
import android.view.WindowManager

data class DisplayInfo(
    val currentRefreshRate: Float,
    val maxRefreshRate: Float,
    val supportedRefreshRates: List<Float>
)

class VisualModeManager(private val context: Context) {
    fun getDisplayInfo(): DisplayInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }

        val currentRefreshRate = display?.refreshRate ?: 60f
        
        val supportedRates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            display?.supportedModes?.map { it.refreshRate }?.distinct()?.sorted() ?: listOf(currentRefreshRate)
        } else {
            listOf(currentRefreshRate)
        }

        val maxRefreshRate = supportedRates.maxOrNull() ?: currentRefreshRate

        return DisplayInfo(
            currentRefreshRate = currentRefreshRate,
            maxRefreshRate = maxRefreshRate,
            supportedRefreshRates = supportedRates
        )
    }
}
