package com.heartrate.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.heartrate.shared.data.repository.HeartRateRepositoryImpl
import com.heartrate.shared.domain.usecase.ObserveHeartRate
import kotlinx.coroutines.launch

fun main() = application {
    val repository = HeartRateRepositoryImpl()
    val observeHeartRate = ObserveHeartRate(repository)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Heart Rate Monitor"
    ) {
        HeartRateScreen(observeHeartRate)
    }
}

@Composable
fun HeartRateScreen(observeHeartRate: ObserveHeartRate) {
    val scope = rememberCoroutineScope()
    var heartRate by remember { mutableStateOf(0) }
    var isListening by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                observeHeartRate.start()
                isListening = true

                observeHeartRate().collect { data ->
                    heartRate = data.heartRate
                }
            } catch (e: Exception) {
                isListening = false
            }
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
                Text(
                    text = "Heart Rate Monitor",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 48.dp)
                )

                Card(
                    modifier = Modifier
                        .width(400.dp)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Heart Rate",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Text(
                            text = "$heartRate",
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "BPM",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isListening) "● " else "○ ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isListening) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = if (isListening) "Monitoring Active" else "Stopped",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isListening) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Desktop Visualization App",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}
