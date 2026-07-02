package com.royalshield.app

import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import kotlin.math.log10

/**
 * Manages ambient sound detection to trigger an alert.
 * Calculates decibels (dB) dynamically and supports continuous updates.
 */
class SoundDetector(
    private val context: Context,
    private var amplitudeThreshold: Int = 15000,
    private val onNoiseDetected: () -> Unit,
    private val onAmplitudeUpdate: ((amplitude: Int, db: Double) -> Unit)? = null,
    private val onError: ((String) -> Unit)? = null
) {

    private var mediaRecorder: MediaRecorder? = null
    private val handler = Handler(Looper.getMainLooper())
    var isRunning = false
        private set

    private val amplitudeChecker = object : Runnable {
        override fun run() {
            if (isRunning) {
                try {
                    val amplitude = mediaRecorder?.maxAmplitude ?: 0
                    // Calculate decibels: 20 * log10(amplitude / reference_amplitude)
                    // If amplitude is 0, use a low floor value to avoid negative infinity
                    val db = if (amplitude > 0) 20 * log10(amplitude.toDouble() / 2700.0) + 40.0 else 0.0
                    val clampedDb = db.coerceIn(0.0, 120.0)

                    Log.d("SoundDetector", "Amplitude: $amplitude | dB: $clampedDb")
                    onAmplitudeUpdate?.invoke(amplitude, clampedDb)

                    if (amplitude > amplitudeThreshold) {
                        Log.w("SoundDetector", "Noise threshold exceeded! Amplitude: $amplitude | dB: $clampedDb")
                        onNoiseDetected()
                    }
                } catch (e: Exception) {
                    Log.e("SoundDetector", "Error in amplitude checker: ${e.message}")
                    onError?.invoke(e.message ?: "Unknown checker error")
                }

                handler.postDelayed(this, 300) // Check faster (every 300 ms) for smooth UI feedback
            }
        }
    }

    /**
     * Set a new threshold value dynamically.
     */
    fun setThreshold(newThreshold: Int) {
        this.amplitudeThreshold = newThreshold
    }

    /**
     * Starts sound detection.
     */
    fun start() {
        if (isRunning) return

        try {
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile("/dev/null") // We don't need to save the file
                prepare()
                start()
            }

            isRunning = true
            handler.post(amplitudeChecker)
            Log.d("SoundDetector", "Sound detection started.")

        } catch (e: SecurityException) {
            Log.e("SoundDetector", "Permission missing for MediaRecorder: ${e.message}")
            onError?.invoke("Microphone permission required")
            mediaRecorder = null
        } catch (e: IOException) {
            Log.e("SoundDetector", "Error starting MediaRecorder: ${e.message}")
            onError?.invoke("Failed to initialize microphone sensor")
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e("SoundDetector", "Unexpected error starting sound detector: ${e.message}")
            onError?.invoke("Sensor initialization error: ${e.message}")
            mediaRecorder = null
        }
    }

    /**
     * Stops sound detection.
     */
    fun stop() {
        if (!isRunning) return

        isRunning = false
        handler.removeCallbacks(amplitudeChecker)

        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e("SoundDetector", "Error stopping MediaRecorder: ${e.message}")
            }
        }
        mediaRecorder = null
        Log.d("SoundDetector", "Sound detection stopped.")
    }
}

