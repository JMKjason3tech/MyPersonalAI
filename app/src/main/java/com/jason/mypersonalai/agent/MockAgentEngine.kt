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
import com.jason.mypersonalai.tools.impl.AndroidAssistantTool
import com.jason.mypersonalai.tools.impl.DeviceControlTool
import com.jason.mypersonalai.tools.impl.DeviceInfoTool
import com.jason.mypersonalai.tools.impl.EchoTool
import com.jason.mypersonalai.tools.impl.OpenAppTool
import com.jason.mypersonalai.tools.impl.OpenSettingsTool
import com.jason.mypersonalai.tools.impl.ResetTool
import java.util.UUID
import kotlinx.coroutines.delay

class MockAgentEngine(context: Context? = null, private val responseDelayMillis: Long = 400L) : AgentEngine {
    private val toolRegistry = ToolRegistry().apply {
        register(CalculatorTool()); register(EchoTool()); register(ResetTool())
        if (context != null) {
            val appContext = context.applicationContext
            register(DeviceControlTool(appContext)); register(OpenAppTool(appContext))
            register(DeviceInfoTool(AndroidBatteryInfoProvider(appContext), AndroidNetworkInfoProvider(appContext), AndroidStorageInfoProvider(), AndroidDeviceInfoProvider(appContext), AndroidNetworkSpeedTestProvider()))
            register(OpenSettingsTool(AndroidSettingsLauncherImpl(appContext))); register(AndroidAssistantTool(appContext))
        }
    }
    private val toolRouter = ToolRouter(toolRegistry)
    private var pendingToolId: String? = null
    private var pendingRawInput: String? = null
    private var awaitingControlValue = false

    override suspend fun generateResponse(history: List<Message>): AgentResult {
        val lastUserMessage = history.lastOrNull { it.role == Role.USER } ?: return AgentResult.Failure(AgentError("No user message found to respond to."))
        val text = lastUserMessage.text.trim()
        if (pendingToolId != null) return handlePendingConfirmation(text)
        if (awaitingControlValue) { val raw=pendingRawInput ?: ""; awaitingControlValue=false; pendingRawInput=null; return runTool("device_control", "$raw $text", ConfirmationResult.Approved) }
        if (text.startsWith("echo ", true)) return runTool("echo", text.substring("echo ".length))
        if (isArithmeticExpression(text)) return runTool("calculator", text)
        if (text.equals("reset", true)) return runTool("reset", text)

        val resolution = ContextualIntentResolver.resolve(text, history).resolution
        when (resolution.intent) {
            IntentResolver.Intent.OPEN_SETTINGS -> if (toolRegistry.find("open_settings") != null) return runTool("open_settings", ContextualIntentResolver.canonicalCommand(resolution) ?: text)
            IntentResolver.Intent.DEVICE_CONTROL -> if (toolRegistry.find("device_control") != null) return runTool("device_control", ContextualIntentResolver.canonicalCommand(resolution) ?: text)
            IntentResolver.Intent.OPEN_APP -> if (toolRegistry.find("open_app") != null) return runTool("open_app", ContextualIntentResolver.canonicalCommand(resolution) ?: text)
            IntentResolver.Intent.CONTACTS, IntentResolver.Intent.CALL_LOG, IntentResolver.Intent.FILES, IntentResolver.Intent.MEDIA, IntentResolver.Intent.WIFI_BLUETOOTH -> if (toolRegistry.find("android_assistant") != null) return runTool("android_assistant", text)
            IntentResolver.Intent.STOP_SPEAKING -> return assistantSuccess("Stopped speaking.")
            IntentResolver.Intent.DEVICE_INFO -> if (toolRegistry.find("device_info") != null) return runTool("device_info", ContextualIntentResolver.canonicalCommand(resolution) ?: text)
            IntentResolver.Intent.UNKNOWN -> Unit
        }
        delay(responseDelayMillis)
        val turnNumber = history.count { it.role == Role.ASSISTANT } + 1
        return assistantSuccess("Mock response #$turnNumber — no AI provider is connected yet. You said: \"${lastUserMessage.text}\"")
    }

    private suspend fun handlePendingConfirmation(text: String): AgentResult = when {
        text.equals("confirm", true) -> { val id=pendingToolId!!; val raw=pendingRawInput!!; pendingToolId=null; pendingRawInput=null; if (id=="device_control" && !Regex("\\d{1,3}\\s*%?").containsMatchIn(raw) && !raw.contains("mute") && !raw.contains("unmute")) { awaitingControlValue=true; pendingRawInput=raw; assistantSuccess("Confirmed. Tell me the target, for example 60% or louder.") } else runTool(id,raw,ConfirmationResult.Approved) }
        text.equals("cancel", true) -> { pendingToolId=null; pendingRawInput=null; awaitingControlValue=false; assistantSuccess("Cancelled: the action was not performed.") }
        else -> assistantSuccess("A confirmation is pending. Type \"confirm\" to proceed or \"cancel\" to abort.")
    }

    private suspend fun runTool(toolId: String, rawInput: String, confirmation: ConfirmationResult? = null): AgentResult = when (val result=toolRouter.route(toolId, ToolInput(raw=rawInput), confirmation)) {
        is ToolExecutionResult.Success -> assistantSuccess(result.output)
        is ToolExecutionResult.Failure -> AgentResult.Failure(AgentError(result.error.displayMessage, result.error.cause))
        is ToolExecutionResult.RequiresConfirmation -> { pendingToolId=toolId; pendingRawInput=rawInput; assistantSuccess("${result.reason} Type \"confirm\" to proceed or \"cancel\" to abort.") }
    }
    private fun assistantSuccess(text:String)=AgentResult.Success(Message(UUID.randomUUID().toString(),Role.ASSISTANT,text,System.currentTimeMillis()))
    private fun isArithmeticExpression(text:String):Boolean { if(text.isEmpty()) return false; if(!Regex("^[0-9+\\-*/(). ]+$").matches(text)) return false; return text.any{it.isDigit()} }
}
