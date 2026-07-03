package com.royalshield.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TriggerType {
    LOCATION_ENTER,
    LOCATION_EXIT,
    WIFI_CONNECT,
    WIFI_DISCONNECT,
    BLUETOOTH_CONNECT,
    TIME_SCHEDULE,
    POWER_CONNECTED,
    POWER_DISCONNECTED,
    ROUTE_DEVIATION,
    BATTERY_LOW
}

enum class ActionType {
    TOGGLE_VPN,
    TOGGLE_CAMERAS,
    SEND_SOS,
    LOCK_DEVICE,
    ACTIVATE_ALARM,
    MUTE_AUDIO,
    ALERT_PROMPT,
    SILENT_SOS
}

@Entity(tableName = "automation_rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val triggerType: TriggerType,
    val actionType: ActionType,
    val triggerParams: String = "",
    val actionParams: String = ""
)
