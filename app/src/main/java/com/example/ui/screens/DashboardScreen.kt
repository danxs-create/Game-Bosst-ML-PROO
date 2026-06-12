package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val realtimeRam by viewModel.realtimeRam.collectAsState()
    val totalRam by viewModel.totalRam.collectAsState()
    val batteryTemp by viewModel.batteryTemp.collectAsState()
    val pingLatency by viewModel.pingLatency.collectAsState()
    val scope = rememberCoroutineScope()
    
    var isBoosting by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ML Booster Pro", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            )
        }
    ) { padding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600), initialOffsetY = { 100 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                // Hero Section / Boost Button
                HeroBoostSection(
                    isBoosting = isBoosting,
                    onBoost = {
                        isBoosting = true
                        viewModel.clearRam()
                        viewModel.applyGamingMode()
                        Toast.makeText(context, "System Optimized for Gaming!", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            delay(1500)
                            isBoosting = false
                            viewModel.launchGame()
                        }
                    }
                )

                // Realtime Stats Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val ramUsagePercent = if (totalRam > 0) ((totalRam - realtimeRam).toFloat() / totalRam.toFloat()) else 0f
                    StatCard(
                        title = "RAM Usage",
                        value = "${(ramUsagePercent * 100).toInt()}%",
                        icon = Icons.Filled.Memory,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        progress = ramUsagePercent
                    )
                    
                    val tempColor = if (batteryTemp >= 40) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    val tempProgress = (batteryTemp - 20f) / 30f // approx scale 20C-50C
                    StatCard(
                        title = "Temperature",
                        value = "${batteryTemp}°C",
                        icon = Icons.Filled.Thermostat,
                        color = tempColor,
                        modifier = Modifier.weight(1f),
                        progress = tempProgress.coerceIn(0f, 1f)
                    )
                }

                // Network Status with Mini Chart
                NetworkMiniChartCard(
                    ping = pingLatency,
                    onTest = { viewModel.doNetworkTest() }
                )

                // Smart Recommendations
                SmartRecommendations(batteryTemp = batteryTemp, ping = pingLatency)

                // Pro Tools Grid
                ProToolsGrid(navController)

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun HeroBoostSection(isBoosting: Boolean, onBoost: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val boostScale by animateFloatAsState(
        targetValue = if (isBoosting) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "boostScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glow
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(if (isBoosting) boostScale else pulseScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Boost Button
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(boostScale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .clickable { if (!isBoosting) onBoost() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isBoosting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("BOOSTING...", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.RocketLaunch, contentDescription = "Boost", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("BOOST", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, progress: Float) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            
            // Progress Bar at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(color.copy(alpha = 0.2f))
            ) {
                val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "prog")
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun NetworkMiniChartCard(ping: Int, onTest: () -> Unit) {
    var history by remember { mutableStateOf(List(10) { 100 }) }
    
    LaunchedEffect(ping) {
        if (ping > 0) {
            history = (history.drop(1) + ping).toMutableList()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Network Latency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (ping <= 0) "..." else "$ping",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (ping in 1..100) MaterialTheme.colorScheme.primary else if (ping > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ms", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val chartColor = MaterialTheme.colorScheme.secondary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxVal = (history.maxOrNull() ?: 100).coerceAtLeast(100).toFloat()
                    val stepX = size.width / (history.size - 1)
                    val scaleY = size.height / maxVal
                    
                    val path = Path()
                    history.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = size.height - (value * scaleY)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path = path, color = chartColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            IconButton(onClick = onTest, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Icon(Icons.Filled.Refresh, contentDescription = "Test", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SmartRecommendations(batteryTemp: Float, ping: Int) {
    val recommendations = mutableListOf<String>()
    if (batteryTemp > 40) recommendations.add("Device temperature critical. Cooling recommended.")
    else if (batteryTemp > 37) recommendations.add("Device warming up. Closing background apps.")
    
    if (ping > 100) recommendations.add("High latency detected. Switch to Wi-Fi.")

    AnimatedVisibility(visible = recommendations.isNotEmpty(), enter = scaleIn(tween(400))) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("System Alerts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                recommendations.forEach { rec ->
                    Text("• $rec", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
fun ProToolsGrid(navController: NavController) {
    Text("Pro Toolbox", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Spacer(modifier = Modifier.height(8.dp))
    
    val tools = listOf(
        Triple("Gaming Visual Mode", "visual", Icons.Filled.DisplaySettings),
        Triple("Smart Guide Mode", "guide", Icons.Filled.MenuBook),
        Triple("Battery Monitor", "battery", Icons.Filled.BatteryChargingFull),
        Triple("Network Tracker", "network", Icons.Filled.Wifi),
        Triple("App Usage", "usage", Icons.Filled.DataUsage),
        Triple("Session History", "history", Icons.Filled.History),
        Triple("Analytics", "analytics", Icons.Filled.Analytics)
    )
    
    tools.chunked(2).forEach { rowTools ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            rowTools.forEach { tool ->
                Card(
                    modifier = Modifier.weight(1f).height(90.dp).clickable { navController.navigate(tool.second) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(tool.third, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(tool.first, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            if (rowTools.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

