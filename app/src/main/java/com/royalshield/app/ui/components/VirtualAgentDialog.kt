package com.royalshield.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.viewmodels.AssistantMessage
import com.royalshield.app.ui.viewmodels.AssistantMessageRole
import com.royalshield.app.ui.viewmodels.AssistantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VirtualAgentDialog(
    onDismiss: () -> Unit,
    viewModel: AssistantViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.messages.size, uiState.isSending) {
        val lastIndex = uiState.messages.lastIndex
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    fun submit() {
        if (messageText.isBlank() || uiState.isSending) return
        val text = messageText
        messageText = ""
        focusManager.clearFocus()
        viewModel.sendMessage(text)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F0F13).copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF7B42F6).copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SupportAgent, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ROYAL AGENT",
                            style = MaterialTheme.typography.titleMedium,
                            color = RoyalGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isSending) "Consultando servicio..." else "Chat autenticado",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isSending) RoyalGold else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        item {
                            Text(
                                text = "Escribe una consulta. Las respuestas provienen del servicio autenticado de Royal Shield.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    items(uiState.messages, key = { it.id }) { message ->
                        AgentChatBubble(message)
                    }
                    if (uiState.isSending) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = RoyalGold,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Esperando respuesta real...", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Surface(color = Color.Black.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            enabled = !uiState.isSending,
                            placeholder = { Text("Escribe tu consulta...", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E),
                                disabledContainerColor = Color(0xFF151515),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.Gray,
                                cursorColor = RoyalGold,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submit() })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { submit() },
                            enabled = messageText.isNotBlank() && !uiState.isSending,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (messageText.isNotBlank() && !uiState.isSending) RoyalGold else Color.DarkGray,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentChatBubble(message: AssistantMessage) {
    val isUser = message.role == AssistantMessageRole.USER
    val isError = message.role == AssistantMessageRole.ERROR
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = when {
        isError -> Color(0xFF3A1515)
        isUser -> RoyalGold.copy(alpha = 0.2f)
        else -> Color(0xFF1E1E1E)
    }
    val borderColor = when {
        isError -> Color(0xFFFF6B6B)
        isUser -> RoyalGold.copy(alpha = 0.5f)
        else -> CyberCyan.copy(alpha = 0.3f)
    }

    Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isError) Color(0xFFFFB4B4) else Color.White,
                fontSize = 14.sp
            )
        }
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
