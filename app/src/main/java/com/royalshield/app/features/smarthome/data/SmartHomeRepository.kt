package com.royalshield.app.features.smarthome.data

import kotlinx.coroutines.flow.Flow

interface SmartHomeRepository {
    val connectedDevices: Flow<List<SmartDevice>>
    suspend fun hasPermission(): Boolean
    suspend fun requestPermission(): Boolean
    suspend fun scan(): List<SmartDevice>
    suspend fun save(device: SmartDevice)
}
