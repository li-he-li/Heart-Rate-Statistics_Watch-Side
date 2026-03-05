package com.heartrate.phone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartrate.shared.domain.usecase.ObserveHeartRate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HeartRateViewModel(
    private val observeHeartRate: ObserveHeartRate
) : ViewModel() {

    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            try {
                observeHeartRate.start()
                _isListening.value = true

                observeHeartRate().collect { data ->
                    _heartRate.value = data.heartRate
                }
            } catch (e: Exception) {
                _isListening.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            observeHeartRate.stop()
            _isListening.value = false
        }
    }
}
