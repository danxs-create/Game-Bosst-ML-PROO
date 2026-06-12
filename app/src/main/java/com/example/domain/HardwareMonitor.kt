package com.example.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class NetworkInfo(
    val ping: Int,
    val jitter: Int,
    val packetLoss: Float,
    val type: String,
    val status: String
)

data class BatteryInfo(
    val percentage: Int,
    val temperature: Float,
    val isCharging: Boolean,
    val technology: String,
    val health: String,
    val statusText: String
)

class HardwareMonitor(private val context: Context) {

    fun getBatteryInfo(intent: android.content.Intent?): BatteryInfo {
        if (intent == null) return BatteryInfo(0, 0f, false, "Unknown", "Unknown", "Unknown")

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0

        val tempVal = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val temperature = tempVal / 10.0f

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        val statusText = when {
            temperature >= 40 -> "Overheating"
            temperature in 35.0..39.9 -> "Warm"
            else -> "Normal"
        }

        return BatteryInfo(percentage, temperature, isCharging, technology, health, statusText)
    }

    suspend fun performNetworkTest(): NetworkInfo = withContext(Dispatchers.IO) {
        val type = getConnectionType()
        var successPings = 0
        var totalPings = 5
        var totalLatency = 0
        val latencies = mutableListOf<Int>()

        try {
            val process = Runtime.getRuntime().exec("ping -c 5 -W 1 8.8.8.8")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val regex = "time=([0-9.]+) ms".toRegex()
                val match = regex.find(line ?: "")
                if (match != null) {
                    val time = match.groupValues[1].toFloat().toInt()
                    latencies.add(time)
                    successPings++
                    totalLatency += time
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            // Ignore
        }

        val ping = if (successPings > 0) totalLatency / successPings else -1
        val packetLoss = ((totalPings - successPings) / totalPings.toFloat()) * 100

        var jitter = 0
        if (latencies.size > 1) {
            var totalDiff = 0
            for (i in 1 until latencies.size) {
                totalDiff += Math.abs(latencies[i] - latencies[i - 1])
            }
            jitter = totalDiff / (latencies.size - 1)
        }

        val status = when {
            ping == -1 || packetLoss > 10 -> "Poor"
            ping > 100 || jitter > 20 -> "Fair"
            ping > 50 || jitter > 10 -> "Good"
            else -> "Excellent"
        }

        NetworkInfo(ping, jitter, packetLoss.toFloat(), type, status)
    }

    private fun getConnectionType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "None"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "None"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            else -> "Other"
        }
    }
}
