package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.FeaturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageScreen(featuresViewModel: FeaturesViewModel, navController: NavController) {
    val context = LocalContext.current
    val todayUsage by featuresViewModel.todayUsage.collectAsState()

    LaunchedEffect(Unit) {
        featuresViewModel.loadUsageStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Usage Dashboard") },
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
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) {
                Text("Grant Usage Access Permission")
            }

            Text("Top Apps Today", style = MaterialTheme.typography.titleLarge)

            if (todayUsage.isEmpty()) {
                Text("No usage data available or permission not granted.")
            } else {
                val maxUsage = todayUsage.maxOfOrNull { it.totalTimeInForeground } ?: 1L
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(todayUsage.take(10)) { usage ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(usage.appName, style = MaterialTheme.typography.bodyLarge)
                                    Text("${usage.totalTimeInForeground / 60000} mins", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { usage.totalTimeInForeground.toFloat() / maxUsage.toFloat() },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
