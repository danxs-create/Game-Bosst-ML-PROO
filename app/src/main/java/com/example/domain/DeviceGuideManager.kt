package com.example.domain

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class GameRecommendation(
    val gameName: String,
    val graphics: String,
    val frameRate: String,
    val shadow: Boolean,
    val antiAliasing: Boolean,
    val hdMode: Boolean,
    val explanation: String
)

class DeviceGuideManager(private val context: Context) {
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"

    fun getRecommendations(): List<GameRecommendation> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        val ramGiga = memoryInfo.totalMem / (1024 * 1024 * 1024)

        val isHighEnd = ramGiga >= 6
        val isMidEnd = ramGiga in 4..5

        return listOf(
            GameRecommendation(
                "Mobile Legends",
                if (isHighEnd) "Ultra" else if (isMidEnd) "High" else "Smooth",
                if (isHighEnd) "Ultra (120 FPS)" else if (isMidEnd) "High (60 FPS)" else "Medium",
                isHighEnd,
                isHighEnd,
                isHighEnd || isMidEnd,
                "Optimized for stable framerates during intense 5v5 teamfights without thermal throttling."
            ),
            GameRecommendation(
                "Free Fire",
                if (isHighEnd) "Max" else if (isMidEnd) "Ultra" else "Standard",
                if (isHighEnd) "High" else "Normal",
                isHighEnd || isMidEnd,
                isHighEnd,
                isHighEnd || isMidEnd,
                "Balances viewing distance for spotting enemies with battery consumption."
            ),
            GameRecommendation(
                "PUBG Mobile",
                if (isHighEnd) "HDR" else if (isMidEnd) "HD" else "Smooth",
                if (isHighEnd) "Extreme" else if (isMidEnd) "Ultra" else "High",
                isHighEnd,
                isHighEnd,
                isHighEnd,
                "Prioritizes frame rate over graphics to reduce input lag."
            ),
            GameRecommendation(
                "Call of Duty Mobile",
                if (isHighEnd) "Very High" else if (isMidEnd) "High" else "Medium",
                if (isHighEnd) "Max" else if (isMidEnd) "Very High" else "High",
                isHighEnd,
                isHighEnd,
                isHighEnd || isMidEnd,
                "Maximizes reaction time with highest possible frame limits."
            )
        )
    }
}
