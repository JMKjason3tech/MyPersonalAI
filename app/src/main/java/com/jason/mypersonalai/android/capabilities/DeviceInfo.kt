package com.jason.mypersonalai.android.capabilities

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
    val type: String,
    val ssid: String? = null,
    val ssidUnavailableReason: String? = null,
    val linkSpeedMbps: Int? = null,
    val frequencyMhz: Int? = null,
    val downstreamMbps: Double? = null,
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

interface BatteryInfoProvider { fun getBatteryInfo(): BatteryInfo }
interface NetworkInfoProvider { fun getNetworkInfo(): NetworkInfo }
interface StorageInfoProvider { fun getStorageInfo(): StorageInfo }
interface NetworkSpeedTestProvider { suspend fun runSpeedTest(): NetworkSpeedInfo }
interface DeviceInfoProvider { fun getDeviceInfo(): DeviceInfo }
