package com.royalshield.app.managers

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

data class PrivacyApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val permissions: List<String>,
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    HIGH, MEDIUM, LOW
}

object PrivacyAdvisorManager {

    private val DANGEROUS_PERMISSIONS = listOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS
    )

    suspend fun scanApps(
        context: Context,
        onProgress: suspend (progress: Float, currentApp: String) -> Unit
    ): List<PrivacyApp> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val riskyApps = mutableListOf<PrivacyApp>()
        val total = installedApps.size

        installedApps.forEachIndexed { index, packageInfo ->
            val appName = packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName ?: "Unknown"
            onProgress(((index + 1) / total.toFloat()) * 100f, appName)

            val permissions = packageInfo.requestedPermissions
            if (permissions != null) {
                val detectedRisks = mutableListOf<String>()
                for (perm in permissions) {
                    if (DANGEROUS_PERMISSIONS.contains(perm)) {
                        // Check if permission is actually granted
                        if (pm.checkPermission(perm, packageInfo.packageName) == PackageManager.PERMISSION_GRANTED) {
                            detectedRisks.add(perm.substringAfterLast("."))
                        }
                    }
                }

                if (detectedRisks.isNotEmpty()) {
                    val icon = packageInfo.applicationInfo?.loadIcon(pm)
                    
                    val riskLevel = when {
                        detectedRisks.contains("CAMERA") || detectedRisks.contains("RECORD_AUDIO") -> RiskLevel.HIGH
                        detectedRisks.contains("READ_SMS") -> RiskLevel.HIGH
                        detectedRisks.size > 3 -> RiskLevel.MEDIUM
                        else -> RiskLevel.LOW
                    }

                    riskyApps.add(
                        PrivacyApp(
                            packageName = packageInfo.packageName,
                            appName = appName,
                            icon = icon,
                            permissions = detectedRisks,
                            riskLevel = riskLevel
                        )
                    )
                }
            }
            // Realistic scan delay to provide a high-end, thorough security feel
            kotlinx.coroutines.delay(65)
        }
        
        // Sort by risk (High first)
        return riskyApps.sortedBy { it.riskLevel }
    }

    // Legacy sync call fallback
    fun scanApps(context: Context): List<PrivacyApp> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val riskyApps = mutableListOf<PrivacyApp>()

        for (packageInfo in installedApps) {
            val permissions = packageInfo.requestedPermissions
            if (permissions != null) {
                val detectedRisks = mutableListOf<String>()
                for (perm in permissions) {
                    if (DANGEROUS_PERMISSIONS.contains(perm)) {
                        if (pm.checkPermission(perm, packageInfo.packageName) == PackageManager.PERMISSION_GRANTED) {
                            detectedRisks.add(perm.substringAfterLast("."))
                        }
                    }
                }

                if (detectedRisks.isNotEmpty()) {
                    val appName = packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName ?: "Unknown"
                    val icon = packageInfo.applicationInfo?.loadIcon(pm)
                    
                    val riskLevel = when {
                        detectedRisks.contains("CAMERA") || detectedRisks.contains("RECORD_AUDIO") -> RiskLevel.HIGH
                        detectedRisks.contains("READ_SMS") -> RiskLevel.HIGH
                        detectedRisks.size > 3 -> RiskLevel.MEDIUM
                        else -> RiskLevel.LOW
                    }

                    riskyApps.add(
                        PrivacyApp(
                            packageName = packageInfo.packageName,
                            appName = appName,
                            icon = icon,
                            permissions = detectedRisks,
                            riskLevel = riskLevel
                        )
                    )
                }
            }
        }
        return riskyApps.sortedBy { it.riskLevel }
    }
}

