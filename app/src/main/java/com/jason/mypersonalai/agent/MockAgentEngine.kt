package com.jason.mypersonalai.agent

import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Deterministic, fully offline [AgentEngine].
 *
 * No AI provider, no network call, no randomness — the same
 * conversation history always produces the same reply shape. This
 * exists purely to prove the end-to-end flow (UI -> state layer ->
 * AgentEngine -> state layer -> UI) works before any real provider is
 * wired in.
 *
 * The artificial [responseDelayMillis] delay exists only so the UI's
 * busy/loading state has something real to show; it is not simulating
 * "thinking", just standing in for the latency a real provider call
 * will have.
 */
class MockAgentEngine(
    private val responseDelayMillis: Long = 400L
) : AgentEngine {

    override suspend fun generateResponse(history: List<Message>): AgentResult {
        val lastUserMessage = history.lastOrNull { it.role == Role.USER }
            ?: return AgentResult.Failure(
                AgentError(displayMessage = "No user message found to respond to.")
            )

        delay(responseDelayMillis)

        val turnNumber = history.count { it.role == Role.ASSISTANT } + 1
        val replyText = "Mock response #$turnNumber — no AI provider is connected yet. " +
            "You said: \"${lastUserMessage.text}\""

        return AgentResult.Success(
            Message(
                id = UUID.randomUUID().toString(),
                role = Role.ASSISTANT,
                text = replyText,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }
}
