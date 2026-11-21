package com.royalshield.app.data

sealed interface AvailabilityState {
    val reason: String?
    val dataFreshness: String?

    data class Ready(
        override val dataFreshness: String,
        val sources: List<String>,
        val modelVersion: String?,
        val confidence: Double?,
    ) : AvailabilityState {
        override val reason: String? = null
    }

    data class Degraded(
        override val reason: String,
        override val dataFreshness: String?,
        val sources: List<String>,
    ) : AvailabilityState

    data class Unavailable(
        override val reason: String,
    ) : AvailabilityState {
        override val dataFreshness: String? = null
    }
}

data class AdvancedFeatureAvailability(
    val vpn: AvailabilityState,
    val forecast: AvailabilityState,
    val threatMap: AvailabilityState,
    val automations: AvailabilityState,
    val reports: AvailabilityState,
    val policies: AvailabilityState,
) {
    fun all(): List<AvailabilityState> =
        listOf(vpn, forecast, threatMap, automations, reports, policies)

    companion object {
        fun unavailable(reason: String): AdvancedFeatureAvailability {
            fun state() = AvailabilityState.Unavailable(reason)
            return AdvancedFeatureAvailability(
                vpn = state(),
                forecast = state(),
                threatMap = state(),
                automations = state(),
                reports = state(),
                policies = state(),
            )
        }
    }
}
