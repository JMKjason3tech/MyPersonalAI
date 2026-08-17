package com.jason.mypersonalai.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jason.mypersonalai.agent.AgentEngine
import com.jason.mypersonalai.agent.AgentResult
import com.jason.mypersonalai.agent.Message
import com.jason.mypersonalai.agent.MockAgentEngine
import com.jason.mypersonalai.agent.Role
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Conversation/state layer.
 *
 * This is the only thing the UI talks to. It owns the conversation
 * history and busy/error state, and is the sole caller of
 * [AgentEngine]. The UI layer never imports [AgentEngine] or any
 * concrete engine implementation — that boundary is what lets the
 * agent orchestration layer change (mock -> real provider -> tool-using)
 * without touching Compose code.
 *
 * [agentEngine] defaults to [MockAgentEngine] for milestone 2. A later
 * milestone swaps this default (or injects a different implementation)
 * without changing this class's public surface.
 */
class ConversationViewModel(
    private val agentEngine: AgentEngine = MockAgentEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    /**
     * Submit a new user message. No-ops on blank input. Ignored while
     * a response is already in flight, so rapid double-sends can't
     * race each other.
     */
    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _uiState.value.isBusy) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = Role.USER,
            text = text,
            timestampMillis = System.currentTimeMillis()
        )

        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                isBusy = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = agentEngine.generateResponse(_uiState.value.messages)) {
                is AgentResult.Success -> _uiState.update { current ->
                    current.copy(
                        messages = current.messages + result.message,
                        isBusy = false
                    )
                }
                is AgentResult.Failure -> _uiState.update { current ->
                    current.copy(
                        isBusy = false,
                        errorMessage = result.error.displayMessage
                    )
                }
            }
        }
    }

    /** Dismiss the current error banner without affecting the conversation. */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
