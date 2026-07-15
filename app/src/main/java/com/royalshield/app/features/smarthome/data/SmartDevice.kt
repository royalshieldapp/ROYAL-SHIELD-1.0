package com.royalshield.app.features.smarthome.data

enum class DeviceType {
    LIGHT, PLUG, THERMOSTAT, CAMERA, LOCK, SPEAKER, TV, OTHER
}

data class SmartDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isConnected: Boolean = false,
    val isOn: Boolean = false,
    val brightness: Float = 1f,
    val lightColorArgb: Long = 0xFFFFE4B5,
    val provider: String = DEMO_PROVIDER,
    val endpointLabel: String = "Demo device"
) {
    companion object {
        const val DEMO_PROVIDER = "Royal Shield Demo"
        const val GOOGLE_HOME_PROVIDER = "Google Home"
    }
}
