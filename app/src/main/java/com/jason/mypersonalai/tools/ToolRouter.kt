package com.jason.mypersonalai.tools

/**
 * Validates and executes a tool request against a [ToolRegistry].
 *
 * This is the seam where risk evaluation and confirmation-gating will
 * be added in Milestone 4 — right now [route] only checks that the
 * requested tool exists and catches unexpected exceptions from
 * [Tool.execute], converting them into a structured [ToolExecutionResult.Failure]
 * so a misbehaving tool can never crash the agent layer above it.
 */
class ToolRouter(
    private val registry: ToolRegistry
) {
    suspend fun route(toolId: String, input: ToolInput): ToolExecutionResult {
        val tool = registry.find(toolId)
            ?: return ToolExecutionResult.Failure(
                ToolError(displayMessage = "Unknown tool: '$toolId'.")
            )

        return try {
            tool.execute(input)
        } catch (e: Exception) {
            ToolExecutionResult.Failure(
                ToolError(
                    displayMessage = "The '$toolId' tool failed unexpectedly.",
                    cause = e
                )
            )
        }
    }
}
