package com.royalshield.app.security

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object AppCheckProviderInstaller {
    fun install() {
        check(
            appCheckProviderMode(isDebugBuild = false) ==
                AppCheckProviderMode.PLAY_INTEGRITY,
        )
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
    }
}
