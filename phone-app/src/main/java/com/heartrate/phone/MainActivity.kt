package com.heartrate.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heartrate.shared.presentation.viewmodel.HeartRateViewModel
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject

/**
 * Main Activity for Phone App
 *
 * Receives heart rate data from watch and relays to desktop
 */
class MainActivity : ComponentActivity() {
    private val sharedViewModel: HeartRateViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                HeartRateScreen(sharedViewModel)
            }
        }
    }
}

@Composable
fun HeartRateScreen(viewModel: HeartRateViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title
            Text(
                text = "❤️ Heart Rate Monitor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Main Heart Rate Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Heart Rate Label
                    Text(
                        text = "Current Heart Rate",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
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
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // BPM Label
                    Text(
                        text = "BPM",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 24.dp),
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
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Battery Level
                    if (uiState.batteryLevel != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "🔋 ${uiState.batteryLevel}%",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Error Message
                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "⚠️ $error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Device Info
            if (uiState.deviceInfo != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "📱 Device: ${uiState.deviceInfo}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // App Type Label
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "📲 Phone Relay App",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
