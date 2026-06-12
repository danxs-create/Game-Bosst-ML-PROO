package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val dndEnabled by viewModel.settingsRepository.dndEnabled.collectAsState(initial = false)
    val brightnessFix by viewModel.settingsRepository.brightnessFix.collectAsState(initial = false)
    val autoBoostEnabled by viewModel.settingsRepository.autoBoostEnabled.collectAsState(initial = false)
    val tempThreshold by viewModel.settingsRepository.tempThreshold.collectAsState(initial = 40)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Gaming Mode Features", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Auto Reject Calls (DND)")
                    Text("Requires DND permission", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = dndEnabled,
                    onCheckedChange = { 
                        scope.launch { viewModel.settingsRepository.setDnd(it) }
                        if (it) {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Lock Screen Brightness")
                    Text("Requires Write Settings permission", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = brightnessFix,
                    onCheckedChange = { 
                        scope.launch { viewModel.settingsRepository.setBrightnessFix(it) }
                        if (it) {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            HorizontalDivider()
            
            Text("Auto Booster", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Boost on Game Launch")
                    Text("Requires Accessibility Service enabled", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = autoBoostEnabled,
                    onCheckedChange = { 
                        scope.launch { viewModel.settingsRepository.setAutoBoost(it) }
                        if (it) {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            HorizontalDivider()

            Text("Floating Dashboard", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            val overlayEnabled by viewModel.settingsRepository.overlayEnabled.collectAsState(initial = false)
            val overlayTransparency by viewModel.settingsRepository.overlayTransparency.collectAsState(initial = 0.8f)
            val overlaySize by viewModel.settingsRepository.overlaySize.collectAsState(initial = 1f)

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Enable Overlay")
                    Text("Requires Display over other apps", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = overlayEnabled,
                    onCheckedChange = {
                        scope.launch { viewModel.settingsRepository.setOverlayEnabled(it) }
                        if (it && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            context.startActivity(intent)
                        } else if (it) {
                            val intent = Intent(context, com.example.service.OverlayService::class.java)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        } else {
                            context.stopService(Intent(context, com.example.service.OverlayService::class.java))
                        }
                    }
                )
            }

            if (overlayEnabled) {
                Column {
                    Text("Overlay Transparency: ${(overlayTransparency * 100).toInt()}%")
                    Slider(
                        value = overlayTransparency,
                        onValueChange = { scope.launch { viewModel.settingsRepository.setOverlayTransparency(it) } },
                        valueRange = 0.2f..1f
                    )
                }
                Column {
                    Text("Overlay Size: ${(overlaySize * 100).toInt()}%")
                    Slider(
                        value = overlaySize,
                        onValueChange = { scope.launch { viewModel.settingsRepository.setOverlaySize(it) } },
                        valueRange = 0.5f..1.5f
                    )
                }
            }

            HorizontalDivider()

            Text("Temperature Monitor", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Column {
                Text("Warning Threshold: $tempThreshold°C")
                Slider(
                    value = tempThreshold.toFloat(),
                    onValueChange = { scope.launch { viewModel.settingsRepository.setTempThreshold(it.toInt()) } },
                    valueRange = 35f..50f,
                    steps = 15
                )
            }
            
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Temperature Notifications (For Android 13+)")
            }
        }
    }
}
