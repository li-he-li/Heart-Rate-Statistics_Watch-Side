package com.heartrate.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.heartrate.shared.di.getAppModules
import com.heartrate.shared.presentation.viewmodel.HeartRateViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

fun main() = application {
    // Initialize Koin if not already started
    if (org.koin.core.context.GlobalContext.getOrNull() == null) {
        startKoin {
            modules(getAppModules())
        }
    }

    // Create a KoinComponent to access dependencies
    val app = DesktopHeartRateApp()

    Window(
        onCloseRequest = {
            app.viewModel.onCleared()
            exitApplication()
        },
        title = "Heart Rate Monitor - Desktop",
        resizable = true
    ) {
        HeartRateScreen(app.viewModel)
    }
}

class DesktopHeartRateApp : KoinComponent {
    val viewModel: HeartRateViewModel by inject()
}

@Composable
fun HeartRateScreen(viewModel: HeartRateViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Start monitoring when screen is first created
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    // Cleanup when screen is destroyed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onCleared()
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Title
                Text(
                    text = "❤️ Heart Rate Monitor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 48.dp)
                )

                // Main Heart Rate Card
                Card(
                    modifier = Modifier
                        .width(500.dp)
                        .padding(24.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Heart Rate Label
                        Text(
                            text = "Current Heart Rate",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        // Heart Rate Value
                        Text(
                            text = if (uiState.currentHeartRate > 0) {
                                "${uiState.currentHeartRate}"
                            } else {
                                "--"
                            },
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // BPM Label
                        Text(
                            text = "BPM",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 40.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 32.dp),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Connection Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isConnected = uiState.connectionStatus == com.heartrate.shared.presentation.model.ConnectionStatus.CONNECTED
                            Text(
                                text = when {
                                    uiState.isMonitoring && isConnected -> "● "
                                    uiState.isMonitoring -> "◎ "
                                    else -> "○ "
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = when {
                                    uiState.isMonitoring && isConnected ->
                                        MaterialTheme.colorScheme.primary
                                    uiState.isMonitoring -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = when {
                                    uiState.isMonitoring && isConnected ->
                                        "Connected"
                                    uiState.isMonitoring -> "Connecting..."
                                    else -> "Stopped"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = when {
                                    uiState.isMonitoring && isConnected ->
                                        MaterialTheme.colorScheme.primary
                                    uiState.isMonitoring -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }

                        // Battery Level
                        if (uiState.batteryLevel != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "🔋 ${uiState.batteryLevel}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Error Message
                        uiState.errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "⚠️ $error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Device Info
                if (uiState.deviceInfo != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        modifier = Modifier.width(500.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "💻 Connected Device: ${uiState.deviceInfo}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // App Type Label
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "💻 Desktop Visualization App",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
