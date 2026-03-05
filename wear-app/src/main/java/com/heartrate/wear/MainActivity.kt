package com.heartrate.wear

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.heartrate.shared.data.repository.HeartRateRepositoryImpl
import com.heartrate.shared.domain.usecase.ObserveHeartRate
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private val observeHeartRate = ObserveHeartRate(HeartRateRepositoryImpl())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textView = TextView(this)
        textView.text = "Heart Rate: --"
        textView.textSize = 24f
        textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        setContentView(textView)

        startMonitoring()
    }

    private fun startMonitoring() {
        lifecycleScope.launch {
            try {
                observeHeartRate.start()

                observeHeartRate().collect { data ->
                    runOnUiThread {
                        textView.text = "Heart Rate:\n${data.heartRate} BPM"
                    }
                }
            } catch (e: Exception) {
                textView.text = "Error: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            observeHeartRate.stop()
        }
    }
}
