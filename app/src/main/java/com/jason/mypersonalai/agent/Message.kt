package com.jason.mypersonalai.agent

/**
 * Who authored a [Message].
 *
 * Deliberately just two values for now. A future milestone may add
 * a SYSTEM or TOOL role once the tool registry/router exists — this
 * enum is the natural extension point for that, not something to
 * pre-build now.
 */
enum class Role {
    USER,
    ASSISTANT
}

/**
 * A single turn in a conversation.
 *
 * This is the shared currency between the conversation/state layer
 * and the agent orchestration layer. The UI never constructs these
 * directly except to display them.
 */
data class Message(
    val id: String,
    val role: Role,
    val text: String,
    val timestampMillis: Long
)
