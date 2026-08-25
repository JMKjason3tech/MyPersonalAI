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
        "security" to listOf("security"),
        "network" to listOf("network", "internet", "mobile data")
    )

    private val deviceInfoTargets = linkedMapOf(
        "device_info" to listOf("device info", "device information", "phone info", "hardware"),
        "network" to listOf("network", "internet", "mobile data"),
        "speed_test" to listOf("speed test", "internet speed", "network speed", "download speed", "upload speed")
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

        val settingsTarget = findTarget(text, settingsTargets)
        val deviceInfoTarget = findTarget(text, deviceInfoTargets)
        val target = settingsTarget ?: deviceInfoTarget
        if (target == null) return Resolution(Intent.UNKNOWN, 0)

        val hasSettingsWord = text.contains("settings")
        val hasNavigationVerb = navigationVerbs.any(text::contains)
        val hasConfigurationVerb = configurationVerbs.any(text::contains)
        val hasInformationVerb = informationVerbs.any(text::contains)

        // Device-information targets are never opened as Settings merely
        // because a sentence contains "change" or "show". They represent
        // information/diagnostics and should be handled by DeviceInfoTool.
        if (settingsTarget == null && deviceInfoTarget != null) {
            if (hasInformationVerb || isSimpleTargetRequest(text, deviceInfoTarget, deviceInfoTargets)) {
                return Resolution(Intent.DEVICE_INFO, 90, deviceInfoTarget)
            }
            return Resolution(Intent.UNKNOWN, 0, deviceInfoTarget)
        }

        // Explicit configuration language means the user wants the place
        // where the setting can be changed, even when "settings" is omitted.
        if (hasConfigurationVerb) {
            return Resolution(Intent.OPEN_SETTINGS, 97, settingsTarget)
        }

        // Information language wins over generic words such as "show" when
        // the user is clearly asking for a status/value/details response.
        if (hasInformationVerb && !hasSettingsWord) {
            return Resolution(Intent.DEVICE_INFO, 90, settingsTarget)
        }

        if (hasSettingsWord || hasNavigationVerb) {
            return Resolution(Intent.OPEN_SETTINGS, 95, settingsTarget)
        }

        // A bare, short target such as "battery" remains a useful device-info
        // request. Arbitrary sentences containing a target do not fall back
        // to Device Info anymore.
        if (isSimpleTargetRequest(text, settingsTarget, settingsTargets)) {
            return Resolution(Intent.DEVICE_INFO, 80, settingsTarget)
        }

        return Resolution(Intent.UNKNOWN, 0, settingsTarget)
    }

    private fun findTarget(text: String, targets: Map<String, List<String>>): String? =
        targets.entries.firstOrNull { entry ->
            entry.value.any { phrase -> containsPhrase(text, phrase) }
        }?.key

    private fun isSimpleTargetRequest(
        text: String,
        target: String,
        targets: Map<String, List<String>>
    ): Boolean {
        val targetWords = targets[target].orEmpty()
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
