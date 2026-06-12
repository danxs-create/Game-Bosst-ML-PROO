package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.FeaturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(featuresViewModel: FeaturesViewModel, navController: NavController) {
    val history by featuresViewModel.sessionHistory.collectAsState()

    val totalPlayTimeSecs = history.sumOf { it.duration }
    val totalPlayTimeMins = totalPlayTimeSecs / 60
    val totalPlayTimeHours = totalPlayTimeMins / 60

    val avgTemp = if (history.isNotEmpty()) history.map { it.avgTemp }.average() else 0.0
    val avgPing = if (history.isNotEmpty()) history.map { it.avgPing }.average() else 0.0
    
    val currentDaySessions = history.filter { System.currentTimeMillis() - it.startTime < 86400000L }
    val dailyPlayTimeMins = currentDaySessions.sumOf { it.duration } / 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Analytics") },
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
            Text("Play Time Overview", style = MaterialTheme.typography.titleLarge)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today's Playtime", style = MaterialTheme.typography.bodyMedium)
                        Text("$dailyPlayTimeMins mins", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Playtime", style = MaterialTheme.typography.bodyMedium)
                        Text("$totalPlayTimeHours hrs", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Historical Averages", style = MaterialTheme.typography.titleLarge)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Average session temperature: ${String.format("%.1f", avgTemp)}°C", style = MaterialTheme.typography.bodyLarge)
                    Text("Average session ping: ${avgPing.toInt()} ms", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
