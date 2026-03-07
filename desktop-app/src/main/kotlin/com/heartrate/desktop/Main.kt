package com.heartrate.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.heartrate.shared.di.getAppModules
import com.heartrate.shared.presentation.model.ConnectionStatus
import com.heartrate.shared.presentation.model.HeartRateUiState
import com.heartrate.shared.presentation.viewmodel.HeartRateViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import java.awt.Point
import java.awt.Color as AwtColor
import java.awt.Window as AwtWindow
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import kotlin.math.abs

fun main() = application {
    if (org.koin.core.context.GlobalContext.getOrNull() == null) {
        startKoin {
            modules(getAppModules())
        }
    }

    val app = remember { DesktopHeartRateApp() }
    val viewModel = app.viewModel
    val uiState by viewModel.uiState.collectAsState()
    var lastNonZeroHeartRate by remember { mutableStateOf(0) }

    var compactMode by remember { mutableStateOf(false) }
    var currentRoute by remember { mutableStateOf(DesktopRoute.MONITOR) }

    val mainWindowState = rememberWindowState(width = 980.dp, height = 720.dp)
    val compactWindowState = rememberWindowState(width = 220.dp, height = 92.dp)
    val closeApplicationSafely = {
        viewModel.onCleared()
        exitApplication()
    }

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }
    LaunchedEffect(uiState.currentHeartRate) {
        if (uiState.currentHeartRate > 0) {
            lastNonZeroHeartRate = uiState.currentHeartRate
        }
    }

    if (compactMode) {
        Window(
            onCloseRequest = closeApplicationSafely,
            title = "Heart Rate Overlay",
            state = compactWindowState,
            transparent = true,
            undecorated = true,
            alwaysOnTop = true,
            resizable = false
        ) {
            CompactHeartRateOverlay(
                heartRate = if (uiState.currentHeartRate > 0) {
                    uiState.currentHeartRate
                } else {
                    lastNonZeroHeartRate
                },
                hostWindow = window,
                onExitCompactMode = {
                    compactMode = false
                    viewModel.startMonitoring()
                }
            )
        }
    } else {
        Window(
            onCloseRequest = closeApplicationSafely,
            title = "Heart Rate Monitor - Desktop",
            state = mainWindowState,
            resizable = true
        ) {
            DesktopMainContent(
                uiState = uiState,
                currentRoute = currentRoute,
                onRouteChange = { currentRoute = it },
                onEnableCompactMode = {
                    viewModel.startMonitoring()
                    compactMode = true
                },
                onConnectWebSocket = { viewModel.connectWebSocket("ws://localhost:8080") },
                onDisconnectWebSocket = { viewModel.disconnectWebSocket() },
                onStartBle = { viewModel.startBLE() },
                onStopBle = { viewModel.stopBLE() }
            )
        }
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
private fun DesktopMainContent(
    uiState: HeartRateUiState,
    currentRoute: DesktopRoute,
    onRouteChange: (DesktopRoute) -> Unit,
    onEnableCompactMode: () -> Unit,
    onConnectWebSocket: () -> Unit,
    onDisconnectWebSocket: () -> Unit,
    onStartBle: () -> Unit,
    onStopBle: () -> Unit
) {
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
                            onClick = { onRouteChange(route) },
                            enabled = currentRoute != route
                        ) {
                            Text(route.title)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (currentRoute) {
                    DesktopRoute.MONITOR -> MonitorScreen(
                        uiState = uiState,
                        onEnterCompactMode = onEnableCompactMode
                    )

                    DesktopRoute.CONNECTION -> ConnectionScreen(
                        uiState = uiState,
                        onConnectWebSocket = onConnectWebSocket,
                        onDisconnectWebSocket = onDisconnectWebSocket,
                        onStartBle = onStartBle,
                        onStopBle = onStopBle
                    )

                    DesktopRoute.ABOUT -> AboutScreen()
                }
            }
        }
    }
}

@Composable
private fun MonitorScreen(
    uiState: HeartRateUiState,
    onEnterCompactMode: () -> Unit
) {
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

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Need a minimal view? Switch to compact transparent overlay mode.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onEnterCompactMode) {
            Text("Enter Compact Overlay")
        }
    }
}

@Composable
private fun CompactHeartRateOverlay(
    heartRate: Int,
    hostWindow: AwtWindow,
    onExitCompactMode: () -> Unit
) {
    val longPressMs = 250L
    val moveThreshold = 2
    var dragStartPointer: Point? by remember { mutableStateOf(null) }
    var dragStartWindow: Point? by remember { mutableStateOf(null) }
    var pressTimeMs: Long by remember { mutableStateOf(0L) }

    DisposableEffect(hostWindow) {
        hostWindow.background = AwtColor(0, 0, 0, 0)

        val pressListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                pressTimeMs = System.currentTimeMillis()
                dragStartPointer = e.locationOnScreen
                dragStartWindow = hostWindow.location
            }
        }

        val dragListener = object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (System.currentTimeMillis() - pressTimeMs < longPressMs) return

                val pointer = dragStartPointer ?: return
                val origin = dragStartWindow ?: return
                val current = e.locationOnScreen
                val dx = current.x - pointer.x
                val dy = current.y - pointer.y
                if (abs(dx) < moveThreshold && abs(dy) < moveThreshold) return

                val nextX = origin.x + dx
                val nextY = origin.y + dy
                hostWindow.setLocation(nextX, nextY)
            }
        }

        hostWindow.addMouseListener(pressListener)
        hostWindow.addMouseMotionListener(dragListener)

        onDispose {
            hostWindow.removeMouseListener(pressListener)
            hostWindow.removeMouseMotionListener(dragListener)
        }
    }

    val heartRateText = if (heartRate > 0) heartRate.toString() else "0"

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u2764",
                color = Color(0xFFE53935),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onExitCompactMode() }
                    )
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = heartRateText,
                color = Color(0xFFE53935),
                fontSize = 50.sp,
                fontWeight = FontWeight.ExtraBold
            )
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
