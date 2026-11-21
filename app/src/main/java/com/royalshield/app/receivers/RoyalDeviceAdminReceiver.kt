package com.royalshield.app.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.royalshield.app.util.NotificationUtils

class RoyalDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, user)
        // Extract manager
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val componentName = android.content.ComponentName(context, RoyalDeviceAdminReceiver::class.java)

        // Only works if we actually have active admin powers
        if (dpm.isAdminActive(componentName)) {
            val failedAttempts = dpm.getCurrentFailedPasswordAttempts()
            if (failedAttempts >= 3) {
                // Trigger formal BRUTE FORCE alert
                NotificationUtils.showSecurityAlert(
                    context,
                    title = "ALERT: BRUTE FORCE ATTEMPT",
                    message = "Multiple failed attempts to unlock the device ($failedAttempts).",
                    isCritical = true
                )
            }
        }
    }
}
