package com.example.domain

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BoosterManager(private val context: Context) {

    fun clearRam(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfoBefore = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfoBefore)

        val runningProcesses = am.runningAppProcesses
        if (runningProcesses != null) {
            for (process in runningProcesses) {
                if (process.processName != context.packageName) {
                    am.killBackgroundProcesses(process.processName)
                }
            }
        }

        val memoryInfoAfter = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfoAfter)
        
        // Return freed memory in MB
        val freedBytes = memoryInfoAfter.availMem - memoryInfoBefore.availMem
        return maxOf(0L, freedBytes / (1024 * 1024))
    }

    suspend fun pingTest(): Int = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("ping -c 1 8.8.8.8")
            val exitValue = process.waitFor()
            if (exitValue == 0) {
                val input = process.inputStream.bufferedReader().use { it.readText() }
                val regex = "time=([0-9.]+) ms".toRegex()
                val match = regex.find(input)
                if (match != null) {
                    return@withContext match.groupValues[1].toFloat().toInt()
                }
            }
            return@withContext -1
        } catch (e: Exception) {
            Log.e("BoosterManager", "Ping failed", e)
            return@withContext -1
        }
    }

    fun launchMobileLegends() {
        val packageName = "com.mobile.legends"
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Mobile Legends not installed", Toast.LENGTH_SHORT).show()
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                // fall back to browser
            }
        }
    }
}
