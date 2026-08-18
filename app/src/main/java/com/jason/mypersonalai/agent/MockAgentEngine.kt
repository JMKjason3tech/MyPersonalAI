package com.jason.mypersonalai.agent

import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import com.jason.mypersonalai.tools.ToolRegistry
import com.jason.mypersonalai.tools.ToolRouter
import com.jason.mypersonalai.tools.impl.CalculatorTool
import com.jason.mypersonalai.tools.impl.EchoTool
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
 * As of Milestone 3, this engine also owns a [ToolRegistry]/[ToolRouter]
 * pair and does simple pattern-matching to decide whether a message
 * should be routed to a tool. This is intentionally crude — real
 * intent detection is Milestone 6's job (real AI provider). The point
 * here is only to prove the tool pipeline (agent -> router -> registry
 * -> tool -> structured result -> agent) actually works end to end.
 *
 * The artificial [responseDelayMillis] delay applies only to the
 * non-tool fallback path, standing in for the latency a real provider
 * call will have. Tool calls are not artificially delayed.
 */
class MockAgentEngine(
    private val responseDelayMillis: Long = 400L
) : AgentEngine {

    private val toolRegistry = ToolRegistry().apply {
        register(CalculatorTool())
        register(EchoTool())
    }
    private val toolRouter = ToolRouter(toolRegistry)

    override suspend fun generateResponse(history: List<Message>): AgentResult {
        val lastUserMessage = history.lastOrNull { it.role == Role.USER }
            ?: return AgentResult.Failure(
                AgentError(displayMessage = "No user message found to respond to.")
            )

        val text = lastUserMessage.text.trim()

        // Echo trigger: message starts with "echo " followed by content.
        if (text.startsWith("echo ", ignoreCase = true)) {
            val echoInput = text.substring("echo ".length)
            return runTool(toolId = "echo", rawInput = echoInput)
        }

        // Calculator trigger: the entire message is a pure arithmetic
        // expression (digits, whitespace, + - * / ( ) . only), and
        // contains at least one digit.
        if (isArithmeticExpression(text)) {
            return runTool(toolId = "calculator", rawInput = text)
        }

        // Fallback: same Milestone 2 mock behavior as before.
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

    private suspend fun runTool(toolId: String, rawInput: String): AgentResult {
        return when (val result = toolRouter.route(toolId, ToolInput(raw = rawInput))) {
            is ToolExecutionResult.Success -> AgentResult.Success(
                Message(
                    id = UUID.randomUUID().toString(),
                    role = Role.ASSISTANT,
                    text = result.output,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            is ToolExecutionResult.Failure -> AgentResult.Failure(
                AgentError(displayMessage = result.error.displayMessage, cause = result.error.cause)
            )
            is ToolExecutionResult.RequiresConfirmation -> AgentResult.Failure(
                // No confirmation UI exists yet (Milestone 4). Surface as a
                // clear failure rather than silently proceeding or hanging.
                AgentError(displayMessage = "This action requires confirmation, which isn't supported yet.")
            )
        }
    }

    private fun isArithmeticExpression(text: String): Boolean {
        if (text.isEmpty()) return false
        val allowedChars = Regex("^[0-9+\\-*/(). ]+$")
        if (!allowedChars.matches(text)) return false
        return text.any { it.isDigit() }
    }
}
