package com.royalshield.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.royalshield.app.data.db.TriggerType

class AutomationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AutomationReceiver", "Received action: $action")

        val triggerType = when (action) {
            Intent.ACTION_POWER_CONNECTED -> TriggerType.POWER_CONNECTED
            Intent.ACTION_POWER_DISCONNECTED -> TriggerType.POWER_DISCONNECTED
            else -> null
        }

        if (triggerType != null) {
            Log.i("AutomationReceiver", "Trigger detected: $triggerType")
            AutomationEngine.processEvent(context.applicationContext, triggerType)
        }
    }
}
