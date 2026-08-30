package com.jason.mypersonalai.agent

object IntentResolver {
    enum class Intent { OPEN_SETTINGS, DEVICE_CONTROL, OPEN_APP, CONTACTS, CALL_LOG, FILES, MEDIA, WIFI_BLUETOOTH, DEVICE_INFO, STOP_SPEAKING, UNKNOWN }
    data class Resolution(val intent: Intent, val confidence: Int, val target: String? = null)

    private val settingsTargets = linkedMapOf(
        "settings" to listOf("settings", "system settings", "android settings"),
        "wifi" to listOf("wi-fi", "wifi", "wireless"),
        "bluetooth" to listOf("bluetooth"), "battery" to listOf("battery", "power"),
        "display" to listOf("display", "screen"), "sound" to listOf("sound", "audio", "volume"),
        "location" to listOf("location", "gps"), "storage" to listOf("storage"),
        "apps" to listOf("app", "apps", "application", "applications"),
        "notifications" to listOf("notification", "notifications"), "accessibility" to listOf("accessibility"),
        "date_time" to listOf("date and time", "date", "time", "clock"), "security" to listOf("security"),
        "network" to listOf("network", "internet", "mobile data")
    )
    private val deviceInfoTargets = linkedMapOf(
        "device_info" to listOf("device info", "device information", "phone info", "hardware info", "hardware information"),
        "battery" to listOf("battery", "power"), "storage" to listOf("storage"),
        "wifi" to listOf("wi-fi", "wifi", "wireless"),
        "network" to listOf("network information", "network info", "network status", "internet information", "internet info", "internet status")
    )
    private val navigation = listOf("open", "access", "view", "display", "launch", "navigate", "go to", "take me to", "bring up", "get me to", "let me see", "start")
    private val configuration = listOf("change", "modify", "configure", "adjust", "manage", "customize", "edit", "control", "set up", "setup", "alter", "update", "increase", "decrease", "raise", "lower", "turn up", "turn down", "set")
    private val information = listOf("check", "tell me", "how much", "how many", "what is", "what's", "show me", "give me", "status", "details", "information", "info", "report", "inspect", "diagnose", "recent", "missed")
    private val connectivityActions = listOf("nearby", "scan", "discover", "connect", "turn on", "turn off", "switch on", "switch off", "enable", "disable", "is it on", "is it off", "status")

    fun resolve(input: String): Resolution {
        val text = normalize(input)
        if (text.isBlank()) return Resolution(Intent.UNKNOWN, 0)

        if (listOf("stop speaking", "be quiet", "stop talking", "silence", "cancel speaking").any { containsPhrase(text, it) })
            return Resolution(Intent.STOP_SPEAKING, 99, "stop")
        if (listOf("recent calls", "missed calls", "call history", "call log", "who called", "call records").any { text.contains(it) })
            return Resolution(Intent.CALL_LOG, 96)
        if (text.startsWith("call ") || text.startsWith("phone ") || listOf("contacts", "contact list", "find a contact", "who has this number").any { text.contains(it) })
            return Resolution(Intent.CONTACTS, 96, if (text.startsWith("call ")) "call" else null)
        if (listOf("files", "file manager", "documents", "downloads").any { containsPhrase(text, it) })
            return Resolution(Intent.FILES, 94)
        if (listOf("pictures", "photos", "music", "songs", "media").any { containsPhrase(text, it) })
            return Resolution(Intent.MEDIA, 94)

        // Real device controls must win over the Settings navigation target "sound"/"volume".
        if (isBrightnessCommand(text)) return Resolution(Intent.DEVICE_CONTROL, 99, "brightness")
        if (isVolumeCommand(text)) return Resolution(Intent.DEVICE_CONTROL, 99, "volume")

        // Connectivity changes/scans are actions; ordinary requests such as
        // "configure my wireless connection" remain Settings navigation.
        if ((text.contains("wifi") || text.contains("wi-fi") || text.contains("bluetooth")) && connectivityActions.any { text.contains(it) })
            return Resolution(Intent.WIFI_BLUETOOTH, 95, if (text.contains("bluetooth")) "bluetooth" else "wifi")

        if (listOf("speed test", "internet speed", "network speed", "download speed", "upload speed").any { text.contains(it) })
            return Resolution(Intent.DEVICE_INFO, 99, "speed_test")

        val settingsTarget = findTarget(text, settingsTargets, "settings") ?: findTarget(text, settingsTargets)
        if (settingsTarget != null) {
            val hasNavigation = navigation.any { text.contains(it) }
            val hasConfiguration = configuration.any { text.contains(it) }
            val hasExplicitSettings = text.contains("settings")
            val hasInformation = information.any { text.contains(it) }
            if ((hasExplicitSettings && !hasInformation) || hasNavigation || hasConfiguration)
                return Resolution(Intent.OPEN_SETTINGS, 95, settingsTarget)
        }

        val infoTarget = findTarget(text, deviceInfoTargets)
        if (infoTarget != null && (information.any { text.contains(it) } || deviceInfoTargets[infoTarget].orEmpty().any { text == it }))
            return Resolution(Intent.DEVICE_INFO, 90, infoTarget)

        if (navigation.any { text.contains(it) }) {
            val candidate = extractAppCandidate(text)
            if (!candidate.isNullOrBlank()) return Resolution(Intent.OPEN_APP, 80, candidate)
        }
        return Resolution(Intent.UNKNOWN, 0)
    }

    private fun isBrightnessCommand(text: String): Boolean {
        val hasBrightness = listOf("brightness", "screen brightness", "screen light").any { text.contains(it) }
        val relative = listOf("brighter", "dimmer", "brighten", "dim", "increase", "decrease", "raise", "lower", "turn up", "turn down", "turn volume up", "turn volume down", "turn sound up", "turn sound down", "set").any { text.contains(it) }
        return hasBrightness && relative || (text.contains("screen") && listOf("brighter", "dimmer", "brighten", "dim").any { text.contains(it) })
    }

    private fun isVolumeCommand(text: String): Boolean {
        val hasVolume = listOf("volume", "sound volume", "media volume").any { text.contains(it) }
        val relative = listOf("louder", "quieter", "softer", "mute", "unmute", "increase", "decrease", "raise", "lower", "turn up", "turn down", "turn volume up", "turn volume down", "turn sound up", "turn sound down", "set").any { text.contains(it) }
        return hasVolume && relative
    }

    private fun findTarget(text: String, targets: Map<String, List<String>>, exclude: String? = null) =
        targets.entries.asSequence().filter { it.key != exclude }
            .filter { e -> e.value.any { containsPhrase(text, it) } }
            .maxByOrNull { e -> e.value.filter { containsPhrase(text, it) }.maxOfOrNull(String::length) ?: 0 }?.key

    private fun extractAppCandidate(text: String): String? {
        val c = text.replace(Regex("^(please )?"), "")
            .replace(Regex("^(open|launch|start|access|view|display|navigate to|go to|take me to|bring up|get me to|let me see)\\s+"), "")
            .replace(Regex("\\s+(app|application)$"), "").trim()
        return c.takeIf { it.isNotBlank() && it !in setOf("an", "an app", "the app", "an application", "the application", "it", "that", "this", "same", "there") }
    }

    private fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9\\s-]"), " ").replace(Regex("\\s+"), " ").trim()
    private fun containsPhrase(text: String, phrase: String) = Regex("(?:^|\\s)${Regex.escape(phrase)}(?:$|\\s)").containsMatchIn(text)
}
