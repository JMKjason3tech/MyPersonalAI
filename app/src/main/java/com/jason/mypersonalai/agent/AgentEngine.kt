package com.jason.mypersonalai.agent

/**
 * Agent orchestration layer boundary.
 *
 * This is the seam between the conversation/state layer and everything
 * that eventually produces a response: for now that's [MockAgentEngine],
 * later a real AI-provider-backed engine, and later still one that can
 * route through a tool registry. None of that exists yet — the
 * conversation layer only ever depends on this interface, never on a
 * concrete implementation, so those additions won't require UI or
 * state-layer changes.
 *
 * Implementations must never throw for expected failure modes (no
 * network, provider error, invalid input, etc.) — they report them via
 * [AgentResult.Failure] instead, per the project's error-handling rule
 * that every layer returns structured success/failure information.
 */
interface AgentEngine {
    /**
     * Produce the next assistant [Message] given the conversation so far.
     *
     * @param history the full conversation, oldest first, including the
     *   most recent user message. Implementations do not mutate this list.
     */
    suspend fun generateResponse(history: List<Message>): AgentResult
}

/** Structured outcome of [AgentEngine.generateResponse]. */
sealed class AgentResult {
    data class Success(val message: Message) : AgentResult()
    data class Failure(val error: AgentError) : AgentResult()
}

/**
 * A user-facing-safe description of why the agent could not respond.
 *
 * [displayMessage] should always be safe to show directly in the UI —
 * plain language, no stack traces or raw exception text.
 */
data class AgentError(
    val displayMessage: String,
    val cause: Throwable? = null
)
