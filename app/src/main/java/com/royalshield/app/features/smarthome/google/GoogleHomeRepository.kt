package com.royalshield.app.features.smarthome.google

import android.content.Context
import com.google.home.HomeDevice
import com.google.home.ForcePermissionFlow
import com.google.home.PermissionsResultStatus
import com.google.home.PermissionsState
import com.google.home.matter.standard.DimmableLightDevice
import com.google.home.matter.standard.DimmablePlugInUnitDevice
import com.google.home.matter.standard.ExtendedColorLightDevice
import com.google.home.matter.standard.LevelControl
import com.google.home.matter.standard.LevelControlTrait
import com.google.home.matter.standard.OnOff
import com.google.home.matter.standard.OnOffLightDevice
import com.google.home.matter.standard.OnOffPluginUnitDevice
import com.royalshield.app.features.smarthome.data.DeviceType
import com.royalshield.app.features.smarthome.data.SmartDevice
import com.royalshield.app.features.smarthome.data.SmartDeviceDao
import com.royalshield.app.features.smarthome.data.SmartDeviceEntity
import com.royalshield.app.features.smarthome.data.SmartHomeRepository
import com.royalshield.app.features.smarthome.data.toDomain
import com.royalshield.app.features.smarthome.data.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GoogleHomeRepository(
    context: Context,
    private val dao: SmartDeviceDao
) : SmartHomeRepository {
    private val client by lazy { GoogleHomeClientManager.getClient(context) }

    override val connectedDevices: Flow<List<SmartDevice>> =
        dao.observeConnectedDevices().map { devices -> devices.map(SmartDeviceEntity::toDomain) }

    override suspend fun hasPermission(): Boolean =
        client.hasPermissions()
            .filter { it != PermissionsState.PERMISSIONS_STATE_UNINITIALIZED }
            .first() == PermissionsState.GRANTED

    override suspend fun requestPermission(): Boolean =
        client.requestPermissions(ForcePermissionFlow.FORCE_LAUNCH).status == PermissionsResultStatus.SUCCESS

    override suspend fun scan(): List<SmartDevice> {
        if (!hasPermission()) return emptyList()
        return buildList {
            client.devices(enableMultipartDevices = true).list().forEach { device ->
                toSmartDevice(device)?.let { smartDevice ->
                    val saved = dao.getById(smartDevice.id)?.toDomain()
                    if (saved?.isConnected == true) {
                        dao.upsert(smartDevice.copy(isConnected = true).toEntity())
                    }
                    add(smartDevice)
                }
            }
        }
    }

    override suspend fun save(device: SmartDevice) {
        val previous = dao.getById(device.id)?.toDomain()
        if (previous != null && device.provider == SmartDevice.GOOGLE_HOME_PROVIDER) {
            val homeDevice = client.devices(enableMultipartDevices = true).list()
                .firstOrNull { it.id.id == device.id }
                ?: error("Google Home device is no longer available")

            if (previous.isOn != device.isOn) {
                val onOff = homeDevice.onOffTrait()
                    ?: error("This Google Home device does not support on/off control")
                if (device.isOn) onOff.on() else onOff.off()
            }
            if (previous.brightness != device.brightness) {
                val levelControl = homeDevice.levelControlTrait()
                    ?: error("This Google Home device does not support brightness control")
                levelControl.moveToLevelWithOnOff(
                    level = (device.brightness.coerceIn(0f, 1f) * 254).toInt().toUByte(),
                    transitionTime = null,
                    optionsMask = LevelControlTrait.OptionsBitmap(),
                    optionsOverride = LevelControlTrait.OptionsBitmap()
                )
            }
        }
        dao.upsert(device.toEntity())
    }

    private suspend fun toSmartDevice(device: HomeDevice): SmartDevice? {
        val type = when {
            device.has(ExtendedColorLightDevice) || device.has(DimmableLightDevice) ||
                device.has(OnOffLightDevice) -> DeviceType.LIGHT
            device.has(DimmablePlugInUnitDevice) || device.has(OnOffPluginUnitDevice) -> DeviceType.PLUG
            else -> return null
        }
        val onOff = device.onOffTrait()?.onOff ?: false
        val level = device.levelControlTrait()?.currentLevel?.toInt()
        return SmartDevice(
            id = device.id.id,
            name = device.name,
            type = type,
            isOn = onOff,
            brightness = level?.div(254f)?.coerceIn(0f, 1f) ?: 1f,
            provider = SmartDevice.GOOGLE_HOME_PROVIDER,
            endpointLabel = if (device.isMatterDevice) "Matter via Google Home" else "Works with Google Home"
        )
    }

    private suspend fun HomeDevice.onOffTrait(): OnOff? = when {
        has(ExtendedColorLightDevice) -> typeOrNull(ExtendedColorLightDevice).first()?.standardTraits?.onOff
        has(DimmableLightDevice) -> typeOrNull(DimmableLightDevice).first()?.standardTraits?.onOff
        has(OnOffLightDevice) -> typeOrNull(OnOffLightDevice).first()?.standardTraits?.onOff
        has(DimmablePlugInUnitDevice) -> typeOrNull(DimmablePlugInUnitDevice).first()?.standardTraits?.onOff
        has(OnOffPluginUnitDevice) -> typeOrNull(OnOffPluginUnitDevice).first()?.standardTraits?.onOff
        else -> null
    }

    private suspend fun HomeDevice.levelControlTrait(): LevelControl? = when {
        has(ExtendedColorLightDevice) -> typeOrNull(ExtendedColorLightDevice).first()?.standardTraits?.levelControl
        has(DimmableLightDevice) -> typeOrNull(DimmableLightDevice).first()?.standardTraits?.levelControl
        has(OnOffLightDevice) -> typeOrNull(OnOffLightDevice).first()?.standardTraits?.levelControl
        has(DimmablePlugInUnitDevice) -> typeOrNull(DimmablePlugInUnitDevice).first()?.standardTraits?.levelControl
        has(OnOffPluginUnitDevice) -> typeOrNull(OnOffPluginUnitDevice).first()?.standardTraits?.levelControl
        else -> null
    }
}
