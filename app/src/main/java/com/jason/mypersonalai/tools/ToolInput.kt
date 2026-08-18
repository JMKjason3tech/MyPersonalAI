package com.jason.mypersonalai.tools

/**
 * Input handed to a [Tool.execute] call.
 *
 * [raw] is the original text the tool was triggered from — useful for
 * simple tools that parse their own input (e.g. [CalculatorTool]).
 * [arguments] is a minimal structured slot for tools that need named
 * parameters; unused by Milestone 3's tools but kept so [ToolRouter]
 * and [Tool] don't need to change shape once a tool needs it.
 */
data class ToolInput(
    val raw: String,
    val arguments: Map<String, String> = emptyMap()
)
