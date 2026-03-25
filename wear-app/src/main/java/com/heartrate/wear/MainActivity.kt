package com.heartrate.wear

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.heartrate.shared.presentation.model.ConnectionStatus
import com.heartrate.shared.presentation.model.HeartRateUiState
import com.heartrate.shared.presentation.viewmodel.HeartRateViewModel
import com.heartrate.wear.service.WearMonitoringForegroundService
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
        sharedViewModel.detachUi()
        super.onDestroy()
    }
}

@Composable
private fun WearPermissionGate(viewModel: HeartRateViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(hasBodySensorsPermission(context)) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(context) {
        hasPermission = hasBodySensorsPermission(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasBodySensorsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            WearMonitoringForegroundService.start(context)
        } else {
            WearMonitoringForegroundService.stop(context)
            viewModel.stopMonitoring()
        }
    }

    if (hasPermission) {
        WearNavApp(viewModel)
    } else {
        PermissionScreen(
            permissionDenied = permissionDenied,
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
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.detachUi()
        }
    }

    MonitorScreen(uiState = viewModel.uiState.collectAsState().value)
}

@Composable
private fun PermissionScreen(
    permissionDenied: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val permanentlyDenied = activity != null &&
        permissionDenied &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BODY_SENSORS)

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            item {
                Text("Sensor Permission", color = Color.White)
            }
            item {
                Text("BODY_SENSORS is required to read heart rate.", color = Color.White)
            }
            if (permissionDenied) {
                item {
                    Text("Permission denied. Monitoring cannot start.", color = Color.White)
                }
            }
            item {
                Chip(
                    onClick = onRequestPermission,
                    label = { Text("Grant Permission") },
                    colors = ChipDefaults.primaryChipColors()
                )
            }
            if (permanentlyDenied) {
                item {
                    Chip(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        },
                        label = { Text("Open Settings") }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorScreen(
    uiState: HeartRateUiState
) {
    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            item {
                Text(text = "Heart Rate", color = Color.White)
            }
            item {
                Text(
                    text = if (uiState.currentHeartRate > 0) "${uiState.currentHeartRate} BPM" else "-- BPM",
                    color = Color(0xFF00E676)
                )
            }
            item {
                Text(
                    text = when {
                        uiState.isMonitoring && uiState.connectionStatus == ConnectionStatus.CONNECTED -> "Connected"
                        uiState.isMonitoring -> "Connecting"
                        else -> "Stopped"
                    },
                    color = Color.White
                )
            }
            item {
                Text(text = "Battery: ${uiState.batteryLevel?.toString() ?: "--"}%", color = Color.White)
            }
        }
    }
}
