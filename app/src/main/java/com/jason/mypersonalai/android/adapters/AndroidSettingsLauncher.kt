package com.jason.mypersonalai.android.adapters

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.jason.mypersonalai.android.capabilities.AndroidSettingsLauncher
import com.jason.mypersonalai.android.capabilities.SettingsLaunchResult

/** Real Android implementation of the Settings launcher capability. */
class AndroidSettingsLauncherImpl(
    private val context: Context
) : AndroidSettingsLauncher {

    override fun open(request: String): SettingsLaunchResult {
        val normalized = request.trim().lowercase()
        val candidates = destinationCandidates(normalized)

        for ((action, destination) in candidates) {
            if (tryStart(action)) {
                return SettingsLaunchResult(
                    success = true,
                    destination = destination,
                    message = "Opening $destination.",
                    usedFallback = false
                )
            }
        }

        if (tryStart(Settings.ACTION_SETTINGS)) {
            return SettingsLaunchResult(
                success = true,
                destination = "Android Settings",
                message = "I couldn't open the specific Settings page, so I'm opening Android Settings instead.",
                usedFallback = true
            )
        }

        return SettingsLaunchResult(
            success = false,
            destination = "Android Settings",
            message = "I couldn't open Android Settings on this device."
        )
    }

    private fun tryStart(action: String): Boolean {
        return runCatching {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    private fun destinationCandidates(request: String): List<Pair<String, String>> = when {
        request == "settings" ||
            request.contains("system settings") ||
            request.contains("android settings") -> listOf(
            Settings.ACTION_SETTINGS to "Android Settings"
        )
        request.contains("wifi") || request.contains("wi-fi") || request.contains("wireless") -> listOf(
            Settings.ACTION_WIFI_SETTINGS to "Wi-Fi settings",
            Settings.ACTION_WIRELESS_SETTINGS to "network settings"
        )
        request.contains("bluetooth") -> listOf(
            Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth settings"
        )
        request.contains("network") || request.contains("internet") || request.contains("mobile data") -> listOf(
            Settings.ACTION_WIRELESS_SETTINGS to "network settings",
            Settings.ACTION_NETWORK_OPERATOR_SETTINGS to "mobile network settings"
        )
        request.contains("battery") || request.contains("power") -> listOf(
            Settings.ACTION_BATTERY_SAVER_SETTINGS to "battery settings"
        )
        request.contains("display") || request.contains("screen") -> listOf(
            Settings.ACTION_DISPLAY_SETTINGS to "display settings"
        )
        request.contains("sound") || request.contains("audio") || request.contains("volume") -> listOf(
            Settings.ACTION_SOUND_SETTINGS to "sound settings"
        )
        request.contains("location") || request.contains("gps") -> listOf(
            Settings.ACTION_LOCATION_SOURCE_SETTINGS to "location settings"
        )
        request.contains("storage") -> listOf(
            Settings.ACTION_INTERNAL_STORAGE_SETTINGS to "storage settings"
        )
        request.contains("app") || request.contains("application") -> listOf(
            Settings.ACTION_APPLICATION_SETTINGS to "app settings"
        )
        request.contains("notification") -> notificationSettingsCandidates()
        request.contains("accessibility") -> listOf(
            Settings.ACTION_ACCESSIBILITY_SETTINGS to "accessibility settings"
        )
        request.contains("date") || request.contains("time") || request.contains("clock") -> listOf(
            Settings.ACTION_DATE_SETTINGS to "date and time settings"
        )
        request.contains("security") -> listOf(
            Settings.ACTION_SECURITY_SETTINGS to "security settings"
        )
        else -> emptyList()
    }

    private fun notificationSettingsCandidates(): List<Pair<String, String>> {
        // Prefer the API 33+ all-app notification destination. This avoids
        // accidentally opening MyPersonalAI's own notification page and
        // keeps the request focused on notification settings rather than the
        // generic Android Settings home screen.
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS to "notification settings")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add("android.settings.NOTIFICATION_SETTINGS" to "notification settings")
            }
        }
    }
}
