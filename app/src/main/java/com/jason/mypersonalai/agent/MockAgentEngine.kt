package com.jason.mypersonalai.agent

import android.content.Context
import com.jason.mypersonalai.android.adapters.AndroidBatteryInfoProvider
import com.jason.mypersonalai.android.adapters.AndroidNetworkInfoProvider
import com.jason.mypersonalai.android.adapters.AndroidNetworkSpeedTestProvider
import com.jason.mypersonalai.android.adapters.AndroidDeviceInfoProvider
import com.jason.mypersonalai.android.adapters.AndroidSettingsLauncherImpl
import com.jason.mypersonalai.android.adapters.AndroidStorageInfoProvider
import com.jason.mypersonalai.tools.ConfirmationResult
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import com.jason.mypersonalai.tools.ToolRegistry
import com.jason.mypersonalai.tools.ToolRouter
import com.jason.mypersonalai.tools.impl.CalculatorTool
import com.jason.mypersonalai.tools.impl.DeviceInfoTool
import com.jason.mypersonalai.tools.impl.EchoTool
import com.jason.mypersonalai.tools.impl.OpenSettingsTool
import com.jason.mypersonalai.tools.impl.ResetTool
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Deterministic local AgentEngine.
 *
 * Android capabilities are exposed through registered tools. Settings
 * actions only launch the official Android Settings UI; they do not modify
 * protected system state directly.
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
            register(OpenSettingsTool(AndroidSettingsLauncherImpl(appContext)))
        }
    }

    private val toolRouter = ToolRouter(toolRegistry)
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

        if (text.startsWith("echo ", ignoreCase = true)) {
            return runTool(toolId = "echo", rawInput = text.substring("echo ".length))
        }

        if (isArithmeticExpression(text)) {
            return runTool(toolId = "calculator", rawInput = text)
        }

        if (text.equals("reset", ignoreCase = true)) {
            return runTool(toolId = "reset", rawInput = text)
        }

        val resolution = IntentResolver.resolve(text)
        when (resolution.intent) {
            IntentResolver.Intent.OPEN_SETTINGS -> {
                if (toolRegistry.find("open_settings") != null) {
                    return runTool(toolId = "open_settings", rawInput = text)
                }
            }
            IntentResolver.Intent.DEVICE_INFO -> {
                if (toolRegistry.find("device_info") != null) {
                    return runTool(toolId = "device_info", rawInput = text)
                }
            }
            IntentResolver.Intent.UNKNOWN -> Unit
        }

        // Preserve the established Milestone 2/3/4 fallback contract:
        // unhandled natural-language input receives the deterministic mock
        // response. IntentResolver controls routing only; it does not change
        // the existing generic-response behavior.
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
