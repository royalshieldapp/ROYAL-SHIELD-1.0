package com.royalshield.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.royalshield.app.ui.theme.Royal_shieldTheme

class DiscreteModeActivity : ComponentActivity() {

    private var volumeUpCount = 0
    private var lastVolumePressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide System Bars for full "Off Screen" look
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            Royal_shieldTheme {
                DiscreteScreen()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastVolumePressTime < 1000) {
                volumeUpCount++
            } else {
                volumeUpCount = 1
            }
            lastVolumePressTime = currentTime

            if (volumeUpCount >= 3) {
                // TRIGGER SOS
                SosManager.triggerSilentSos(this)
                volumeUpCount = 0
                return true
            }
            return true // Consume event to prevent system volume change logging clearly
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun DiscreteScreen() {
    val context = LocalContext.current
    var tapCount by remember { mutableIntStateOf(0) }
    
    // Triple Tap Detector Logic
    LaunchedEffect(tapCount) {
         if (tapCount >= 3) {
             SosManager.triggerSilentSos(context)
             tapCount = 0
         } else if (tapCount > 0) {
             // Reset after 1 second if not reached 3
             kotlinx.coroutines.delay(1000)
             if (tapCount < 3) tapCount = 0 
         }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Looks like screen is off
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        tapCount++ 
                    },
                    onLongPress = {
                        // Long Press to Exit or Trigger? User asked for "Gesture"
                        // Let's implement Long Press as Exit for safety, or trigger? 
                        // User asked for "Triple Tap" and "Hold Volume". 
                        // Let's keep Long Press on Screen as Exit mechanism so user isn't stuck.
                        Toast.makeText(context, "Exiting Discrete Mode", Toast.LENGTH_SHORT).show()
                        (context as? android.app.Activity)?.finish()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Minimal Clock to make it look like "Always On Display" (Fake Stealth)
        // Or completely black. User said "Discrete Mode".
        // A fake clock is less suspicious than a dead black screen that touches back.
        Text(
            text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
            color = Color.DarkGray.copy(alpha = 0.3f), // Very dim
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
    }
}

