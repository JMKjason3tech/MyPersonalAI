package com.jason.mypersonalai.agent

object ContextualIntentResolver {
    data class ContextualResolution(
        val resolution: IntentResolver.Resolution,
        val usedConversationContext: Boolean
    )

    fun resolve(input: String, history: List<Message>): ContextualResolution {
        val direct = IntentResolver.resolve(input)
        if (direct.intent != IntentResolver.Intent.UNKNOWN) return ContextualResolution(direct, false)
        if (!isFollowUp(input)) return ContextualResolution(direct, false)

        val previous = history.asReversed().asSequence()
            .filter { it.role == Role.USER }
            .map { it.text }
            .map(IntentResolver::resolve)
            .firstOrNull { it.intent != IntentResolver.Intent.UNKNOWN }
            ?: return ContextualResolution(direct, false)

        val contextual = when (previous.intent) {
            IntentResolver.Intent.OPEN_SETTINGS -> if (!isSettingsActionFollowUp(input)) null else previous.copy(confidence = minOf(previous.confidence, 88))
            IntentResolver.Intent.DEVICE_INFO -> if (!isInformationFollowUp(input)) null else previous.copy(confidence = minOf(previous.confidence, 88))
            IntentResolver.Intent.UNKNOWN -> null
        }
        return if (contextual == null) ContextualResolution(direct, false) else ContextualResolution(contextual, true)
    }

    fun canonicalCommand(resolution: IntentResolver.Resolution): String? = when (resolution.intent) {
        IntentResolver.Intent.OPEN_SETTINGS -> resolution.target?.let { target ->
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
        IntentResolver.Intent.DEVICE_INFO -> when (resolution.target) {
            "speed_test" -> "speed test"
            else -> resolution.target
        }
        IntentResolver.Intent.UNKNOWN -> null
    }

    private fun isFollowUp(input: String): Boolean {
        val text = normalize(input)
        if (text.isBlank()) return false
        val referenceWords = listOf("it", "that", "this", "same", "again", "previous", "there")
        val followUpPhrases = listOf("do that", "do it", "open it", "open that", "check it", "check that", "show it", "show that", "view it", "view that", "try again", "do it again", "do that again", "repeat that", "repeat it", "tell me more about it", "tell me more about that", "what about it", "what about that", "what about this")
        return followUpPhrases.any { text == it || text.startsWith("$it ") } ||
            (referenceWords.any { word -> containsWord(text, word) } && listOf("open", "access", "view", "show", "check", "tell", "give", "repeat", "do", "try", "again", "what", "same").any { text.contains(it) })
    }

    private fun isSettingsActionFollowUp(input: String): Boolean = normalize(input).let { text -> listOf("open", "access", "view", "show", "go", "take me", "bring up", "do", "try", "repeat", "again").any(text::contains) }

    private fun isInformationFollowUp(input: String): Boolean = normalize(input).let { text -> listOf("check", "tell", "show", "give", "what", "how", "details", "information", "info", "status", "repeat", "again", "do").any(text::contains) }

    private fun containsWord(text: String, word: String): Boolean = Regex("(?:^|\\s)${Regex.escape(word)}(?:$|\\s)").containsMatchIn(text)
    private fun normalize(input: String): String = input.lowercase().replace(Regex("[^a-z0-9\\s-]"), " ").replace(Regex("\\s+"), " ").trim()
}
