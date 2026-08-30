package com.jason.mypersonalai.agent

object ContextualIntentResolver {
    data class ContextualResolution(val resolution: IntentResolver.Resolution, val usedConversationContext: Boolean)

    fun resolve(input: String, history: List<Message>): ContextualResolution {
        val direct = IntentResolver.resolve(input)
        if (direct.intent != IntentResolver.Intent.UNKNOWN) return ContextualResolution(direct, false)
        if (!isFollowUp(input)) return ContextualResolution(direct, false)
        val previous = history.asReversed().asSequence().filter { it.role == Role.USER }.map { it.text }.map(IntentResolver::resolve)
            .firstOrNull { it.intent != IntentResolver.Intent.UNKNOWN } ?: return ContextualResolution(direct, false)
        val contextual = when (previous.intent) {
            IntentResolver.Intent.OPEN_SETTINGS -> if (isInformationFollowUp(input)) null else if (isSettingsActionFollowUp(input)) previous.copy(confidence = minOf(previous.confidence, 88)) else null
            IntentResolver.Intent.DEVICE_INFO -> if (isInformationFollowUp(input)) previous.copy(confidence = minOf(previous.confidence, 88)) else null
            IntentResolver.Intent.DEVICE_CONTROL, IntentResolver.Intent.OPEN_APP, IntentResolver.Intent.CONTACTS, IntentResolver.Intent.CALL_LOG, IntentResolver.Intent.FILES, IntentResolver.Intent.MEDIA, IntentResolver.Intent.WIFI_BLUETOOTH, IntentResolver.Intent.STOP_SPEAKING -> previous.copy(confidence = minOf(previous.confidence, 88))
            IntentResolver.Intent.UNKNOWN -> null
        }
        return if (contextual == null) ContextualResolution(direct, false) else ContextualResolution(contextual, true)
    }

    fun canonicalCommand(resolution: IntentResolver.Resolution): String? = when (resolution.intent) {
        IntentResolver.Intent.OPEN_SETTINGS -> resolution.target?.let {
            when (it) {
                "settings" -> "open settings"; "notifications" -> "open notification settings"; "wifi" -> "open wifi settings"; "bluetooth" -> "open bluetooth settings"; "battery" -> "open battery settings"; "display" -> "open display settings"; "sound" -> "open sound settings"; "location" -> "open location settings"; "storage" -> "open storage settings"; "apps" -> "open app settings"; "accessibility" -> "open accessibility settings"; "date_time" -> "open date and time settings"; "security" -> "open security settings"; "network" -> "open network settings"; else -> null
            }
        }
        IntentResolver.Intent.DEVICE_INFO -> if (resolution.target == "speed_test") "speed test" else resolution.target
        IntentResolver.Intent.DEVICE_CONTROL -> resolution.target
        IntentResolver.Intent.OPEN_APP -> resolution.target
        IntentResolver.Intent.CONTACTS -> "contacts"
        IntentResolver.Intent.CALL_LOG -> "call log"
        IntentResolver.Intent.FILES -> "files"
        IntentResolver.Intent.MEDIA -> "media"
        IntentResolver.Intent.WIFI_BLUETOOTH -> resolution.target
        IntentResolver.Intent.STOP_SPEAKING -> "stop speaking"
        IntentResolver.Intent.UNKNOWN -> null
    }

    private fun isFollowUp(input: String): Boolean {
        val text = normalize(input)
        if (text.isBlank()) return false
        val referenceWords = listOf("it", "that", "this", "same", "again", "previous", "there")
        val followUpPhrases = listOf("do that", "do it", "open it", "open that", "check it", "check that", "show it", "show that", "view it", "view that", "try again", "do it again", "do that again", "repeat that", "repeat it", "tell me more about it", "tell me more about that", "what about it", "what about that", "what about this")
        return followUpPhrases.any { text == it || text.startsWith("$it ") } || (referenceWords.any { containsWord(text, it) } && listOf("open", "access", "view", "show", "check", "tell", "give", "repeat", "do", "try", "again", "what", "same", "adjust", "change").any { text.contains(it) })
    }
    private fun isSettingsActionFollowUp(input: String) = normalize(input).let { text ->
        val action = listOf("open", "access", "view", "go", "take me", "bring up", "do that", "do it", "try again", "repeat that", "repeat it").any(text::contains)
        action && !isInformationFollowUp(text)
    }
    private fun isInformationFollowUp(input: String) = normalize(input).let { listOf("check", "tell", "show", "give", "what", "how", "details", "information", "info", "status", "repeat", "again", "do").any(it::contains) }
    private fun containsWord(text: String, word: String) = Regex("(?:^|\\s)${Regex.escape(word)}(?:$|\\s)").containsMatchIn(text)
    private fun normalize(input: String) = input.lowercase().replace(Regex("[^a-z0-9\\s-]"), " ").replace(Regex("\\s+"), " ").trim()
}
