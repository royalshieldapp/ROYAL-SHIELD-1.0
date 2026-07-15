package com.royalshield.app.features.smarthome

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.data.db.AppDatabase
import com.royalshield.app.features.smarthome.data.SmartDevice
import com.royalshield.app.features.smarthome.data.SmartHomeRepository
import com.royalshield.app.features.smarthome.google.GoogleHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmartHomeUiState(
    val isScanning: Boolean = false,
    val scannedDevices: List<SmartDevice> = emptyList(),
    val connectedDevices: List<SmartDevice> = emptyList(),
    val message: String? = null,
    val isDemoProvider: Boolean = false
)

class SmartHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SmartHomeRepository = GoogleHomeRepository(
        application,
        AppDatabase.getDatabase(application).smartDeviceDao()
    )
    private val _uiState = MutableStateFlow(SmartHomeUiState())
    val uiState: StateFlow<SmartHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.connectedDevices.collect { connected ->
                _uiState.update { state ->
                    state.copy(
                        connectedDevices = connected,
                        scannedDevices = state.scannedDevices.filterNot { candidate ->
                            connected.any { it.id == candidate.id }
                        }
                    )
                }
            }
        }
    }

    fun scan() {
        if (_uiState.value.isScanning) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _uiState.update { it.copy(message = "Google Home requires Android 10 or newer") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, message = null) }
            runCatching {
                val authorized = repository.hasPermission() || repository.requestPermission()
                if (!authorized) error("Google Home permission was not granted")
                repository.scan()
            }
                .onSuccess { found ->
                    val connectedIds = _uiState.value.connectedDevices.mapTo(mutableSetOf()) { it.id }
                    val available = found.filterNot { it.id in connectedIds }
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            scannedDevices = available,
                            message = if (available.isEmpty()) "No new devices found" else null
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isScanning = false, message = "Google Home authorization or scan failed")
                    }
                }
        }
    }

    fun connect(device: SmartDevice) {
        viewModelScope.launch {
            runCatching { repository.save(device.copy(isConnected = true)) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            scannedDevices = state.scannedDevices.filterNot { it.id == device.id },
                            message = "Connected to ${device.name}"
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(message = "Unable to save device") } }
        }
    }

    fun updateDevice(device: SmartDevice) {
        viewModelScope.launch {
            runCatching { repository.save(device.copy(brightness = device.brightness.coerceIn(0f, 1f))) }
                .onFailure { _uiState.update { it.copy(message = "Unable to update device") } }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
