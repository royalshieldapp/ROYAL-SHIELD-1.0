package com.royalshield.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.SafeGreen
import com.royalshield.app.ui.theme.NeonRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.royalshield.app.managers.AiManager
import com.royalshield.app.ui.components.CyberConsole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScriptLabScreen(onBack: () -> Unit) {
    val aiManager = remember { AiManager() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var generatedScript by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(true) }

    // Scroll state for the main screen
    val scrollState = rememberScrollState()

    com.royalshield.app.ui.components.RoyalGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = RoyalGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("AI Script Lab", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_back_gold),
                                contentDescription = "Back",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = RoyalGold
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ETHICS NOTICE
            if (showDisclaimer) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = NeonRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ethics & Safety Notice", color = NeonRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "The scripts generated here are strictly for educational purposes in controlled environments. Misuse is illegal and unethical.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // INPUT SECTION
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Describe Your Script", color = RoyalGold, fontWeight = FontWeight.SemiBold)
                
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { 
                        Text(
                            "E.g., A Python script that scans for open ports on a local IP...", 
                            color = Color.Gray.copy(alpha = 0.5f)
                        ) 
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalGold,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = RoyalGold,
                        focusedContainerColor = Color(0xFF0A0A0A),
                        unfocusedContainerColor = Color(0xFF0A0A0A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    )
                )
                
                // Help Me Think Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    val suggestions = listOf(
                        "Scan Ports (Python)", 
                        "Log Parser (Bash)", 
                        "File Hashing (Py)", 
                        "System Info (PS1)"
                    )
                    suggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { prompt = "Create a ${suggestion.replace("(", "").replace(")", "")} script that..." },
                            label = { Text(suggestion, color = Color.White) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF1E1E1E),
                                labelColor = Color.White
                            ),
                            border = null
                        )
                    }
                }
            }

            // GENERATE BUTTON
            Button(
                onClick = {
                    if (prompt.length < 10) {
                        Toast.makeText(context, "Please describe your script in more detail.", Toast.LENGTH_SHORT).show()
                    } else {
                        isGenerating = true
                        generatedScript = ""
                        aiManager.analyzeThreat("AI Script Lab: $prompt") { result ->
                            isGenerating = false
                            generatedScript = result
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold),
                shape = RoundedCornerShape(12.dp),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Generating...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Generate Script", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            // OUTPUT SECTION
            AnimatedVisibility(visible = generatedScript.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Generated Script", color = SafeGreen, fontWeight = FontWeight.Bold)
                        
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("AI Script", generatedScript)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Script copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                        border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = generatedScript,
                                color = Color(0xFFBBBBBB),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("NEURAL_LINK TRAFFIC", color = RoyalGold.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            CyberConsole(promptFlow = aiManager.prompts)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }
}

// Mock AI Logic
private fun generateMockScript(prompt: String): String {
    val p = prompt.lowercase()
    return when {
        p.contains("python") -> """
import os

print("Simulated Python Script for: " + "$prompt")
# Todo: Implement actual logic
        """.trimIndent()

        p.contains("bash") -> """
#!/bin/bash
echo "Simulated Bash Script for: $prompt"
# Todo: Implement actual logic
        """.trimIndent()
        
        else -> """
# Script generated for: "$prompt"
# Language: Python (Default)

def main():
    print("AI Script Generation Successful")

if __name__ == "__main__":
    main()
        """.trimIndent()
    }
}
