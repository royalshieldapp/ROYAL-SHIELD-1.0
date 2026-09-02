package com.royalshield.app.ui.screens

enum class MapLocationStatus {
    LOCATION_OFF,
    PERMISSION_REQUIRED,
    WAITING_FOR_FIX,
    AVAILABLE,
}

fun resolveMapLocationStatus(
    locationEnabled: Boolean,
    hasPermission: Boolean,
    hasFix: Boolean,
): MapLocationStatus = when {
    !locationEnabled -> MapLocationStatus.LOCATION_OFF
    !hasPermission -> MapLocationStatus.PERMISSION_REQUIRED
    !hasFix -> MapLocationStatus.WAITING_FOR_FIX
    else -> MapLocationStatus.AVAILABLE
}
