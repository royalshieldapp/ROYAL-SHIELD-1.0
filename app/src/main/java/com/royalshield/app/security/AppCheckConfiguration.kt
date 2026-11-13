package com.royalshield.app.security

enum class AppCheckProviderMode {
    DEBUG,
    PLAY_INTEGRITY,
}

fun appCheckProviderMode(isDebugBuild: Boolean): AppCheckProviderMode =
    if (isDebugBuild) AppCheckProviderMode.DEBUG else AppCheckProviderMode.PLAY_INTEGRITY
