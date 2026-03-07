package com.heartrate.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.heartrate.shared.di.getAppModules
import com.heartrate.shared.presentation.model.ConnectionStatus
import com.heartrate.shared.presentation.model.HeartRateUiState
import com.heartrate.shared.presentation.viewmodel.HeartRateViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

fun main() = application {
    if (org.koin.core.context.GlobalContext.getOrNull() == null) {
        startKoin {
            modules(getAppModules())
        }
    }

    val app = DesktopHeartRateApp()

    Window(
        onCloseRequest = {
            app.viewModel.onCleared()
            exitApplication()
        },
        title = "Heart Rate Monitor - Desktop",
        resizable = true
    ) {
        DesktopNavApp(app.viewModel)
    }
}

class DesktopHeartRateApp : KoinComponent {
    val viewModel: HeartRateViewModel by inject()
}

private enum class DesktopRoute(val title: String) {
    MONITOR("Monitor"),
    CONNECTION("Connection"),
    ABOUT("About")
}

@Composable
private fun DesktopNavApp(viewModel: HeartRateViewModel) {
    var currentRoute by remember { mutableStateOf(DesktopRoute.MONITOR) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

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
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    DesktopRoute.entries.forEach { route ->
                        TextButton(
                            onClick = { currentRoute = route },
                            enabled = currentRoute != route
                        ) {
                            Text(route.title)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (currentRoute) {
                    DesktopRoute.MONITOR -> MonitorScreen(uiState)
                    DesktopRoute.CONNECTION -> ConnectionScreen(
                        uiState = uiState,
                        onConnectWebSocket = { viewModel.connectWebSocket("ws://localhost:8080") },
                        onDisconnectWebSocket = { viewModel.disconnectWebSocket() },
                        onStartBle = { viewModel.startBLE() },
                        onStopBle = { viewModel.stopBLE() }
                    )
                    DesktopRoute.ABOUT -> AboutScreen()
                }
            }
        }
    }
}

@Composable
private fun MonitorScreen(uiState: HeartRateUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Heart Rate Monitor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .width(500.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (uiState.currentHeartRate > 0) "${uiState.currentHeartRate}" else "--",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "BPM",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 24.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                val isConnected = uiState.connectionStatus == ConnectionStatus.CONNECTED
                Text(
                    text = when {
                        uiState.isMonitoring && isConnected -> "Connected"
                        uiState.isMonitoring -> "Connecting..."
                        else -> "Stopped"
                    },
                    color = when {
                        uiState.isMonitoring && isConnected -> MaterialTheme.colorScheme.primary
                        uiState.isMonitoring -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Battery: ${uiState.batteryLevel?.toString() ?: "--"}%")
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
    onStopBle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connection Controls",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Status: ${uiState.connectionStatus.name}")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Error: ${uiState.errorMessage ?: "None"}",
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onConnectWebSocket) { Text("Connect WS") }
            Button(onClick = onDisconnectWebSocket) { Text("Disconnect WS") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartBle) { Text("Start BLE") }
            Button(onClick = onStopBle) { Text("Stop BLE") }
        }
    }
}

@Composable
private fun AboutScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Desktop App",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "KMP visualization scaffold with navigation.",
            textAlign = TextAlign.Center
        )
    }
}
