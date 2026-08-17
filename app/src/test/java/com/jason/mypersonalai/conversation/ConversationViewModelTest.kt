package com.jason.mypersonalai.conversation

import com.jason.mypersonalai.agent.AgentEngine
import com.jason.mypersonalai.agent.AgentError
import com.jason.mypersonalai.agent.AgentResult
import com.jason.mypersonalai.agent.Message
import com.jason.mypersonalai.agent.Role
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
class ConversationViewModelTest {

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
    fun `sending a message appends the user message immediately and sets busy`() = runTest {
        val viewModel = ConversationViewModel(agentEngine = NeverRespondingEngine())

        viewModel.sendMessage("hi there")

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals(Role.USER, state.messages.first().role)
        assertTrue(state.isBusy)
    }

    @Test
    fun `successful agent response is appended and busy clears`() = runTest {
        val viewModel = ConversationViewModel(agentEngine = EchoAgentEngine())

        viewModel.sendMessage("hello")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(Role.ASSISTANT, state.messages[1].role)
        assertFalse(state.isBusy)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `failed agent response surfaces an error and clears busy without adding a message`() =
        runTest {
            val viewModel = ConversationViewModel(agentEngine = FailingAgentEngine())

            viewModel.sendMessage("hello")
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.messages.size) // only the user message
            assertFalse(state.isBusy)
            assertEquals("mock failure", state.errorMessage)
        }

    @Test
    fun `blank input is ignored`() = runTest {
        val viewModel = ConversationViewModel(agentEngine = EchoAgentEngine())

        viewModel.sendMessage("   ")

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    private class EchoAgentEngine : AgentEngine {
        override suspend fun generateResponse(history: List<Message>): AgentResult {
            return AgentResult.Success(
                Message(id = "reply", role = Role.ASSISTANT, text = "echo", timestampMillis = 0L)
            )
        }
    }

    private class FailingAgentEngine : AgentEngine {
        override suspend fun generateResponse(history: List<Message>): AgentResult {
            return AgentResult.Failure(AgentError(displayMessage = "mock failure"))
        }
    }

    /** Never completes — used to inspect state while a call is still in flight. */
    private class NeverRespondingEngine : AgentEngine {
        override suspend fun generateResponse(history: List<Message>): AgentResult {
            kotlinx.coroutines.delay(Long.MAX_VALUE)
            error("unreachable")
        }
    }
}
