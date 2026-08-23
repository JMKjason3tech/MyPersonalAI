package com.jason.mypersonalai.tools

/**
 * Validates, risk-checks, and executes a tool request against a [ToolRegistry].
 *
 * As of Milestone 4, this is where [ConfirmationPolicy] is enforced:
 * if a tool's declared [RiskLevel] requires confirmation and none has
 * been supplied yet, execution is refused and a [ToolExecutionResult.RequiresConfirmation]
 * is returned instead. The caller must invoke [route] again, this time
 * passing the user's [ConfirmationResult], for the tool to actually run.
 * This is the single enforcement point for the project's "never silently
 * decide a risky action is okay" rule — [Tool] implementations never see
 * or reason about this at all.
 */
class ToolRouter(
    private val registry: ToolRegistry
) {
    suspend fun route(
        toolId: String,
        input: ToolInput,
        confirmation: ConfirmationResult? = null
    ): ToolExecutionResult {
        val tool = registry.find(toolId)
            ?: return ToolExecutionResult.Failure(
                ToolError(displayMessage = "Unknown tool: '$toolId'.")
            )

        if (ConfirmationPolicy.requiresConfirmation(tool.riskLevel)) {
            when (confirmation) {
                null -> return ToolExecutionResult.RequiresConfirmation(
                    reason = "The '$toolId' tool is ${tool.riskLevel} risk and requires confirmation."
                )
                ConfirmationResult.Denied -> return ToolExecutionResult.Failure(
                    ToolError(displayMessage = "Cancelled: '$toolId' was not confirmed.")
                )
                ConfirmationResult.Approved -> {
                    // Fall through and execute below.
                }
            }
        }

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
