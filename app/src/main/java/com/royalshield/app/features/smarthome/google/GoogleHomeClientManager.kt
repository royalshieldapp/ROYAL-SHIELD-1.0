package com.royalshield.app.features.smarthome.google

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.home.FactoryRegistry
import com.google.home.Home
import com.google.home.HomeClient
import com.google.home.HomeConfig
import com.google.home.matter.standard.DimmableLightDevice
import com.google.home.matter.standard.DimmablePlugInUnitDevice
import com.google.home.matter.standard.ExtendedColorLightDevice
import com.google.home.matter.standard.LevelControl
import com.google.home.matter.standard.OnOff
import com.google.home.matter.standard.OnOffLightDevice
import com.google.home.matter.standard.OnOffPluginUnitDevice
import kotlinx.coroutines.Dispatchers

object GoogleHomeClientManager {
    @Volatile
    private var clientInstance: HomeClient? = null

    fun getClient(context: Context): HomeClient = clientInstance ?: synchronized(this) {
        clientInstance ?: Home.getClient(
            context.applicationContext,
            HomeConfig(
                coroutineContext = Dispatchers.IO,
                factoryRegistry = FactoryRegistry(
                    traits = listOf(OnOff, LevelControl),
                    types = listOf(
                        OnOffLightDevice,
                        DimmableLightDevice,
                        ExtendedColorLightDevice,
                        OnOffPluginUnitDevice,
                        DimmablePlugInUnitDevice
                    )
                )
            )
        ).also { clientInstance = it }
    }

    fun registerPermissionCaller(activity: ComponentActivity) {
        getClient(activity).registerActivityResultCallerForPermissions(activity)
    }
}
