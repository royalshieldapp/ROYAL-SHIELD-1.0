package com.royalshield.app.managers

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Validated Singleton for Analytics and Crashlytics.
 * Handles tracking of user events and non-fatal exceptions.
 */
object AnalyticsManager {

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context)
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                isInitialized = true
                logEvent("app_open", null)
            } catch (e: Exception) {
                // Determine if we should log to local console in debug
                e.printStackTrace()
            }
        }
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics?.logEvent(eventName, params)
        } catch (e: Exception) {
            // Failsafe
        }
    }

    fun logScreenView(screenName: String, screenClass: String = screenName) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun recordException(t: Throwable) {
        try {
            FirebaseCrashlytics.getInstance().recordException(t)
        } catch (e: Exception) {
            // Failsafe
        }
    }

    fun setUserId(userId: String) {
        try {
            firebaseAnalytics?.setUserId(userId)
            FirebaseCrashlytics.getInstance().setUserId(userId)
        } catch (e: Exception) {
            // Failsafe
        }
    }
}
