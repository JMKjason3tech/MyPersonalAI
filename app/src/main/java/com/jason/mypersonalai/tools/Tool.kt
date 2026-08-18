package com.jason.mypersonalai.tools

/**
 * A first-class, self-describing capability the agent can invoke.
 *
 * Tools are looked up by [id] through [ToolRegistry] and invoked through
 * [ToolRouter] — the agent never branches on tool-specific logic itself.
 * Milestone 3 tools are deterministic and carry no Android permissions;
 * [riskLevel] exists now so the risk/confirmation system (Milestone 4)
 * has something to read, but nothing enforces it yet.
 */
interface Tool {
    /** Stable, unique identifier used for registration and routing. */
    val id: String

    /** Short human-readable description of what this tool does. */
    val description: String

    /** Declared risk classification. Not yet enforced — see Milestone 4. */
    val riskLevel: RiskLevel

    /**
     * Run the tool.
     *
     * Implementations must never throw for expected failure modes
     * (invalid input, etc.) — report them via [ToolExecutionResult.Failure]
     * instead, matching the project's structured-failure rule.
     */
    suspend fun execute(input: ToolInput): ToolExecutionResult
}

/** Declared risk level for a [Tool]. Not yet enforced anywhere. */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
