package com.royalshield.app.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.royalshield.app.managers.AiManager
import com.royalshield.app.managers.DidManager
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DidAgentDialog(
    onDismiss: () -> Unit,
    onVoiceCommand: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val aiManager = remember { AiManager() }
    
    var isListening by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var agentStatus by remember { mutableStateOf("Tap mic to speak") }
    var videoUrl by remember { mutableStateOf<String?>(null) }

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }

    // Speech Recognizer
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    DisposableEffect(context) {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        onDispose {
            recognizer.destroy()
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            agentStatus = "Permission granted. Listening..."
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } else {
            agentStatus = "Microphone permission required."
        }
    }

    // Polling logic for video
    fun pollForVideo(talkId: String) {
        scope.launch {
            var attempts = 0
            while (attempts < 20) {
                delay(2000)
                val status = DidManager.getTalkStatus(talkId)
                if (status?.status == "done" && status.resultUrl != null) {
                    videoUrl = status.resultUrl
                    break
                } else if (status?.status == "error") {
                    break
                }
                attempts++
            }
        }
    }

    fun processCommand(spokenText: String) {
        agentStatus = "Command: \"$spokenText\""
        isProcessing = true
        
        // Let the Dashboard handle navigation if it matches a keyword
        onVoiceCommand(spokenText)
        
        // Also get an AI response
        scope.launch {
            agentStatus = "Thinking..."
            aiManager.analyzeThreat("Voice Command: $spokenText") { response ->
                isProcessing = false
                val safeResponse = response ?: "I couldn't process that command."
                agentStatus = safeResponse
                tts?.speak(safeResponse, TextToSpeech.QUEUE_FLUSH, null, null)
                
                // Try to generate video in background
                scope.launch {
                    val agentId = PreferencesManager.getDidAgentId() ?: "v2_agt_6s8X40sT"
                    val talkId = DidManager.createAgentTalk(agentId, safeResponse)
                    if (talkId != null) {
                        pollForVideo(talkId)
                    }
                }
            }
        }
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                agentStatus = "Listening..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                agentStatus = "Processing..."
            }
            override fun onError(error: Int) {
                isListening = false
                agentStatus = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error. Check connection."
                    SpeechRecognizer.ERROR_CLIENT -> "Client error. Try restarting."
                    else -> "Error ($error). Tap to try again."
                }
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrEmpty()) {
                    processCommand(text)
                } else {
                    agentStatus = "No command detected"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
        ) {
            if (videoUrl != null) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setOnCompletionListener { 
                                videoUrl = null 
                                agentStatus = "Tap mic to speak"
                            }
                            setOnPreparedListener { mp ->
                                mp.start()
                            }
                        }
                    },
                    update = { view ->
                        view.setVideoURI(Uri.parse(videoUrl))
                        view.start()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                   AnimatedWaveformRing(isActive = isListening || isProcessing)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROYAL AGENT",
                        color = RoyalGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = agentStatus,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    )

                    Text(
                        text = "SUGGESTED COMMANDS (Say or Tap):",
                        color = RoyalGold.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    val scrollState = rememberScrollState()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        val commands = listOf(
                            Pair("Scan", Icons.Default.Search),
                            Pair("VPN", Icons.Default.Lock),
                            Pair("SOS", Icons.Default.Warning),
                            Pair("Map", Icons.Default.Map),
                            Pair("Settings", Icons.Default.Settings)
                        )
                        commands.forEach { (label, icon) ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.08f),
                                                Color.White.copy(alpha = 0.03f)
                                            )
                                        )
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.25f),
                                                    RoyalGold.copy(alpha = 0.15f),
                                                    Color.White.copy(alpha = 0.05f)
                                                )
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        processCommand(label)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = RoyalGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    val buttonColor = if (isListening) Color.Red else if (isProcessing) Color.Gray else RoyalGold
                    
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(buttonColor)
                            .clickable(enabled = !isProcessing) {
                                if (isListening) {
                                    speechRecognizer?.stopListening()
                                } else {
                                    startListening()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
