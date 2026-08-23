package com.jason.mypersonalai.agent

import com.jason.mypersonalai.tools.ConfirmationResult
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import com.jason.mypersonalai.tools.ToolRegistry
import com.jason.mypersonalai.tools.ToolRouter
import com.jason.mypersonalai.tools.impl.CalculatorTool
import com.jason.mypersonalai.tools.impl.EchoTool
import com.jason.mypersonalai.tools.impl.ResetTool
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Deterministic, fully offline [AgentEngine].
 *
 * No AI provider, no network call, no randomness — the same
 * conversation history always produces the same reply shape. This
 * exists purely to prove the end-to-end flow works before any real
 * provider is wired in.
 *
 * Milestone 3 added a [ToolRegistry]/[ToolRouter] pair with simple
 * pattern-matching to decide whether a message should route to a tool.
 * Milestone 4 adds a text-based confirmation flow on top of that: when
 * a tool's risk level requires confirmation (see [com.jason.mypersonalai.tools.ConfirmationPolicy]),
 * this engine holds the pending request in [pendingToolId]/[pendingRawInput]
 * and only proceeds once the next message is exactly "confirm" or "cancel".
 * There is no confirmation dialog UI yet — that's later polish — so this
 * is a deliberately simple, fully testable stand-in that still proves
 * the underlying architecture (the router's gate, not just the UI) works.
 */
class MockAgentEngine(
    private val responseDelayMillis: Long = 400L
) : AgentEngine {

    private val toolRegistry = ToolRegistry().apply {
        register(CalculatorTool())
        register(EchoTool())
        register(ResetTool())
    }
    private val toolRouter = ToolRouter(toolRegistry)

    // Milestone 4: a single pending tool call awaiting explicit
    // confirmation. Single-slot by design — only one confirmation can
    // be outstanding at a time, which keeps the flow unambiguous.
    private var pendingToolId: String? = null
    private var pendingRawInput: String? = null

    override suspend fun generateResponse(history: List<Message>): AgentResult {
        val lastUserMessage = history.lastOrNull { it.role == Role.USER }
            ?: return AgentResult.Failure(
                AgentError(displayMessage = "No user message found to respond to.")
            )

        val text = lastUserMessage.text.trim()

        if (pendingToolId != null) {
            return handlePendingConfirmation(text)
        }

        // Echo trigger: message starts with "echo " followed by content.
        if (text.startsWith("echo ", ignoreCase = true)) {
            return runTool(toolId = "echo", rawInput = text.substring("echo ".length))
        }

        // Calculator trigger: the entire message is a pure arithmetic
        // expression (digits, whitespace, + - * / ( ) . only), and
        // contains at least one digit.
        if (isArithmeticExpression(text)) {
            return runTool(toolId = "calculator", rawInput = text)
        }

        // Reset trigger: exact message "reset". HIGH risk -> will be
        // gated by ConfirmationPolicy via ToolRouter.
        if (text.equals("reset", ignoreCase = true)) {
            return runTool(toolId = "reset", rawInput = text)
        }

        // Fallback: same Milestone 2 mock behavior as before.
        delay(responseDelayMillis)
        val turnNumber = history.count { it.role == Role.ASSISTANT } + 1
        val replyText = "Mock response #$turnNumber — no AI provider is connected yet. " +
            "You said: \"${lastUserMessage.text}\""

        return assistantSuccess(replyText)
    }

    private suspend fun handlePendingConfirmation(text: String): AgentResult {
        val toolId = pendingToolId!!
        val rawInput = pendingRawInput!!

        return when {
            text.equals("confirm", ignoreCase = true) -> {
                pendingToolId = null
                pendingRawInput = null
                runTool(toolId = toolId, rawInput = rawInput, confirmation = ConfirmationResult.Approved)
            }
            text.equals("cancel", ignoreCase = true) -> {
                pendingToolId = null
                pendingRawInput = null
                assistantSuccess("Cancelled: '$toolId' was not performed.")
            }
            else -> assistantSuccess(
                "A confirmation is pending for '$toolId'. Type \"confirm\" to proceed or \"cancel\" to abort."
            )
        }
    }

    private suspend fun runTool(
        toolId: String,
        rawInput: String,
        confirmation: ConfirmationResult? = null
    ): AgentResult {
        return when (val result = toolRouter.route(toolId, ToolInput(raw = rawInput), confirmation)) {
            is ToolExecutionResult.Success -> assistantSuccess(result.output)
            is ToolExecutionResult.Failure -> AgentResult.Failure(
                AgentError(displayMessage = result.error.displayMessage, cause = result.error.cause)
            )
            is ToolExecutionResult.RequiresConfirmation -> {
                pendingToolId = toolId
                pendingRawInput = rawInput
                assistantSuccess("${result.reason} Type \"confirm\" to proceed or \"cancel\" to abort.")
            }
        }
    }

    private fun assistantSuccess(text: String): AgentResult.Success = AgentResult.Success(
        Message(
            id = UUID.randomUUID().toString(),
            role = Role.ASSISTANT,
            text = text,
            timestampMillis = System.currentTimeMillis()
        )
    )

    private fun isArithmeticExpression(text: String): Boolean {
        if (text.isEmpty()) return false
        val allowedChars = Regex("^[0-9+\\-*/(). ]+$")
        if (!allowedChars.matches(text)) return false
        return text.any { it.isDigit() }
    }
}
