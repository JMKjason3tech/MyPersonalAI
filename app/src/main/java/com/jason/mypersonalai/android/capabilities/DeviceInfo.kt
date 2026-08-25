package com.jason.mypersonalai.android.capabilities

/**
 * Read-only Android device capabilities.
 *
 * These interfaces deliberately keep Android framework types out of the
 * tool layer. Real implementations live in android.adapters, which keeps
 * DeviceInfoTool deterministic and unit-testable with simple fakes.
 */

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Float? = null,
    val voltageMillivolts: Int? = null,
    val health: String = "Unknown",
    val technology: String? = null,
    val chargeSource: String = "Unknown"
)

data class NetworkInfo(
    val isConnected: Boolean,
    /** e.g. "wifi", "cellular", "ethernet", "other", or "unknown". */
    val type: String,
    val ssid: String? = null,
    /** Human-readable reason when Android cannot provide the Wi-Fi SSID. */
    val ssidUnavailableReason: String? = null,
    val linkSpeedMbps: Int? = null,
    val frequencyMhz: Int? = null,
    /** Android's current estimated downstream capability, not a speed-test result. */
    val downstreamMbps: Double? = null,
    /** Android's current estimated upstream capability, not a speed-test result. */
    val upstreamMbps: Double? = null
)

data class StorageInfo(
    val availableBytes: Long,
    val totalBytes: Long
)

data class NetworkSpeedInfo(
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val latencyMs: Long? = null,
    val endpoint: String? = null,
    val errorMessage: String? = null
)

data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val hardware: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildId: String,
    val securityPatch: String?,
    val kernelVersion: String?,
    val supportedAbis: List<String>,
    val cpuCores: Int,
    val totalRamBytes: Long?,
    val availableRamBytes: Long?,
    val screenWidthPixels: Int?,
    val screenHeightPixels: Int?,
    val densityDpi: Int?,
    val refreshRateHz: Float?,
    val socManufacturer: String?,
    val socModel: String?,
    val uptimeMillis: Long,
    val hasCamera: Boolean,
    val hasFlash: Boolean,
    val hasGps: Boolean,
    val hasBluetooth: Boolean,
    val hasWifi: Boolean,
    val hasNfc: Boolean,
    val hasBiometric: Boolean,
    val hasUsbHost: Boolean,
    val sensorCount: Int
)

interface BatteryInfoProvider {
    fun getBatteryInfo(): BatteryInfo
}

interface NetworkInfoProvider {
    fun getNetworkInfo(): NetworkInfo
}

interface StorageInfoProvider {
    fun getStorageInfo(): StorageInfo
}

interface NetworkSpeedTestProvider {
    suspend fun runSpeedTest(): NetworkSpeedInfo
}

interface DeviceInfoProvider {
    fun getDeviceInfo(): DeviceInfo
}
