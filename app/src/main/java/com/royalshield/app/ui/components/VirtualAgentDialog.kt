package com.royalshield.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AgentMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun VirtualAgentDialog(onDismiss: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<AgentMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Initial Greeting - Multilingual
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            delay(500)
            messages.add(AgentMessage(text = "🛡️ Royal Shield AI Online.\n\nHola. Hello. Olá. Bonjour.\n\nI speak all languages. How can I protect you today?", isUser = false))
        }
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
                // Header
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
                        // Use SupportAgent icon for a more "Human" feel
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
                            text = "Multi-Language Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Chat Area
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { msg ->
                        AgentChatBubble(message = msg)
                    }
                }

                // Input Area
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Ask anything / Pregunta lo que sea...", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = RoyalGold,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (messageText.isNotBlank()) {
                                    val text = messageText
                                    messageText = ""
                                    focusManager.clearFocus()
                                    onSendMessage(text, messages, scope)
                                }
                            })
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    val text = messageText
                                    messageText = ""
                                    focusManager.clearFocus()
                                    onSendMessage(text, messages, scope)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(RoyalGold, CircleShape)
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
fun AgentChatBubble(message: AgentMessage) {
    val isAgent = !message.isUser
    val alignment = if (isAgent) Alignment.Start else Alignment.End
    val containerColor = if (isAgent) Color(0xFF1E1E1E) else RoyalGold.copy(alpha = 0.2f)
    val borderColor = if (isAgent) CyberCyan.copy(alpha = 0.3f) else RoyalGold.copy(alpha = 0.5f)

    Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isAgent) 4.dp else 16.dp,
                bottomEnd = if (isAgent) 16.dp else 4.dp
            ),
            color = containerColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = Color.White,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

fun onSendMessage(text: String, messages: MutableList<AgentMessage>, scope: kotlinx.coroutines.CoroutineScope) {
    messages.add(AgentMessage(text = text, isUser = true))
    
    scope.launch {
        delay(1000)
        val response = getAgentResponse(text)
        messages.add(AgentMessage(text = response, isUser = false))
    }
}

fun getAgentResponse(query: String): String {
    val q = query.lowercase().trim()
    
    // LANGUAGE DETECTION LOGIC
    val isSpanish = q.any { it in "áéíóúñ" } || q.contains("hola") || q.contains("gracias") || q.contains("como") || q.contains("seguridad")
    val isPortuguese = q.contains("olá") || q.contains("obrigado") || q.contains("segurança") || q.contains("como estai")
    val isFrench = q.contains("bonjour") || q.contains("merci") || q.contains("securite") || q.contains("comment")
    
    return when {
         // SPANISH RESPONSES
         isSpanish && (q.contains("hola") || q.contains("buenos")) -> "Hola, Oficial. Sistemas listos."
         isSpanish && (q.contains("scan") || q.contains("analisis") || q.contains("virus")) -> "Iniciando escaneo profundo... Sin amenazas detectadas en este momento."
         isSpanish && (q.contains("sos") || q.contains("ayuda")) -> "Protocolo SOS en espera. Presione el botón ROJO en el panel para activar la baliza de emergencia."
         isSpanish && (q.contains("quien eres") || q.contains("nombre")) -> "Soy Royal AI, su asistente de ciberseguridad personal."
         isSpanish -> "Entendido. Procesando su solicitud en español: '$query'. Sistemas seguros."

         // PORTUGUESE RESPONSES
         isPortuguese && (q.contains("olá") || q.contains("oi")) -> "Olá! Sistemas de segurança ativos e prontos."
         isPortuguese && (q.contains("sos") || q.contains("ajuda")) -> "SOS em espera. Pressione o botão VERMELHO para emergência."
         isPortuguese -> "Entendido. Processando em Português: '$query'. Tudo seguro."

         // FRENCH RESPONSES
         isFrench && (q.contains("bonjour") || q.contains("salut")) -> "Bonjour. Systèmes de sécurité actifs."
         isFrench -> "Bien reçu. Traitement en Français: '$query'."

         // ENGLISH (DEFAULT)
         q.contains("hi") || q.contains("hello") -> "Greetings. Security systems nominal."
         q.contains("scan") || q.contains("check") -> "Running diagnostics... System integrity 100%."
         q.contains("sos") || q.contains("help") -> "SOS Protocol standby. Press the RED button on the dashboard for immediate extraction."
         q.contains("who are you") -> "I am Royal AI, your personal cybersecurity defense agent."
         else -> "Command received: '$query'. \n\nI speak EN, ES, PT, FR. How can I assist?"
    }
}
