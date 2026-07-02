package com.royalshield.app

import android.app.Application
import android.util.Log
import com.royalshield.app.managers.AnalyticsManager
import com.royalshield.app.managers.PreferencesManager
import com.google.android.gms.ads.MobileAds
import com.royalshield.app.util.NotificationUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import java.security.Security
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoyalShieldApp : Application() {

    companion object {
        lateinit var instance: RoyalShieldApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Bouncy Castle Providers for PQC
        try {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
            // Only add PQC if available in classpath
            Class.forName("org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider")?.let {
                Security.addProvider(BouncyCastlePQCProvider())
            }
        } catch (e: Exception) {
            Log.e("RoyalShieldApp", "BouncyCastle init failed: ${e.message}")
        }
        
        // Initialize Analytics
        AnalyticsManager.initialize(this)
        
        // Initialize Preferences
        PreferencesManager.init(this)
        
        // Initialize Notification Channels (Phase 2)
        NotificationUtils.initChannels(this)
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("RoyalShieldApp", "FATAL CRASH:", throwable)
            try {
                val path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(path, "crash_log.txt")
                val writer = java.io.PrintWriter(file)
                throwable.printStackTrace(writer)
                writer.flush()
                writer.close()
            } catch (e: Exception) {
                Log.e("RoyalShieldApp", "Failed to write crash log", e)
            }
            defaultHandler?.uncaughtException(thread, throwable) ?: System.exit(1)
        }

        // Initialize AdMob in background to avoid blocking main thread and causing ANR
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                MobileAds.initialize(this@RoyalShieldApp) {}
            } catch (e: Exception) {
                Log.e("RoyalShieldApp", "AdMob init failed: ${e.message}")
            }
        }

        // Set basic user properties/context if needed
        AnalyticsManager.logEvent("app_initialized")
    }
}
