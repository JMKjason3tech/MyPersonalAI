package com.jason.mypersonalai.agent

import android.content.Context
import com.jason.mypersonalai.android.adapters.AndroidBatteryInfoProvider
import com.jason.mypersonalai.android.adapters.AndroidNetworkInfoProvider
import com.jason.mypersonalai.android.adapters.AndroidNetworkSpeedTestProvider
import com.jason.mypersonalai.android.adapters.AndroidDeviceInfoProvider
import com.jason.mypersonalai.android.adapters.AndroidStorageInfoProvider
import com.jason.mypersonalai.tools.ConfirmationResult
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import com.jason.mypersonalai.tools.ToolRegistry
import com.jason.mypersonalai.tools.ToolRouter
import com.jason.mypersonalai.tools.impl.CalculatorTool
import com.jason.mypersonalai.tools.impl.DeviceInfoTool
import com.jason.mypersonalai.tools.impl.EchoTool
import com.jason.mypersonalai.tools.impl.ResetTool
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Deterministic local [AgentEngine].
 *
 * No AI provider is connected. Normal tools are local and deterministic;
 * the explicit "speed test" command is the one exception because it
 * intentionally performs a small Internet throughput measurement.
 *
 * Milestone 3 added tool registry/routing. Milestone 4 added a
 * text-based confirmation flow for risky tools. Milestone 5 adds the
 * first real Android capability: [DeviceInfoTool], which only
 * registers when a real [Context] is supplied (it's null and safely
 * skipped in plain JVM unit tests — no Android dependency needed
 * there at all). This is why [context] is optional: it lets this
 * class remain fully unit-testable without Robolectric or a device,
 * while still doing real Android work when actually running on one.
 */
class MockAgentEngine(
    context: Context? = null,
    private val responseDelayMillis: Long = 400L
) : AgentEngine {

    private val toolRegistry = ToolRegistry().apply {
        register(CalculatorTool())
        register(EchoTool())
        register(ResetTool())
        if (context != null) {
            val appContext = context.applicationContext
            register(
                DeviceInfoTool(
                    batteryInfoProvider = AndroidBatteryInfoProvider(appContext),
                    networkInfoProvider = AndroidNetworkInfoProvider(appContext),
                    storageInfoProvider = AndroidStorageInfoProvider(),
                    deviceInfoProvider = AndroidDeviceInfoProvider(appContext),
                    networkSpeedTestProvider = AndroidNetworkSpeedTestProvider()
                )
            )
        }
    }
    private val toolRouter = ToolRouter(toolRegistry)

    // Milestone 4: a single pending tool call awaiting explicit
    // confirmation.
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

        // Echo trigger.
        if (text.startsWith("echo ", ignoreCase = true)) {
            return runTool(toolId = "echo", rawInput = text.substring("echo ".length))
        }

        // Calculator trigger.
        if (isArithmeticExpression(text)) {
            return runTool(toolId = "calculator", rawInput = text)
        }

        // Reset trigger. HIGH risk -> gated by ConfirmationPolicy.
        if (text.equals("reset", ignoreCase = true)) {
            return runTool(toolId = "reset", rawInput = text)
        }

        // Device info trigger. Only actually routes if the tool got
        // registered (i.e. a real Context was supplied) — otherwise
        // this falls through to the generic mock reply below, which
        // is the "degrade gracefully" behavior the plan calls for.
        val deviceInfoKeywords = listOf("battery", "network", "wifi", "wi-fi", "storage", "device info", "device information", "phone info", "hardware", "speed test", "internet speed", "network speed", "download speed", "upload speed")
        val looksLikeDeviceInfoQuery = deviceInfoKeywords.any { text.contains(it, ignoreCase = true) }
        if (looksLikeDeviceInfoQuery && toolRegistry.find("device_info") != null) {
            return runTool(toolId = "device_info", rawInput = text)
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
