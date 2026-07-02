package com.royalshield.app.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Video Background Component
 * Plays a looping video as background
 */
@Composable
fun VideoBackground(
    videoResId: Int? = null,
    videoUrl: String? = null,
    shouldLoop: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (videoResId == null && videoUrl == null) return

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                // Set video source
                val uri = if (videoUrl != null) {
                    Uri.parse(videoUrl)
                } else {
                    Uri.parse("android.resource://${ctx.packageName}/$videoResId")
                }
                
                setVideoURI(uri)
                
                // Loop behavior
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = shouldLoop
                    mediaPlayer.setVolume(0f, 0f) // Mute by default, consider unmuting for agent
                    if (videoUrl != null) {
                         // For agent, maybe volume up? But let's keep it consistent for now.
                         // Or add volume param.
                    }
                }
                
                start()
            }
        },
        modifier = modifier
    )
}
