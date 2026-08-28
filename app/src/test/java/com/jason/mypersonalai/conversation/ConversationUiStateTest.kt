package com.jason.mypersonalai.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationUiStateTest {
    @Test
    fun newStateStartsAsEmptyNewChat() {
        val state = ConversationUiState()
        assertEquals("New chat", state.conversationTitle)
        assertEquals(0, state.messages.size)
        assertEquals(0, state.history.size)
    }
}
