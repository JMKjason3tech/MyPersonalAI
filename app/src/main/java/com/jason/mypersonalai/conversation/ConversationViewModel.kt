package com.jason.mypersonalai.conversation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jason.mypersonalai.agent.AgentEngine
import com.jason.mypersonalai.agent.AgentResult
import com.jason.mypersonalai.agent.Message
import com.jason.mypersonalai.agent.MockAgentEngine
import com.jason.mypersonalai.agent.Role
import com.jason.mypersonalai.history.ConversationHistoryStore
import com.jason.mypersonalai.history.StoredConversation
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversationViewModel(
    context: Context? = null,
    private val agentEngine: AgentEngine = MockAgentEngine(context = context)
) : ViewModel() {
    private val appContext = context?.applicationContext
    private val historyStore = appContext?.let(::ConversationHistoryStore)
    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        val stored = historyStore?.load().orEmpty()
        val first = stored.firstOrNull()
        if (first != null) {
            applyConversation(first)
        } else {
            newChat()
        }
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _uiState.value.isBusy) return
        ensureConversation()
        val userMessage = Message(UUID.randomUUID().toString(), Role.USER, text, System.currentTimeMillis())
        _uiState.update { current ->
            current.copy(
                conversationTitle = if (current.messages.isEmpty()) makeTitle(text) else current.conversationTitle,
                messages = current.messages + userMessage,
                isBusy = true,
                errorMessage = null
            )
        }
        persistCurrent()
        val conversationId = _uiState.value.conversationId
        val requestMessages = _uiState.value.messages
        viewModelScope.launch {
            when (val result = agentEngine.generateResponse(requestMessages)) {
                is AgentResult.Success -> {
                    if (_uiState.value.conversationId == conversationId) {
                        _uiState.update { current -> current.copy(messages = current.messages + result.message, isBusy = false) }
                        persistCurrent()
                    }
                }
                is AgentResult.Failure -> {
                    if (_uiState.value.conversationId == conversationId) {
                        _uiState.update { current -> current.copy(isBusy = false, errorMessage = result.error.displayMessage) }
                    }
                }
            }
        }
    }

    fun newChat() {
        _uiState.value = ConversationUiState(conversationId = UUID.randomUUID().toString())
        refreshHistory()
    }

    fun resetCurrentChat() {
        val id = _uiState.value.conversationId.ifBlank { UUID.randomUUID().toString() }
        historyStore?.delete(id)
        _uiState.value = ConversationUiState(conversationId = id)
        refreshHistory()
    }

    fun openConversation(id: String) {
        val stored = historyStore?.load()?.firstOrNull { it.id == id } ?: return
        applyConversation(stored)
    }

    fun deleteConversation(id: String) {
        historyStore?.delete(id)
        if (_uiState.value.conversationId == id) newChat() else refreshHistory()
    }

    fun dismissError() { _uiState.update { it.copy(errorMessage = null) } }

    private fun ensureConversation() {
        if (_uiState.value.conversationId.isBlank()) _uiState.update { it.copy(conversationId = UUID.randomUUID().toString()) }
    }

    private fun applyConversation(stored: StoredConversation) {
        _uiState.value = ConversationUiState(stored.id, stored.title, stored.messages, history = summaries())
    }

    private fun persistCurrent() {
        val state = _uiState.value
        if (state.messages.isEmpty() || historyStore == null) return
        val stored = StoredConversation(state.conversationId, state.conversationTitle, System.currentTimeMillis(), state.messages)
        viewModelScope.launch(Dispatchers.IO) {
            historyStore.save(stored)
            withContext(Dispatchers.Main) { refreshHistory() }
        }
    }

    private fun refreshHistory() { _uiState.update { it.copy(history = summaries()) } }

    private fun summaries(): List<ConversationSummary> = historyStore?.load()?.map {
        ConversationSummary(it.id, it.title, it.updatedAt, it.messages.size)
    }.orEmpty()

    private fun makeTitle(text: String): String = text.replace(Regex("\\s+"), " ").trim().let { if (it.length <= 34) it else it.take(31) + "…" }
}
