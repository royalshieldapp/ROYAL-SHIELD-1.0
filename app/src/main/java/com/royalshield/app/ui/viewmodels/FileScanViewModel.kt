package com.royalshield.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.VirusTotalRepository
import com.royalshield.app.calculateSHA256
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Result(val isThreat: Boolean, val message: String) : ScanState()
    data class Error(val error: String) : ScanState()
}

class FileScanViewModel : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val repository = VirusTotalRepository()

    fun scanFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            try {
                // Resolve file from URI (Simplified for demo, requires content resolver in production)
                // Assuming we can get a path or stream. For VT we need file or hash.
                
                // 1. Try to get file path (Does not work for all URIs, especially from pickers)
                // For this demo, let's assume we can read the stream to calc hash.
                
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _scanState.value = ScanState.Error("Cannot open file")
                    return@launch
                }
                
                // Calculate Hash
                val buffer = ByteArray(8192)
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                inputStream.close()
                val hash = digest.digest().joinToString("") { "%02x".format(it) }

                // 2. Check Hash with VT
                val result = repository.scanFileHash(hash)
                
                // Add a 3 second artificial delay for the visual effect
                kotlinx.coroutines.delay(3000)
                
                if (result.severity == "API Key missing") {
                     // Fallback mock scan if no key
                    _scanState.value = ScanState.Result(true, "Threat Detected (Mock Scan)\n\nDetails:\nNo API Key found.\nSHA-256: ${hash.take(24)}...")
                } else {
                    val isThreat = result.severity == "high" || result.severity == "medium"
                    _scanState.value = ScanState.Result(isThreat, "Scan Completed\n\nDetails:\nSeverity: ${result.severity.uppercase()}\nSHA-256: ${hash.take(24)}...")
                }

            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun reset() {
        _scanState.value = ScanState.Idle
    }
}
