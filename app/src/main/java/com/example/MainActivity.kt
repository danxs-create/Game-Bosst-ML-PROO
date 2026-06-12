package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.service.TemperatureMonitorService
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start Temperature Monitor Service
        val serviceIntent = Intent(this, TemperatureMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(androidx.compose.ui.platform.LocalContext.current.applicationContext))
    val featuresViewModel: com.example.ui.FeaturesViewModel = viewModel()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
        LaunchedEffect(Unit) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    NavHost(
        navController = navController, 
        startDestination = "dashboard",
        enterTransition = { fadeIn(animationSpec = tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
    ) {
        composable("dashboard") {
            DashboardScreen(viewModel = viewModel, navController = navController)
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel, navController = navController)
        }
        composable("battery") {
            com.example.ui.screens.BatteryMonitorScreen(featuresViewModel = featuresViewModel, navController = navController)
        }
        composable("network") {
            com.example.ui.screens.NetworkMonitorScreen(featuresViewModel = featuresViewModel, navController = navController)
        }
        composable("usage") {
            com.example.ui.screens.AppUsageScreen(featuresViewModel = featuresViewModel, navController = navController)
        }
        composable("history") {
            com.example.ui.screens.SessionLogScreen(featuresViewModel = featuresViewModel, navController = navController)
        }
        composable("analytics") {
            com.example.ui.screens.AnalyticsScreen(featuresViewModel = featuresViewModel, navController = navController)
        }
        composable("visual") {
            com.example.ui.screens.GamingVisualScreen(featuresViewModel = featuresViewModel, navController = navController)
        }
        composable("guide") {
            com.example.ui.screens.SmartGuideScreen(featuresViewModel = featuresViewModel, navController = navController)
        }
    }
}
