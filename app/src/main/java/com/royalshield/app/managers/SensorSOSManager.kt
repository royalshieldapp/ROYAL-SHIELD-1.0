package com.royalshield.app.managers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Manager to detect sudden impacts or falls using device sensors.
 * Triggers the SOS sequence if a high G-force event is detected.
 */
class SensorSOSManager(
    private val context: Context,
    private val onImpactDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastUpdate: Long = 0
    private val IMPACT_THRESHOLD = 30.0f // High G-force threshold for impact (approx 3G)
    private val SHAKE_THRESHOLD = 800 // Threshold for vigorous shaking

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("SensorSOSManager", "Accelerometer monitoring started.")
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d("SensorSOSManager", "Accelerometer monitoring stopped.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate magnitude of acceleration
            val acceleration = sqrt(x * x + y * y + z * z)

            if (acceleration > IMPACT_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdate > 2000) { // Avoid multiple triggers for same event
                    lastUpdate = currentTime
                    Log.w("SensorSOSManager", "High impact detected! G-Force: ${acceleration/9.8f}")
                    onImpactDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
