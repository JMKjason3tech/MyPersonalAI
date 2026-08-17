package com.jason.mypersonalai.conversation

import com.jason.mypersonalai.agent.Message

/**
 * Everything the conversation screen needs to render itself.
 *
 * Owned and mutated only by [ConversationViewModel]; the UI treats
 * this as read-only.
 */
data class ConversationUiState(
    val messages: List<Message> = emptyList(),
    val isBusy: Boolean = false,
    val errorMessage: String? = null
)
