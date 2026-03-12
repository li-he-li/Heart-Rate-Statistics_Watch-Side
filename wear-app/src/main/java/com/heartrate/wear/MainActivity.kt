package com.heartrate.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.heartrate.shared.presentation.model.ConnectionStatus
import com.heartrate.shared.presentation.model.HeartRateUiState
import com.heartrate.shared.presentation.viewmodel.HeartRateViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val sharedViewModel: HeartRateViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WearPermissionGate(sharedViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.onCleared()
    }
}

@Composable
private fun WearPermissionGate(viewModel: HeartRateViewModel) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasBodySensorsPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(context) {
        hasPermission = hasBodySensorsPermission(context)
    }

    if (hasPermission) {
        WearNavApp(viewModel)
    } else {
        PermissionScreen(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.BODY_SENSORS) }
        )
    }
}

private fun hasBodySensorsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BODY_SENSORS
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun WearNavApp(viewModel: HeartRateViewModel) {
    val navController = rememberSwipeDismissableNavController()

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onCleared()
        }
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "monitor"
    ) {
        composable("monitor") {
            MonitorScreen(
                uiState = viewModel.uiState.collectAsState().value,
                onOpenConnection = { navController.navigate("connection") }
            )
        }

        composable("connection") {
            ConnectionScreen(
                uiState = viewModel.uiState.collectAsState().value,
                onConnectWebSocket = { viewModel.connectWebSocket("ws://127.0.0.1:8080/heartrate") },
                onDisconnectWebSocket = { viewModel.disconnectWebSocket() },
                onStartBle = { viewModel.startBLE() },
                onStopBle = { viewModel.stopBLE() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PermissionScreen(onRequestPermission: () -> Unit) {
    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text("Sensor Permission")
            }
            item {
                Text("BODY_SENSORS is required to read heart rate.")
            }
            item {
                Chip(
                    onClick = onRequestPermission,
                    label = { Text("Grant Permission") },
                    colors = ChipDefaults.primaryChipColors()
                )
            }
        }
    }
}

@Composable
private fun MonitorScreen(
    uiState: HeartRateUiState,
    onOpenConnection: () -> Unit
) {
    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(text = "Heart Rate")
            }
            item {
                Text(
                    text = if (uiState.currentHeartRate > 0) "${uiState.currentHeartRate} BPM" else "-- BPM"
                )
            }
            item {
                Text(
                    text = when {
                        uiState.isMonitoring && uiState.connectionStatus == ConnectionStatus.CONNECTED -> "Connected"
                        uiState.isMonitoring -> "Connecting"
                        else -> "Stopped"
                    }
                )
            }
            item {
                Text(text = "Battery: ${uiState.batteryLevel?.toString() ?: "--"}%")
            }
            item {
                Chip(
                    onClick = onOpenConnection,
                    label = { Text("Connections") },
                    colors = ChipDefaults.primaryChipColors()
                )
            }
        }
    }
}

@Composable
private fun ConnectionScreen(
    uiState: HeartRateUiState,
    onConnectWebSocket: () -> Unit,
    onDisconnectWebSocket: () -> Unit,
    onStartBle: () -> Unit,
    onStopBle: () -> Unit,
    onBack: () -> Unit
) {
    val actions = listOf(
        "Connect WS" to onConnectWebSocket,
        "Disconnect WS" to onDisconnectWebSocket,
        "Start BLE" to onStartBle,
        "Stop BLE" to onStopBle,
        "Back" to onBack
    )

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(text = "Connection")
            }
            item {
                Text(text = "Status: ${uiState.connectionStatus.name}")
            }
            item {
                Text(text = "Error: ${uiState.errorMessage ?: "None"}")
            }
            items(actions) { (label, action) ->
                Chip(
                    onClick = action,
                    label = { Text(label) }
                )
            }
        }
    }
}
