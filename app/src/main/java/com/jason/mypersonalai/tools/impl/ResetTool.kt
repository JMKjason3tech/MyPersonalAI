package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.tools.RiskLevel
import com.jason.mypersonalai.tools.Tool
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput

/**
 * Demo HIGH-risk tool used to prove the Milestone 4 confirmation gate.
 *
 * Deliberately a safe no-op — it does not reset, delete, or modify
 * anything in the app. Its only purpose is to be a tool whose risk
 * level forces [com.jason.mypersonalai.tools.ToolRouter] to require
 * confirmation before running, the same way [EchoTool] existed in
 * Milestone 3 purely to prove a second tool could be registered and
 * routed. A tool with a real destructive effect is deliberately out
 * of scope here — that's Milestone 5+ territory, once Android
 * capability adapters exist.
 */
class ResetTool : Tool {
    override val id: String = "reset"
    override val description: String =
        "Demo high-risk action (no-op) used to prove the confirmation gate."
    override val riskLevel: RiskLevel = RiskLevel.HIGH

    override suspend fun execute(input: ToolInput): ToolExecutionResult {
        return ToolExecutionResult.Success(
            output = "Reset acknowledged (this is a safe no-op demo action)."
        )
    }
}
