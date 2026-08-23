package com.jason.mypersonalai.tools

/**
 * The user's decision on a pending [ConfirmationRequest].
 *
 * Passed back into [ToolRouter.route] to let a previously-gated tool
 * call actually proceed (or be cleanly cancelled). There is no third
 * option — a tool call is either approved or denied, deliberately,
 * never left ambiguous.
 */
sealed class ConfirmationResult {
    object Approved : ConfirmationResult()
    object Denied : ConfirmationResult()
}
