package com.jason.mypersonalai.android.adapters

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import android.hardware.SensorManager
import com.jason.mypersonalai.android.capabilities.DeviceInfo
import com.jason.mypersonalai.android.capabilities.DeviceInfoProvider
import java.io.File

/** Real Android implementation of detailed, read-only device information. */
class AndroidDeviceInfoProvider(
    private val context: Context
) : DeviceInfoProvider {

    override fun getDeviceInfo(): DeviceInfo {
        val packageManager = context.packageManager
        val memoryManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also { memoryManager?.getMemoryInfo(it) }
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = windowManager?.defaultDisplay
        val metrics = DisplayMetrics()
        display?.getRealMetrics(metrics)

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensorCount = sensorManager?.getSensorList(android.hardware.Sensor.TYPE_ALL)?.size ?: 0

        val hasFeature = { feature: String -> packageManager.hasSystemFeature(feature) }
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else null
        val socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            buildId = Build.ID,
            securityPatch = securityPatch,
            kernelVersion = readKernelVersion(),
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamBytes = memoryManager?.let { memoryInfo.totalMem },
            availableRamBytes = memoryManager?.let { memoryInfo.availMem },
            screenWidthPixels = metrics.widthPixels.takeIf { it > 0 },
            screenHeightPixels = metrics.heightPixels.takeIf { it > 0 },
            densityDpi = metrics.densityDpi.takeIf { it > 0 },
            refreshRateHz = display?.refreshRate?.takeIf { it > 0f },
            socManufacturer = socManufacturer,
            socModel = socModel,
            uptimeMillis = SystemClock.elapsedRealtime(),
            hasCamera = hasFeature(PackageManager.FEATURE_CAMERA_ANY),
            hasFlash = hasFeature(PackageManager.FEATURE_CAMERA_FLASH),
            hasGps = hasFeature(PackageManager.FEATURE_LOCATION_GPS),
            hasBluetooth = hasFeature(PackageManager.FEATURE_BLUETOOTH),
            hasWifi = hasFeature(PackageManager.FEATURE_WIFI),
            hasNfc = hasFeature(PackageManager.FEATURE_NFC),
            hasBiometric = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hasFeature(PackageManager.FEATURE_FINGERPRINT)
            } else {
                false
            },
            hasUsbHost = hasFeature(PackageManager.FEATURE_USB_HOST),
            sensorCount = sensorCount
        )
    }

    private fun readKernelVersion(): String? = try {
        File("/proc/version").takeIf { it.canRead() }?.readText()?.trim()
            ?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}
