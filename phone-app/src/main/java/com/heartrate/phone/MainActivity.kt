package com.heartrate.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                PhoneApp(sharedViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.onCleared()
    }
}

private enum class PhoneRoute(val route: String, val title: String) {
    MONITOR("monitor", "Monitor"),
    CONNECTION("connection", "Connection")
}

@Composable
private fun PhoneApp(viewModel: HeartRateViewModel) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onCleared()
        }
    }

    Scaffold(
        topBar = {
            TopNavBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PhoneRoute.MONITOR.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(PhoneRoute.MONITOR.route) {
                MonitorScreen(viewModel.uiState.collectAsState().value)
            }

            composable(PhoneRoute.CONNECTION.route) {
                ConnectionScreen(
                    uiState = viewModel.uiState.collectAsState().value,
                    onConnectWebSocket = { viewModel.connectWebSocket("ws://localhost:8080") },
                    onDisconnectWebSocket = { viewModel.disconnectWebSocket() },
                    onStartBle = { viewModel.startBLE() },
                    onStopBle = { viewModel.stopBLE() }
                )
            }
        }
    }
}

@Composable
private fun TopNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        PhoneRoute.entries.forEach { item ->
            TextButton(
                onClick = { navController.navigate(item.route) },
                enabled = currentRoute != item.route
            ) {
                Text(text = item.title)
            }
        }
    }
}

@Composable
private fun MonitorScreen(uiState: HeartRateUiState) {
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
            Text(
                text = "Heart Rate Monitor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

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

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Battery: ${uiState.batteryLevel?.toString() ?: "--"}%")
                }
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
            Text(
                text = "Connection Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Status: ${uiState.connectionStatus.name}",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Error: ${uiState.errorMessage ?: "None"}",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onConnectWebSocket,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect WebSocket")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDisconnectWebSocket,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disconnect WebSocket")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onStartBle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start BLE")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onStopBle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop BLE")
            }
        }
    }
}
