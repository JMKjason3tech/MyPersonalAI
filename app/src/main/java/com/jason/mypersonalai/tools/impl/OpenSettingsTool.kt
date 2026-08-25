package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.android.capabilities.AndroidSettingsLauncher
import com.jason.mypersonalai.tools.RiskLevel
import com.jason.mypersonalai.tools.Tool
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import com.jason.mypersonalai.tools.ToolError

/** Opens an official Android Settings destination. No privileged setting is changed. */
class OpenSettingsTool(
    private val launcher: AndroidSettingsLauncher
) : Tool {
    override val id: String = "open_settings"
    override val description: String = "Open an Android system Settings screen."
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(input: ToolInput): ToolExecutionResult {
        val request = input.raw.trim()
        if (request.isBlank()) {
            return ToolExecutionResult.Failure(ToolError("Tell me which Settings screen you want to open."))
        }

        val result = launcher.open(request)
        return if (result.success) {
            ToolExecutionResult.Success(result.message)
        } else {
            ToolExecutionResult.Failure(ToolError(result.message))
        }
    }
}
