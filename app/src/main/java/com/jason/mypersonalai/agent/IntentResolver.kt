package com.jason.mypersonalai.agent

/**
 * Lightweight, deterministic natural-language intent resolver.
 *
 * The resolver deliberately separates:
 * - an explicit Settings request (open/configure a Settings destination),
 * - an information request (read device/network state), and
 * - unknown/ambiguous text.
 *
 * Common target words such as "battery", "storage" and "wifi" are not
 * sufficient to open Settings. When they are used by themselves they mean
 * "tell me about/check this device information". A Settings action requires
 * an explicit Settings/navigation/configuration signal.
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
        "device_info" to listOf(
            "device info",
            "device information",
            "phone info",
            "hardware info",
            "hardware information"
        ),
        "battery" to listOf("battery", "power"),
        "storage" to listOf("storage"),
        "wifi" to listOf("wi-fi", "wifi", "wireless"),
        "network" to listOf(
            "network information",
            "network info",
            "network status",
            "internet information",
            "internet info",
            "internet status"
        ),
        "speed_test" to listOf(
            "speed test",
            "internet speed",
            "network speed",
            "download speed",
            "upload speed"
        )
    )

    private val navigationVerbs = listOf(
        "open", "access", "view", "display", "launch", "navigate",
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

        // Resolve a specific Settings destination before the generic word
        // "settings". Otherwise phrases such as "open notification settings"
        // are incorrectly labelled with the generic target "settings".
        val specificSettingsTarget = findTarget(text, settingsTargets, excludeTarget = "settings")
        val settingsTarget = specificSettingsTarget ?: findTarget(text, settingsTargets)
        val deviceInfoTarget = findTarget(text, deviceInfoTargets)

        // Explicit Settings language always wins when the user names a
        // Settings destination. This prevents "open battery settings" from
        // being interpreted as a device-information request.
        if (settingsTarget != null) {
            val hasSettingsWord = hasExplicitSettingsWord(text)
            val hasNavigationVerb = navigationVerbs.any(text::contains)
            val hasConfigurationVerb = configurationVerbs.any(text::contains)

            if (hasSettingsWord || hasNavigationVerb || hasConfigurationVerb) {
                return Resolution(
                    Intent.OPEN_SETTINGS,
                    if (settingsTarget == "settings") 98 else 95,
                    settingsTarget
                )
            }

            // "settings" by itself is an unambiguous request for the main
            // Android Settings screen.
            if (settingsTarget == "settings" && text == "settings") {
                return Resolution(Intent.OPEN_SETTINGS, 98, "settings")
            }
        }

        if (deviceInfoTarget != null) {
            val hasInformationVerb = informationVerbs.any(text::contains)
            val isBareKnownDeviceTarget = isBareKnownDeviceTarget(text, deviceInfoTarget)
            val explicitDevicePhrase = deviceInfoTarget == "device_info" ||
                deviceInfoTarget == "speed_test"

            if (hasInformationVerb || isBareKnownDeviceTarget || explicitDevicePhrase) {
                return Resolution(Intent.DEVICE_INFO, 90, deviceInfoTarget)
            }

            // A target embedded in an otherwise unrelated sentence is not an
            // intent. For example, "hello battery" and "random network
            // sentence" must remain UNKNOWN.
            return Resolution(Intent.UNKNOWN, 0, deviceInfoTarget)
        }

        return Resolution(Intent.UNKNOWN, 0)
    }

    private fun findTarget(
        text: String,
        targets: Map<String, List<String>>,
        excludeTarget: String? = null
    ): String? = targets.entries
        .asSequence()
        .filter { it.key != excludeTarget }
        .filter { entry -> entry.value.any { phrase -> containsPhrase(text, phrase) } }
        .maxByOrNull { entry ->
            // Prefer the most specific matching phrase, rather than allowing
            // the generic "settings" target to mask notification/Wi-Fi/etc.
            entry.value.filter { phrase -> containsPhrase(text, phrase) }
                .maxOfOrNull(String::length) ?: 0
        }
        ?.key

    private fun isBareKnownDeviceTarget(text: String, target: String): Boolean {
        val phrases = deviceInfoTargets[target].orEmpty()
        return phrases.any { phrase -> text == phrase }
    }

    private fun hasExplicitSettingsWord(text: String): Boolean =
        text == "settings" ||
            text.contains(" settings") ||
            text.contains("settings ") ||
            text.contains("settings screen") ||
            text.contains("settings page")

    private fun containsPhrase(text: String, phrase: String): Boolean =
        Regex("(?:^|\\s)${Regex.escape(phrase)}(?:$|\\s)").containsMatchIn(text)

    private fun normalize(input: String): String = input
        .lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
