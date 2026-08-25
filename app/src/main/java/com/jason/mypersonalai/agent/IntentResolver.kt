package com.jason.mypersonalai.agent

/**
 * Lightweight, deterministic natural-language intent resolver.
 *
 * This is intentionally local and dependency-free. It separates the user's
 * action from the target instead of requiring one exact command phrase.
 */
object IntentResolver {

    enum class Intent {
        OPEN_SETTINGS,
        DEVICE_INFO,
        UNKNOWN
    }

    data class Resolution(
        val intent: Intent,
        val confidence: Int,
        val target: String? = null
    )

    private val settingsTargets = linkedMapOf(
        "wifi" to listOf("wi-fi", "wifi", "wireless"),
        "bluetooth" to listOf("bluetooth"),
        "battery" to listOf("battery", "power"),
        "display" to listOf("display", "screen"),
        "sound" to listOf("sound", "audio", "volume"),
        "location" to listOf("location", "gps"),
        "storage" to listOf("storage"),
        "apps" to listOf("app", "apps", "application", "applications"),
        "notifications" to listOf("notification", "notifications"),
        "accessibility" to listOf("accessibility"),
        "date_time" to listOf("date and time", "date", "time", "clock"),
        "security" to listOf("security")
    )

    private val navigationVerbs = listOf(
        "open", "access", "show", "view", "display", "launch", "navigate",
        "go to", "take me to", "bring up", "get me to", "let me see"
    )

    private val configurationVerbs = listOf(
        "change", "modify", "configure", "adjust", "manage", "customize",
        "edit", "control", "set up", "setup", "alter", "update"
    )

    private val informationVerbs = listOf(
        "check", "tell me", "how much", "how many", "what is", "what's",
        "show me my", "give me", "status", "details", "information", "info",
        "report", "inspect", "diagnose"
    )

    fun resolve(input: String): Resolution {
        val text = normalize(input)
        if (text.isBlank()) return Resolution(Intent.UNKNOWN, 0)

        val settingsTarget = findTarget(text)
        if (settingsTarget != null) {
            val hasNavigationVerb = navigationVerbs.any(text::contains)
            val hasConfigurationVerb = configurationVerbs.any(text::contains)
            val hasInformationVerb = informationVerbs.any(text::contains)

            if (hasNavigationVerb || hasConfigurationVerb) {
                val confidence = if (hasNavigationVerb && hasConfigurationVerb) 98 else 95
                return Resolution(Intent.OPEN_SETTINGS, confidence, settingsTarget)
            }

            // A bare, short target such as "battery" or "wifi settings" is
            // still useful, but arbitrary sentences containing a target are
            // no longer allowed to fall through to Device Info.
            if (!hasInformationVerb && isSimpleTargetRequest(text, settingsTarget)) {
                return Resolution(Intent.DEVICE_INFO, 80, settingsTarget)
            }

            if (hasInformationVerb) {
                return Resolution(Intent.DEVICE_INFO, 90, settingsTarget)
            }
        }

        return Resolution(Intent.UNKNOWN, 0)
    }

    private fun findTarget(text: String): String? = settingsTargets.entries.firstOrNull { entry ->
        entry.value.any { phrase -> containsPhrase(text, phrase) }
    }?.key

    private fun isSimpleTargetRequest(text: String, target: String): Boolean {
        val targetWords = settingsTargets[target].orEmpty()
        val cleaned = text.removeSuffix("settings").trim()
        return cleaned.split(" ").size <= 3 && targetWords.any { containsPhrase(cleaned, it) }
    }

    private fun containsPhrase(text: String, phrase: String): Boolean {
        return Regex("(?:^|\\s)${Regex.escape(phrase)}(?:$|\\s)").containsMatchIn(text)
    }

    private fun normalize(input: String): String = input
        .lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
