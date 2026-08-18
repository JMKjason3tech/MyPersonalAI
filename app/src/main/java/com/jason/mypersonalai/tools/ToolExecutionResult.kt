package com.jason.mypersonalai.tools

/**
 * Structured outcome of a [Tool.execute] call.
 *
 * Mirrors [com.jason.mypersonalai.agent.AgentResult]'s shape deliberately —
 * the codebase should have one consistent pattern for "this layer's
 * structured result", not a different one per layer.
 *
 * [RequiresConfirmation] is included now even though nothing produces it
 * until Milestone 4 (risk/confirmation system) — the router and any
 * caller need to be able to handle this case from the start so adding
 * real confirmation logic later doesn't require touching every caller.
 */
sealed class ToolExecutionResult {
    data class Success(val output: String) : ToolExecutionResult()
    data class Failure(val error: ToolError) : ToolExecutionResult()
    data class RequiresConfirmation(val reason: String) : ToolExecutionResult()
}

/**
 * A user-facing-safe description of why a tool could not complete.
 *
 * [displayMessage] should always be safe to show directly in the UI —
 * plain language, no stack traces or raw exception text.
 */
data class ToolError(
    val displayMessage: String,
    val cause: Throwable? = null
)
