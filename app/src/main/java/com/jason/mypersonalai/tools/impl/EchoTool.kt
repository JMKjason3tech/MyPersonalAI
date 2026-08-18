package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.tools.RiskLevel
import com.jason.mypersonalai.tools.Tool
import com.jason.mypersonalai.tools.ToolError
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput

/**
 * Returns its input unchanged.
 *
 * Exists purely to prove that a *second*, distinct tool can be
 * registered and routed correctly alongside [CalculatorTool] — i.e.
 * that [com.jason.mypersonalai.tools.ToolRegistry] and
 * [com.jason.mypersonalai.tools.ToolRouter] generalize beyond a single
 * tool. Not a useful feature on its own.
 */
class EchoTool : Tool {
    override val id: String = "echo"
    override val description: String = "Returns the given text unchanged."
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(input: ToolInput): ToolExecutionResult {
        val text = input.raw.trim()
        return if (text.isEmpty()) {
            ToolExecutionResult.Failure(
                ToolError(displayMessage = "Echo error: nothing to echo.")
            )
        } else {
            ToolExecutionResult.Success(output = text)
        }
    }
}
