package com.royalshield.app.ui.screens

enum class RegistrationProvider(val isAvailable: Boolean) {
    PHONE(true),
    GOOGLE(true),
    APPLE(false),
    GITHUB(false),
    FACEBOOK(false),
    EMAIL(false),
}

fun canEnterProtectedApp(hasAuthenticatedUser: Boolean): Boolean = hasAuthenticatedUser
