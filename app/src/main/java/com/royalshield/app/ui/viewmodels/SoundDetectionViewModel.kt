package com.royalshield.app.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.SoundDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SoundDetectionState(
    val isMonitoring: Boolean = false,
    val currentDb: Float = 0f,
    val maxDb: Float = 0f,
    val thresholdAmplitude: Int = 15000,
    val logs: List<String> = emptyList(),
    val errorMessage: String? = null
)

class SoundDetectionViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SoundDetectionState())
    val state: StateFlow<SoundDetectionState> = _state.asStateFlow()

    private var soundDetector: SoundDetector? = null

    init {
        setupSoundDetector()
    }

    private fun setupSoundDetector() {
        val context = getApplication<Application>().applicationContext
        soundDetector = SoundDetector(
            context = context,
            amplitudeThreshold = _state.value.thresholdAmplitude,
            onNoiseDetected = {
                handleNoiseDetected()
            },
            onAmplitudeUpdate = { _, db ->
                _state.update {
                    it.copy(
                        currentDb = db.toFloat(),
                        maxDb = maxOf(it.maxDb, db.toFloat())
                    )
                }
            },
            onError = { error ->
                _state.update { it.copy(errorMessage = error, isMonitoring = false) }
            }
        )
    }

    fun startMonitoring() {
        soundDetector?.start()
        _state.update { it.copy(isMonitoring = soundDetector?.isRunning == true, errorMessage = null) }
        addLog("Ambient monitoring initialized.")
    }

    fun stopMonitoring() {
        soundDetector?.stop()
        _state.update { it.copy(isMonitoring = false, currentDb = 0f) }
        addLog("Ambient monitoring stopped.")
    }

    fun setThreshold(percentage: Float) {
        // Map 0..1 percentage to 1000..32767 amplitude
        val amplitude = (1000 + (percentage * 31767)).toInt()
        soundDetector?.setThreshold(amplitude)
        _state.update { it.copy(thresholdAmplitude = amplitude) }
    }

    private fun handleNoiseDetected() {
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            addLog("⚠️ Noise Limit Exceeded at $timestamp!")
            // You can optionally link this to actual SOS alert actions here.
        }
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val formattedLog = "[$timestamp] $message"
        _state.update { it.copy(logs = listOf(formattedLog) + it.logs.take(49)) }
    }

    override fun onCleared() {
        super.onCleared()
        soundDetector?.stop()
    }
}
