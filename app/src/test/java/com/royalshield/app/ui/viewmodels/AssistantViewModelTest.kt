package com.royalshield.app.ui.viewmodels

import com.royalshield.app.data.AssistantHistoryEntry
import com.royalshield.app.data.AssistantRepositoryResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ignores blank input and blocks duplicate sends while request is active`() = runTest(dispatcher) {
        val response = CompletableDeferred<AssistantRepositoryResult>()
        val sentMessages = mutableListOf<String>()
        val viewModel = AssistantViewModel(
            sender = { message, _ ->
                sentMessages += message
                response.await()
            }
        )

        viewModel.sendMessage("   ")
        viewModel.sendMessage("  primer mensaje  ")
        viewModel.sendMessage("segundo mensaje")
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf("primer mensaje"), sentMessages)
        assertTrue(viewModel.uiState.value.isSending)
        assertEquals(listOf("primer mensaje"), viewModel.uiState.value.messages.map { it.text })

        response.complete(AssistantRepositoryResult.Success("respuesta real"))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSending)
        assertEquals(
            listOf(AssistantMessageRole.USER, AssistantMessageRole.ASSISTANT),
            viewModel.uiState.value.messages.map { it.role }
        )
        assertEquals("respuesta real", viewModel.uiState.value.messages.last().text)
    }

    @Test
    fun `adds real repository errors to chat state`() = runTest(dispatcher) {
        val receivedHistory = mutableListOf<List<AssistantHistoryEntry>>()
        val viewModel = AssistantViewModel(
            sender = { _, history ->
                receivedHistory += history
                AssistantRepositoryResult.Error("servicio no configurado")
            }
        )

        viewModel.sendMessage("hola")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, receivedHistory.size)
        assertEquals(AssistantMessageRole.ERROR, viewModel.uiState.value.messages.last().role)
        assertEquals("servicio no configurado", viewModel.uiState.value.messages.last().text)
        assertFalse(viewModel.uiState.value.isSending)
    }
}
