package com.royalshield.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.CyberCyan
import kotlinx.coroutines.flow.SharedFlow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CyberConsole(
    promptFlow: SharedFlow<String>,
    modifier: Modifier = Modifier
) {
    val logs = remember { mutableStateListOf<ConsoleLog>() }
    val listState = rememberLazyListState()

    LaunchedEffect(promptFlow) {
        promptFlow.collect { message ->
            logs.add(ConsoleLog(message, System.currentTimeMillis()))
            if (logs.size > 50) logs.removeAt(0)
            // Auto-scroll to bottom
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "NEURAL_LINK_CONSOLE v4.2",
                color = RoyalGold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (logs.isEmpty()) RoyalGold.copy(alpha = 0.12f) else CyberCyan.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (logs.isEmpty()) RoyalGold.copy(alpha = 0.35f) else CyberCyan.copy(alpha = 0.35f)
                )
            ) {
                Text(
                    text = if (logs.isEmpty()) "STANDBY" else "LIVE",
                    color = if (logs.isEmpty()) RoyalGold else CyberCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Console idle",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Run an AI module to stream prompts, responses, and telemetry.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: ConsoleLog) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val timestamp = sdf.format(Date(log.timestamp))
    
    val color = when {
        log.message.startsWith(">>>") -> RoyalGold
        log.message.startsWith("<<<") -> CyberCyan
        log.message.startsWith("!!!") -> Color.Red
        else -> Color.Gray
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            "[$timestamp]",
            color = Color.DarkGray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            log.message,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}

data class ConsoleLog(val message: String, val timestamp: Long)
