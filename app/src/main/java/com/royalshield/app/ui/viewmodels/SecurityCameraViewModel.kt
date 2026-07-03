package com.royalshield.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SecurityCameraState(
    val isStreaming: Boolean = false,
    val useFrontCamera: Boolean = false,
    val resolution: String = "1080p",
    val bitRate: String = "2.5 Mbps",
    val streamUrl: String = "rtmp://api.royalshield.net/live/stream_304",
    val statusLogs: List<String> = emptyList(),
    // New fields for real camera logic
    val isRecording: Boolean = false,
    val flashEnabled: Boolean = false,
    val micEnabled: Boolean = true,
    val recordingDurationSeconds: Int = 0,
    val isFullscreen: Boolean = false
)

class SecurityCameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SecurityCameraState())
    val state: StateFlow<SecurityCameraState> = _state.asStateFlow()
    
    private var recordingTimerJob: Job? = null

    fun toggleStreaming() {
        _state.update {
            val nextState = !it.isStreaming
            val newLogs = if (nextState) {
                listOf(
                    "[${getTimestamp()}] Initializing live video stream...",
                    "[${getTimestamp()}] Handshaking with secure ingestion server...",
                    "[${getTimestamp()}] 🟢 Active streaming feed: ${it.streamUrl}"
                ) + it.statusLogs
            } else {
                listOf("[${getTimestamp()}] Live feed transmission terminated safely.") + it.statusLogs
            }
            it.copy(isStreaming = nextState, statusLogs = newLogs.take(50))
        }
    }

    fun toggleCameraSource() {
        _state.update {
            val cameraName = if (!it.useFrontCamera) "Front-Facing Shield Cam" else "Main Rear Cam"
            val logMessage = "[${getTimestamp()}] Hot-swapping camera feed to: $cameraName"
            it.copy(
                useFrontCamera = !it.useFrontCamera,
                statusLogs = listOf(logMessage) + it.statusLogs.take(49)
            )
        }
    }

    fun toggleFlash() {
        _state.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun toggleMic() {
        _state.update { it.copy(micEnabled = !it.micEnabled) }
    }
    
    fun toggleFullscreen() {
        _state.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun startRecording() {
        _state.update { it.copy(isRecording = true, recordingDurationSeconds = 0) }
        startTimer()
    }

    fun stopRecording() {
        _state.update { it.copy(isRecording = false) }
        recordingTimerJob?.cancel()
    }
    
    fun logPhotoCaptured() {
        _state.update {
            val logMessage = "[${getTimestamp()}] 📸 Secure snapshot captured and saved."
            it.copy(statusLogs = listOf(logMessage) + it.statusLogs.take(49))
        }
    }

    private fun startTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.copy(recordingDurationSeconds = it.recordingDurationSeconds + 1) }
            }
        }
    }

    private fun getTimestamp(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
