package com.jason.mypersonalai.history

import android.content.Context
import android.util.Base64
import com.jason.mypersonalai.agent.Message
import com.jason.mypersonalai.agent.Role

/** Lightweight local conversation history. No network and no external database dependency. */
class ConversationHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<StoredConversation> = decode(prefs.getString(KEY_CHATS, "") ?: "")
        .sortedByDescending { it.updatedAt }

    fun save(conversation: StoredConversation) {
        val existing = load().filterNot { it.id == conversation.id }
        val all = (existing + conversation).sortedByDescending { it.updatedAt }.take(MAX_CONVERSATIONS)
        prefs.edit().putString(KEY_CHATS, encode(all)).apply()
    }

    fun delete(id: String) {
        val remaining = load().filterNot { it.id == id }
        prefs.edit().putString(KEY_CHATS, encode(remaining)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_CHATS).apply()
    }

    private fun encode(conversations: List<StoredConversation>): String = conversations.joinToString(CHAT_SEPARATOR) { chat ->
        listOf(chat.id, chat.title, chat.updatedAt.toString(), chat.messages.joinToString(MESSAGE_SEPARATOR) { message ->
            listOf(message.id, message.role.name, message.timestampMillis.toString(), message.text).joinToString(FIELD_SEPARATOR) { b64(it) }
        }).joinToString(FIELD_SEPARATOR) { b64(it) }
    }

    private fun decode(raw: String): List<StoredConversation> = raw.split(CHAT_SEPARATOR)
        .filter { it.isNotBlank() }
        .mapNotNull { chat ->
            val fields = chat.split(FIELD_SEPARATOR).mapNotNull(::unb64)
            if (fields.size < 4) return@mapNotNull null
            val messages = fields[3].split(MESSAGE_SEPARATOR).filter { it.isNotBlank() }.mapNotNull { encoded ->
                val parts = encoded.split(FIELD_SEPARATOR).mapNotNull(::unb64)
                if (parts.size < 4) return@mapNotNull null
                runCatching {
                    Message(parts[0], Role.valueOf(parts[1]), parts[3], parts[2].toLong())
                }.getOrNull()
            }
            StoredConversation(fields[0], fields[1], fields[2].toLongOrNull() ?: 0L, messages)
        }

    private fun b64(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    private fun unb64(value: String): String? = runCatching { String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8) }.getOrNull()

    companion object {
        private const val PREFS = "mypersonalai_conversation_history"
        private const val KEY_CHATS = "conversations"
        private const val MAX_CONVERSATIONS = 50
        private const val CHAT_SEPARATOR = "~CHAT~"
        private const val MESSAGE_SEPARATOR = "~MESSAGE~"
        private const val FIELD_SEPARATOR = "|"
    }
}

data class StoredConversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<Message>
)
