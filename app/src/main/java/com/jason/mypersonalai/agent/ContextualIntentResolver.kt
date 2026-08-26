package com.jason.mypersonalai.agent

/**
 * Adds a small, deterministic conversation-context layer on top of
 * [IntentResolver].
 *
 * The resolver still handles explicit commands first. Context is only used
 * for short follow-up phrases that clearly refer to a previous actionable
 * turn, such as "open it", "check that again", or "tell me more about it".
 * Unrelated text is never silently attached to an older intent.
 */
object ContextualIntentResolver {

    data class ContextualResolution(
        val resolution: IntentResolver.Resolution,
        val usedConversationContext: Boolean
    )

    fun resolve(input: String, history: List<Message>): ContextualResolution {
        val direct = IntentResolver.resolve(input)
        if (direct.intent != IntentResolver.Intent.UNKNOWN) {
            return ContextualResolution(direct, usedConversationContext = false)
        }

        if (!isFollowUp(input)) {
            return ContextualResolution(direct, usedConversationContext = false)
        }

        val previous = history.asReversed()
            .asSequence()
            .filter { it.role == Role.USER }
            .map { it.text }
            .map(IntentResolver::resolve)
            .firstOrNull { it.intent != IntentResolver.Intent.UNKNOWN }
            ?: return ContextualResolution(direct, usedConversationContext = false)

        val contextual = when (previous.intent) {
            IntentResolver.Intent.OPEN_SETTINGS -> {
                if (!isSettingsActionFollowUp(input)) null
                else previous.copy(confidence = minOf(previous.confidence, 88))
            }
            IntentResolver.Intent.DEVICE_INFO -> {
                if (!isInformationFollowUp(input)) null
                else previous.copy(confidence = minOf(previous.confidence, 88))
            }
            IntentResolver.Intent.UNKNOWN -> null
        }

        return if (contextual == null) {
            ContextualResolution(direct, usedConversationContext = false)
        } else {
            ContextualResolution(contextual, usedConversationContext = true)
        }
    }

    /**
     * Converts a contextual resolution into a canonical command understood by
     * the existing tools. This prevents the context layer from becoming a
     * second execution/routing system.
     */
    fun canonicalCommand(resolution: IntentResolver.Resolution): String? =
        when (resolution.intent) {
            IntentResolver.Intent.OPEN_SETTINGS ->
                resolution.target?.let { target ->
                    when (target) {
                        "settings" -> "open settings"
                        "notifications" -> "open notification settings"
                        "wifi" -> "open wifi settings"
                        "bluetooth" -> "open bluetooth settings"
                        "battery" -> "open battery settings"
                        "display" -> "open display settings"
                        "sound" -> "open sound settings"
                        "location" -> "open location settings"
                        "storage" -> "open storage settings"
                        "apps" -> "open app settings"
                        "accessibility" -> "open accessibility settings"
                        "date_time" -> "open date and time settings"
                        "security" -> "open security settings"
                        "network" -> "open network settings"
                        else -> null
                    }
                }
            IntentResolver.Intent.DEVICE_INFO -> resolution.target
            IntentResolver.Intent.UNKNOWN -> null
        }

    private fun isFollowUp(input: String): Boolean {
        val text = normalize(input)
        if (text.isBlank()) return false

        val referenceWords = listOf(
            "it", "that", "this", "same", "again", "previous", "there"
        )
        val followUpPhrases = listOf(
            "do that", "do it", "open it", "open that", "check it", "check that",
            "show it", "show that", "view it", "view that", "try again",
            "do it again", "do that again", "repeat that", "repeat it",
            "tell me more about it", "tell me more about that", "what about it",
            "what about that", "what about this"
        )

        return followUpPhrases.any { text == it || text.startsWith("$it ") } ||
            referenceWords.any { word -> containsWord(text, word) } &&
            listOf(
                "open", "access", "view", "show", "check", "tell", "give",
                "repeat", "do", "try", "again", "what", "same"
            ).any { text.contains(it) }
    }

    private fun isSettingsActionFollowUp(input: String): Boolean {
        val text = normalize(input)
        return listOf(
            "open", "access", "view", "show", "go", "take me", "bring up",
            "do", "try", "repeat", "again"
        ).any(text::contains)
    }

    private fun isInformationFollowUp(input: String): Boolean {
        val text = normalize(input)
        return listOf(
            "check", "tell", "show", "give", "what", "how", "details",
            "information", "info", "status", "repeat", "again", "do"
        ).any(text::contains)
    }

    private fun containsWord(text: String, word: String): Boolean =
        Regex("(?:^|\\s)${Regex.escape(word)}(?:$|\\s)").containsMatchIn(text)

    private fun normalize(input: String): String = input
        .lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
