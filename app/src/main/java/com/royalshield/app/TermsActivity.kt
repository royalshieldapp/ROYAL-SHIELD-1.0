package com.royalshield.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.Royal_shieldTheme

class TermsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Terms & Conditions", color = RoyalGold) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = RoyalGold
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = RoyalGold
                            )
                        )
                    }
                ) { paddingValues ->
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                setBackgroundColor(0xFF000000.toInt()) // Set to black to match the theme
                                loadUrl("file:///android_asset/terms_embed.html")
                            }
                        }
                    )
                }
            }
        }
    }
}
