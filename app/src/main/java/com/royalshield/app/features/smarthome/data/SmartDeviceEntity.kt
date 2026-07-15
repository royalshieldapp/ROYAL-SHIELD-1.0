package com.royalshield.app.features.smarthome.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_devices")
data class SmartDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val isConnected: Boolean,
    val isOn: Boolean,
    val brightness: Float,
    val lightColorArgb: Long,
    val provider: String,
    val endpointLabel: String
)

fun SmartDeviceEntity.toDomain() = SmartDevice(
    id = id,
    name = name,
    type = runCatching { DeviceType.valueOf(type) }.getOrDefault(DeviceType.OTHER),
    isConnected = isConnected,
    isOn = isOn,
    brightness = brightness,
    lightColorArgb = lightColorArgb,
    provider = provider,
    endpointLabel = endpointLabel
)

fun SmartDevice.toEntity() = SmartDeviceEntity(
    id = id,
    name = name,
    type = type.name,
    isConnected = isConnected,
    isOn = isOn,
    brightness = brightness.coerceIn(0f, 1f),
    lightColorArgb = lightColorArgb,
    provider = provider,
    endpointLabel = endpointLabel
)
