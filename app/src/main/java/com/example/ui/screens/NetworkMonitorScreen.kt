package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.FeaturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMonitorScreen(featuresViewModel: FeaturesViewModel, navController: NavController) {
    val networkInfo by featuresViewModel.networkInfo.collectAsState()
    val pingHistory = remember { mutableStateListOf<Int>() }

    LaunchedEffect(networkInfo.ping) {
        if (networkInfo.ping > 0) {
            pingHistory.add(networkInfo.ping)
            if (pingHistory.size > 20) {
                pingHistory.removeAt(0)
            }
        }
    }

    DisposableEffect(Unit) {
        featuresViewModel.startNetworkTest()
        onDispose {
            featuresViewModel.stopNetworkTest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Latency Tracker") },
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
            val statusColor = when (networkInfo.status) {
                "Excellent" -> Color.Green
                "Good" -> Color(0xFF8BC34A)
                "Fair" -> Color.Yellow
                else -> Color.Red
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connection Status: ${networkInfo.status}", color = statusColor, style = MaterialTheme.typography.titleLarge)
                    Text("Type: ${networkInfo.type}", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current Ping: ${if(networkInfo.ping == -1) "..." else "${networkInfo.ping} ms"}", style = MaterialTheme.typography.titleMedium)
                    Text("Jitter: ${networkInfo.jitter} ms", style = MaterialTheme.typography.bodyLarge)
                    Text("Packet Loss: ${networkInfo.packetLoss}%", style = MaterialTheme.typography.bodyLarge)
                }
            }
            
            Text("Latency Graph", style = MaterialTheme.typography.titleMedium)
            val graphColor = MaterialTheme.colorScheme.primary
            Card(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    if (pingHistory.isEmpty()) return@Canvas
                    
                    val maxPing = (pingHistory.maxOrNull() ?: 100).coerceAtLeast(100)
                    val stepX = size.width / 20f
                    val scaleY = size.height / maxPing.toFloat()
                    
                    val path = Path()
                    pingHistory.forEachIndexed { index, ping ->
                        val x = index * stepX
                        val y = size.height - (ping * scaleY)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = graphColor,
                        style = Stroke(width = 4f)
                    )
                }
            }
        }
    }
}
