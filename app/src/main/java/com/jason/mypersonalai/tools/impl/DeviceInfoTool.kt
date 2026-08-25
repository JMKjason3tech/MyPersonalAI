package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.android.capabilities.BatteryInfoProvider
import com.jason.mypersonalai.android.capabilities.DeviceInfoProvider
import com.jason.mypersonalai.android.capabilities.NetworkInfoProvider
import com.jason.mypersonalai.android.capabilities.NetworkSpeedInfo
import com.jason.mypersonalai.android.capabilities.NetworkSpeedTestProvider
import com.jason.mypersonalai.android.capabilities.StorageInfoProvider
import com.jason.mypersonalai.tools.RiskLevel
import com.jason.mypersonalai.tools.Tool
import com.jason.mypersonalai.tools.ToolError
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput

/**
 * Read-only device diagnostics.
 *
 * Existing Milestone 5 commands remain compatible: battery status,
 * network status, storage status and device info. The important semantic
 * change is that "device info" now means the actual hardware/software
 * profile; it no longer dumps battery/network/storage together.
 */
class DeviceInfoTool(
    private val batteryInfoProvider: BatteryInfoProvider,
    private val networkInfoProvider: NetworkInfoProvider,
    private val storageInfoProvider: StorageInfoProvider,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val networkSpeedTestProvider: NetworkSpeedTestProvider
) : Tool {
    override val id: String = "device_info"
    override val description: String = "Reads detailed device, battery, network, and storage information."
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(input: ToolInput): ToolExecutionResult {
        val query = input.raw.trim().lowercase()

        return try {
            val output = when {
                query.contains("battery") -> formatBattery()
                isSpeedQuery(query) -> formatSpeedTest(networkSpeedTestProvider.runSpeedTest())
                query.contains("network") || query.contains("wifi") || query.contains("wi-fi") -> formatNetwork()
                query.contains("storage") -> formatStorage()
                query.contains("device") || query.contains("phone") || query.contains("hardware") -> formatDevice()
                else -> formatDevice()
            }
            ToolExecutionResult.Success(output = output)
        } catch (e: Exception) {
            ToolExecutionResult.Failure(
                ToolError(
                    displayMessage = "Device info error: could not read device status.",
                    cause = e
                )
            )
        }
    }

    private fun isSpeedQuery(query: String): Boolean = listOf(
        "speed test", "internet speed", "network speed", "download speed", "upload speed"
    ).any { query.contains(it) }

    private fun formatBattery(): String {
        val info = batteryInfoProvider.getBatteryInfo()
        val lines = mutableListOf("Battery: ${info.percentage}% (${info.chargeSource})")
        lines += "Health: ${info.health}"
        info.temperatureCelsius?.let { lines += "Temperature: %.1f°C".format(it) }
        info.voltageMillivolts?.let { lines += "Voltage: $it mV" }
        info.technology?.let { lines += "Technology: $it" }
        return lines.joinToString("\n")
    }

    private fun formatNetwork(): String {
        val info = networkInfoProvider.getNetworkInfo()
        if (!info.isConnected) return "Network: disconnected"

        val lines = mutableListOf("Network: connected (${info.type})")
        if (info.type == "wifi") {
            lines += if (info.ssid != null) {
                "WiFi name: ${info.ssid}"
            } else {
                "WiFi name: unavailable (location permission not granted)"
            }
            info.linkSpeedMbps?.let { lines += "WiFi link speed: $it Mbps" }
            info.frequencyMhz?.let { lines += "Frequency: $it MHz" }
        }
        info.downstreamMbps?.let { lines += "Estimated download: $it Mbps" }
        info.upstreamMbps?.let { lines += "Estimated upload: $it Mbps" }
        lines += "For measured Internet speed, ask: \"speed test\"."
        return lines.joinToString("\n")
    }

    private fun formatSpeedTest(info: NetworkSpeedInfo): String {
        if (info.errorMessage != null) {
            return "Network speed test unavailable: ${info.errorMessage}"
        }
        val lines = mutableListOf("Internet speed test")
        info.downloadMbps?.let { lines += "Download: %.1f Mbps".format(it) }
        info.uploadMbps?.let { lines += "Upload: %.1f Mbps".format(it) }
        info.latencyMs?.let { lines += "Latency: $it ms" }
        info.endpoint?.let { lines += "Test endpoint: $it" }
        return lines.joinToString("\n")
    }

    private fun formatStorage(): String {
        val info = storageInfoProvider.getStorageInfo()
        val totalGb = info.totalBytes / (1024.0 * 1024.0 * 1024.0)
        val freeGb = info.availableBytes / (1024.0 * 1024.0 * 1024.0)
        val usedBytes = (info.totalBytes - info.availableBytes).coerceAtLeast(0L)
        val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
        val usagePercent = if (info.totalBytes > 0) usedBytes * 100.0 / info.totalBytes else 0.0
        return listOf(
            "Storage: internal",
            "Total: %.1f GB".format(totalGb),
            "Used: %.1f GB".format(usedGb),
            "Free: %.1f GB".format(freeGb),
            "Usage: %.1f%%".format(usagePercent)
        ).joinToString("\n")
    }

    private fun formatDevice(): String {
        val info = deviceInfoProvider.getDeviceInfo()
        val lines = mutableListOf(
            "Device information",
            "Manufacturer: ${info.manufacturer}",
            "Brand: ${info.brand}",
            "Model: ${info.model}",
            "Device: ${info.device}",
            "Product: ${info.product}",
            "Board: ${info.board}",
            "Hardware: ${info.hardware}",
            "Android: ${info.androidVersion}",
            "API level: ${info.apiLevel}",
            "Build ID: ${info.buildId}",
            "CPU cores: ${info.cpuCores}",
            "Supported ABIs: ${info.supportedAbis.joinToString(", ").ifBlank { "Unknown" }}"
        )
        info.socManufacturer?.let { lines += "SoC manufacturer: $it" }
        info.socModel?.let { lines += "SoC model: $it" }
        info.securityPatch?.let { lines += "Security patch: $it" }
        info.kernelVersion?.let { lines += "Kernel: $it" }
        info.totalRamBytes?.let { lines += "RAM total: ${formatGb(it)} GB" }
        info.availableRamBytes?.let { lines += "RAM available: ${formatGb(it)} GB" }
        if (info.screenWidthPixels != null && info.screenHeightPixels != null) {
            lines += "Display: ${info.screenWidthPixels} × ${info.screenHeightPixels} pixels"
        }
        info.densityDpi?.let { lines += "Density: ${it} dpi" }
        info.refreshRateHz?.let { lines += "Refresh rate: %.1f Hz".format(it) }
        lines += "Uptime: ${formatUptime(info.uptimeMillis)}"
        lines += "Hardware capabilities:"
        lines += "Camera: ${yesNo(info.hasCamera)}"
        lines += "Flash: ${yesNo(info.hasFlash)}"
        lines += "GPS: ${yesNo(info.hasGps)}"
        lines += "Bluetooth: ${yesNo(info.hasBluetooth)}"
        lines += "WiFi: ${yesNo(info.hasWifi)}"
        lines += "NFC: ${yesNo(info.hasNfc)}"
        lines += "Biometric hardware: ${yesNo(info.hasBiometric)}"
        lines += "USB host: ${yesNo(info.hasUsbHost)}"
        lines += "Sensors: ${info.sensorCount}"
        return lines.joinToString("\n")
    }

    private fun formatGb(bytes: Long): String = "%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))

    private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"

    private fun formatUptime(millis: Long): String {
        val totalMinutes = millis / 60_000L
        val days = totalMinutes / 1_440L
        val hours = (totalMinutes % 1_440L) / 60L
        val minutes = totalMinutes % 60L
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
