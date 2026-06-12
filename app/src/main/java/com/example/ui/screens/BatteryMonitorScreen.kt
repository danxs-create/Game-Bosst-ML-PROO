package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.FeaturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorScreen(featuresViewModel: FeaturesViewModel, navController: NavController) {
    val batteryInfo by featuresViewModel.batteryInfo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battery Monitor") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val statusColor = when (batteryInfo.statusText) {
                "Normal" -> Color.Green
                "Warm" -> Color.Yellow
                else -> Color.Red
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Battery Percentage: ${batteryInfo.percentage}%", style = MaterialTheme.typography.titleLarge)
                    LinearProgressIndicator(
                        progress = { batteryInfo.percentage / 100f },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = statusColor
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Temperature: ${batteryInfo.temperature}°C", color = statusColor, style = MaterialTheme.typography.titleLarge)
                    Text("Status: ${batteryInfo.statusText}", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Charging: ${if (batteryInfo.isCharging) "Yes" else "No"}", style = MaterialTheme.typography.bodyLarge)
                    Text("Technology: ${batteryInfo.technology}", style = MaterialTheme.typography.bodyLarge)
                    Text("Health: ${batteryInfo.health}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
