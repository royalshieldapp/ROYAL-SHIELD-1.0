package com.royalshield.app.ui.screens

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.royalshield.app.WebAppInterface

@Composable
fun AiNeuralHubScreen(
    onBack: () -> Unit = {},
    onTriggerSos: () -> Unit = {},
    onCheckUrl: (String) -> Unit = {}
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.domStorageEnabled = true
                addJavascriptInterface(
                    WebAppInterface(
                        context = ctx,
                        onTriggerSos = onTriggerSos,
                        onCheckUrl = onCheckUrl
                    ),
                    "Android"
                )
                loadUrl("file:///android_asset/index.html")
            }
        },
        update = { /* Do nothing to prevent reloading loops */ }
    )
}
