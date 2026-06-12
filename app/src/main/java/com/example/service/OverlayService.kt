package com.example.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.SettingsRepository
import com.example.domain.BoosterManager
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var boosterManager: BoosterManager
    private val lifecycleOwner = MyLifecycleOwner()

    companion object {
        const val CHANNEL_ID = "floating_dashboard_channel"
        const val NOTIFICATION_ID = 300
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        boosterManager = BoosterManager(this)
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ML Booster")
            .setContentText("Gaming Dashboard Active")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
        
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun showOverlay() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            
            setContent {
                MyApplicationTheme {
                    var expanded by remember { mutableStateOf(false) }
                    var transparency by remember { mutableStateOf(0.8f) }
                    var scaleSize by remember { mutableStateOf(1f) }

                    LaunchedEffect(Unit) {
                        transparency = settingsRepository.overlayTransparency.first()
                        scaleSize = settingsRepository.overlaySize.first()
                    }

                    Box(
                        modifier = Modifier
                            .scale(scaleSize)
                            .alpha(transparency)
                            .wrapContentSize()
                    ) {
                        DashboardContent(
                            expanded = expanded,
                            onToggle = { expanded = !expanded },
                            onClose = { stopSelf() },
                            onDrag = { dx, dy -> updateOverlayPosition(dx, dy) }
                        )
                    }
                }
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        windowManager?.addView(composeView, layoutParams)
    }

    private fun updateOverlayPosition(dx: Float, dy: Float) {
        layoutParams?.let { params ->
            params.x += dx.toInt()
            params.y += dy.toInt()
            windowManager?.updateViewLayout(composeView, params)
        }
    }

    @Composable
    fun DashboardContent(expanded: Boolean, onToggle: () -> Unit, onClose: () -> Unit, onDrag: (Float, Float) -> Unit) {
        var ping by remember { mutableStateOf(-1) }
        var temp by remember { mutableStateOf(0f) }
        var battery by remember { mutableStateOf(0) }
        var ram by remember { mutableStateOf(0L) }
        var playTime by remember { mutableStateOf(0L) } // in seconds

        val context = this@OverlayService
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        // Data updaters
        LaunchedEffect(expanded) {
            if (expanded) {
                while (true) {
                    val memoryInfo = ActivityManager.MemoryInfo()
                    am.getMemoryInfo(memoryInfo)
                    ram = memoryInfo.availMem / (1024 * 1024)
                    
                    ping = boosterManager.pingTest()
                    delay(3000)
                }
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                playTime += 1
                delay(1000)
            }
        }
        
        val batteryReceiver = remember {
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val tempVal = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
                    if (tempVal > 0) temp = tempVal / 10.0f
                    
                    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    if (level != -1 && scale != -1) battery = (level * 100 / scale.toFloat()).toInt()
                }
            }
        }
        
        DisposableEffect(Unit) {
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            onDispose { context.unregisterReceiver(batteryReceiver) }
        }

        Column(
            modifier = Modifier
                .wrapContentSize()
                .width(200.dp)
        ) {
            // Header (Draggable)
            Surface(
                shape = if (expanded) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) else RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Gamepad, contentDescription = "Toggle", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = String.format("%02d:%02d", playTime / 60, playTime % 60),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Expanded Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow(Icons.Filled.NetworkWifi, "Ping", if (ping == -1) "..." else "${ping}ms", if (ping > 100) Color.Red else Color.Green)
                        InfoRow(Icons.Filled.Thermostat, "Temp", "${temp}°C", if (temp >= 40) Color.Red else Color(0xFFFFA500))
                        InfoRow(Icons.Filled.Memory, "RAM", "${ram}MB", MaterialTheme.colorScheme.primary)
                        InfoRow(null, "Battery", "${battery}%", if (battery < 20) Color.Red else Color.Green)
                    }
                }
            }
        }
    }

    @Composable
    fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector?, label: String, value: String, valueColor: Color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(value, style = MaterialTheme.typography.labelMedium, color = valueColor)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gaming Dashboard",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        
        job.cancel()
        composeView?.let {
            windowManager?.removeView(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class MyLifecycleOwner : SavedStateRegistryOwner, LifecycleOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
