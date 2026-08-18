package com.jason.mypersonalai.tools

/**
 * Holds the set of [Tool]s available to the agent.
 *
 * Deliberately dumb: registration, lookup, and listing only. Routing
 * decisions (which tool a request maps to, whether it's allowed to run)
 * belong to [ToolRouter], not here — this class doesn't know anything
 * about *why* a tool is being looked up.
 */
class ToolRegistry {

    private val toolsById = mutableMapOf<String, Tool>()

    /**
     * Register [tool].
     *
     * @throws IllegalArgumentException if a tool with the same [Tool.id]
     *   is already registered. Duplicate ids are a programming error,
     *   not a runtime condition callers should handle — hence a thrown
     *   exception here rather than a structured result, unlike
     *   [Tool.execute].
     */
    fun register(tool: Tool) {
        require(!toolsById.containsKey(tool.id)) {
            "Tool with id '${tool.id}' is already registered."
        }
        toolsById[tool.id] = tool
    }

    /** Look up a tool by id, or null if no such tool is registered. */
    fun find(id: String): Tool? = toolsById[id]

    /** All currently registered tools, in registration order. */
    fun all(): List<Tool> = toolsById.values.toList()
}
