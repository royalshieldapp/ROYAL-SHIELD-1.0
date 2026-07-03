package com.royalshield.app.managers

import android.content.Context
import android.util.Log
import com.royalshield.app.SosManager
import com.royalshield.app.data.db.ActionType
import com.royalshield.app.data.db.AutomationRule
import com.royalshield.app.data.db.TriggerType
import com.royalshield.app.util.NotificationUtils

object AutomationEvaluator {

    fun evaluate(context: Context, rule: AutomationRule, currentData: Map<String, Any>) {
        if (!rule.isEnabled) return

        val shouldTrigger = when (rule.triggerType) {
            TriggerType.BATTERY_LOW -> {
                val battery = currentData["battery"] as? Int ?: 100
                val threshold = rule.triggerParams.toIntOrNull() ?: 15
                battery <= threshold
            }
            TriggerType.ROUTE_DEVIATION -> {
                // Simplified: currentData["deviation"] provided by LocationTracker
                val deviation = currentData["deviation"] as? Float ?: 0f
                val threshold = rule.triggerParams.filter { it.isDigit() }.toIntOrNull() ?: 50
                deviation > threshold
            }
            else -> false
        }

        if (shouldTrigger) {
            executeAction(context, rule)
        }
    }

    private fun executeAction(context: Context, rule: AutomationRule) {
        Log.d("AutomationEvaluator", "Triggering automation: ${rule.name} -> ${rule.actionType}")
        
        when (rule.actionType) {
            ActionType.SILENT_SOS -> {
                SosManager.triggerSilentSos(context)
            }
            ActionType.ALERT_PROMPT -> {
                NotificationUtils.showSecurityAlert(
                    context,
                    "Royal Automation: ${rule.name}",
                    "Triggered: ${rule.actionParams}",
                    isCritical = true
                )
            }
            else -> {
                Log.w("AutomationEvaluator", "Action ${rule.actionType} not yet fully implemented in evaluator")
            }
        }
    }
}
