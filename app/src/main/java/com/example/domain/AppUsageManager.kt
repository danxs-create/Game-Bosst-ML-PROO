package com.example.domain

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.util.Calendar

data class AppUsageInfo(val packageName: String, val appName: String, val totalTimeInForeground: Long)

class AppUsageManager(private val context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    fun getUsageStats(interval: Int, startTime: Long, endTime: Long): List<AppUsageInfo> {
        val stats = usageStatsManager.queryUsageStats(interval, startTime, endTime)
        
        return stats?.mapNotNull { stat ->
            if (stat.totalTimeInForeground > 0) {
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(stat.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    stat.packageName
                }
                AppUsageInfo(stat.packageName, appName, stat.totalTimeInForeground)
            } else {
                null
            }
        }?.groupBy { it.packageName }
         ?.map { entry -> 
             entry.value.reduce { acc, info -> 
                 AppUsageInfo(info.packageName, info.appName, acc.totalTimeInForeground + info.totalTimeInForeground) 
             }
         }
         ?.sortedByDescending { it.totalTimeInForeground } ?: emptyList()
    }

    fun getTodayUsage(): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        return getUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
    }

    fun getWeeklyUsage(): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        return getUsageStats(UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime)
    }
}
