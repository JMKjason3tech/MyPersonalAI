package com.jason.mypersonalai.tools

/**
 * Describes a tool call that is waiting on user approval.
 *
 * Produced by [ToolRouter] when [ConfirmationPolicy] says a tool's
 * risk level requires it, and carried by [ToolExecutionResult.RequiresConfirmation]
 * up to whatever layer is responsible for actually asking the user
 * (in Milestone 4, [com.jason.mypersonalai.agent.MockAgentEngine]'s
 * text-based confirm/cancel flow; a real confirmation dialog is later UI work).
 */
data class ConfirmationRequest(
    val toolId: String,
    val riskLevel: RiskLevel,
    val reason: String
)
