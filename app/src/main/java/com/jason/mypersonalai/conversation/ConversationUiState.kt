package com.jason.mypersonalai.conversation

import com.jason.mypersonalai.agent.Message

/** Everything the conversation UI needs to render itself. */
data class ConversationUiState(
    val conversationId: String = "",
    val conversationTitle: String = "New chat",
    val messages: List<Message> = emptyList(),
    val history: List<ConversationSummary> = emptyList(),
    val isBusy: Boolean = false,
    val errorMessage: String? = null
)

data class ConversationSummary(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messageCount: Int
)
