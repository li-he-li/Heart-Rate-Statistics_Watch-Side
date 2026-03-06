package com.heartrate.wear

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.heartrate.shared.presentation.model.HeartRateUiState
import com.heartrate.shared.presentation.viewmodel.HeartRateViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Main Activity for Wear OS App
 *
 * Displays heart rate data from the watch's sensor
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: HeartRateViewModel by inject()

    private lateinit var heartRateText: TextView
    private lateinit var statusText: TextView
    private lateinit var batteryText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create UI programmatically for Wear OS
        setupUI()

        // Start monitoring
        viewModel.startMonitoring()

        // Observe UI state
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun setupUI() {
        // Create main layout
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.BLACK)
        }

        // Create heart rate display
        heartRateText = TextView(this).apply {
            text = "❤\n--"
            textSize = 48f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // Create status display
        statusText = TextView(this).apply {
            text = "● Disconnected"
            textSize = 14f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.GRAY)
        }

        // Create battery display
        batteryText = TextView(this).apply {
            text = "🔋 --%"
            textSize = 12f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.GRAY)
        }

        // Add views to layout
        layout.addView(heartRateText)
        layout.addView(statusText)
        layout.addView(batteryText)

        setContentView(layout)
    }

    private fun updateUI(state: HeartRateUiState) {
        // Update heart rate
        heartRateText.text = if (state.currentHeartRate > 0) {
            "❤\n${state.currentHeartRate}"
        } else {
            "❤\n--"
        }

        // Update status
        val isActive = state.connectionStatus == com.heartrate.shared.presentation.model.ConnectionStatus.CONNECTED
        statusText.text = if (state.isMonitoring) {
            "● Connected"
        } else {
            "○ Stopped"
        }
        statusText.setTextColor(
            when {
                state.isMonitoring && isActive -> Color.parseColor("#00FF00")
                state.isMonitoring -> Color.YELLOW
                else -> Color.GRAY
            }
        )

        // Update battery
        batteryText.text = if (state.batteryLevel != null) {
            "🔋 ${state.batteryLevel}%"
        } else {
            "🔋 --%"
        }

        // Show error if any
        state.errorMessage?.let { error ->
            statusText.text = "⚠ Error"
            statusText.setTextColor(Color.RED)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onCleared()
    }
}
