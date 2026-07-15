package com.royalshield.app.features.smarthome.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartDeviceDao {
    @Query("SELECT * FROM smart_devices WHERE isConnected = 1 AND provider != 'Royal Shield Demo' ORDER BY name")
    fun observeConnectedDevices(): Flow<List<SmartDeviceEntity>>

    @Query("SELECT * FROM smart_devices WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SmartDeviceEntity?

    @Upsert
    suspend fun upsert(device: SmartDeviceEntity)
}
