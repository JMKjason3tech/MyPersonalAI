package com.jason.mypersonalai.tools

/**
 * Deterministic policy for whether a given [RiskLevel] requires
 * explicit user confirmation before a tool executes.
 *
 * A single, centralized decision point — tools never decide this for
 * themselves, and [ToolRouter] is the only caller. Per the Master Plan
 * (section 5): confirmation must be deterministic and explicit, and
 * the AI must never be allowed to silently decide a risky action is
 * okay. Keeping the rule in one object rather than scattered inline
 * checks is what makes that guarantee possible to audit.
 */
object ConfirmationPolicy {
    fun requiresConfirmation(riskLevel: RiskLevel): Boolean {
        return when (riskLevel) {
            RiskLevel.LOW -> false
            RiskLevel.MEDIUM -> true
            RiskLevel.HIGH -> true
        }
    }
}
