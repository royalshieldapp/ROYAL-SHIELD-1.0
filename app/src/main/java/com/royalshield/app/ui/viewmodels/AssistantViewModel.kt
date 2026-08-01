package com.royalshield.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.data.AssistantHistoryEntry
import com.royalshield.app.data.AssistantRepository
import com.royalshield.app.data.AssistantRepositoryResult
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AssistantMessageRole {
    USER,
    ASSISTANT,
    ERROR
}

data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val role: AssistantMessageRole,
    val timestamp: Long = System.currentTimeMillis()
)

data class AssistantUiState(
    val messages: List<AssistantMessage> = emptyList(),
    val isSending: Boolean = false
)

class AssistantViewModel(
    repository: AssistantRepository? = null,
    sender: (suspend (
        message: String,
        history: List<AssistantHistoryEntry>
    ) -> AssistantRepositoryResult)? = null
) : ViewModel() {
    private val sendToRepository = sender
        ?: (repository ?: AssistantRepository())::sendMessage
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState = _uiState.asStateFlow()

    fun sendMessage(message: String) {
        val normalized = message.trim()
        if (normalized.isEmpty() || _uiState.value.isSending) return

        val previousMessages = _uiState.value.messages
        val history = previousMessages
            .filter { it.role != AssistantMessageRole.ERROR }
            .takeLast(10)
            .map { item ->
                AssistantHistoryEntry(
                    role = if (item.role == AssistantMessageRole.USER) "user" else "assistant",
                    content = item.text
                )
            }

        _uiState.value = _uiState.value.copy(
            messages = previousMessages + AssistantMessage(
                text = normalized,
                role = AssistantMessageRole.USER
            ),
            isSending = true
        )

        viewModelScope.launch {
            val result = sendToRepository(normalized, history)
            val response = when (result) {
                is AssistantRepositoryResult.Success -> AssistantMessage(
                    text = result.reply,
                    role = AssistantMessageRole.ASSISTANT
                )
                is AssistantRepositoryResult.Error -> AssistantMessage(
                    text = result.message,
                    role = AssistantMessageRole.ERROR
                )
            }
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + response,
                isSending = false
            )
        }
    }
}
