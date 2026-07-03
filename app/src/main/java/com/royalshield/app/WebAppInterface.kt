package com.royalshield.app

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "Bridge" to communicate JavaScript in the WebView with native Android code.
 */
class WebAppInterface(
    private val context: Context,
    private val onTriggerSos: () -> Unit,
    private val onCheckUrl: (String) -> Unit
) {

    private val scope = CoroutineScope(Dispatchers.Main)

    /** Shows a Toast message from the web. */
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Activates the S.O.S. sequence.
     */
    @JavascriptInterface
    fun triggerSos() {
        showToast("Activando protocolo S.O.S...")
        scope.launch {
            onTriggerSos()
        }
    }

    /**
     * Analyzes a URL for threats.
     */
    @JavascriptInterface
    fun checkUrl(url: String) {
        showToast("Verificando enlace: $url")
        scope.launch {
            onCheckUrl(url)
        }
    }
}