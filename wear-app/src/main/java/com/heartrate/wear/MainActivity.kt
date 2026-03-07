package com.heartrate.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
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
                WearNavApp(sharedViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.onCleared()
    }
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
                onConnectWebSocket = { viewModel.connectWebSocket("ws://localhost:8080") },
                onDisconnectWebSocket = { viewModel.disconnectWebSocket() },
                onStartBle = { viewModel.startBLE() },
                onStopBle = { viewModel.stopBLE() },
                onBack = { navController.popBackStack() }
            )
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
