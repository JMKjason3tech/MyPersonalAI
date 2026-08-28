package com.jason.mypersonalai.agent

/**
 * Deterministic natural-language intent resolver for local Android actions.
 * Specific capability requests are resolved before broad device requests.
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
        "settings" to listOf("settings", "system settings", "android settings"),
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
        "device_info" to listOf("device info", "device information", "phone info", "hardware info", "hardware information"),
        "battery" to listOf("battery", "power"),
        "storage" to listOf("storage"),
        "wifi" to listOf("wi-fi", "wifi", "wireless"),
        "network" to listOf("network information", "network info", "network status", "internet information", "internet info", "internet status")
    )

    private val speedPhrases = listOf(
        "speed test", "internet speed", "network speed", "download speed", "upload speed",
        "test my internet speed", "test my network speed", "test network speed",
        "measure internet speed", "measure network speed", "check my internet speed",
        "check my network speed", "check download speed", "check upload speed"
    )

    private val navigationVerbs = listOf(
        "open", "access", "view", "display", "launch", "navigate", "go to", "take me to", "bring up", "get me to", "let me see"
    )
    private val configurationVerbs = listOf(
        "change", "modify", "configure", "adjust", "manage", "customize", "edit", "control", "set up", "setup", "alter", "update"
    )
    private val informationVerbs = listOf(
        "check", "tell me", "how much", "how many", "what is", "what's", "show me my", "give me", "status", "details", "information", "info", "report", "inspect", "diagnose"
    )

    fun resolve(input: String): Resolution {
        val text = normalize(input)
        if (text.isBlank()) return Resolution(Intent.UNKNOWN, 0)

        // Speed is deliberately checked first. This prevents a request such
        // as "check my network speed" from being consumed by generic network
        // status/device matching.
        if (speedPhrases.any { containsPhrase(text, it) }) {
            return Resolution(Intent.DEVICE_INFO, 99, "speed_test")
        }

        val specificSettingsTarget = findTarget(text, settingsTargets, excludeTarget = "settings")
        val settingsTarget = specificSettingsTarget ?: findTarget(text, settingsTargets)
        val deviceInfoTarget = findTarget(text, deviceInfoTargets)

        if (settingsTarget != null) {
            val hasSettingsWord = hasExplicitSettingsWord(text)
            val hasNavigationVerb = navigationVerbs.any(text::contains)
            val hasConfigurationVerb = configurationVerbs.any(text::contains)
            if (hasSettingsWord || hasNavigationVerb || hasConfigurationVerb) {
                return Resolution(Intent.OPEN_SETTINGS, if (settingsTarget == "settings") 98 else 95, settingsTarget)
            }
            if (settingsTarget == "settings" && text == "settings") {
                return Resolution(Intent.OPEN_SETTINGS, 98, "settings")
            }
        }

        if (deviceInfoTarget != null) {
            val hasInformationVerb = informationVerbs.any(text::contains)
            val isBareKnownTarget = deviceInfoTargets[deviceInfoTarget].orEmpty().any { text == it }
            val explicitDevicePhrase = deviceInfoTarget == "device_info"
            if (hasInformationVerb || isBareKnownTarget || explicitDevicePhrase) {
                return Resolution(Intent.DEVICE_INFO, 90, deviceInfoTarget)
            }
            return Resolution(Intent.UNKNOWN, 0, deviceInfoTarget)
        }

        return Resolution(Intent.UNKNOWN, 0)
    }

    private fun findTarget(text: String, targets: Map<String, List<String>>, excludeTarget: String? = null): String? =
        targets.entries.asSequence()
            .filter { it.key != excludeTarget }
            .filter { entry -> entry.value.any { phrase -> containsPhrase(text, phrase) } }
            .maxByOrNull { entry -> entry.value.filter { phrase -> containsPhrase(text, phrase) }.maxOfOrNull(String::length) ?: 0 }
            ?.key

    private fun hasExplicitSettingsWord(text: String): Boolean =
        text == "settings" || text.contains(" settings") || text.contains("settings ") || text.contains("settings screen") || text.contains("settings page")

    private fun containsPhrase(text: String, phrase: String): Boolean =
        Regex("(?:^|\\s)${Regex.escape(phrase)}(?:$|\\s)").containsMatchIn(text)

    private fun normalize(input: String): String = input.lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
