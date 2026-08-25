package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.android.capabilities.BatteryInfo
import com.jason.mypersonalai.android.capabilities.BatteryInfoProvider
import com.jason.mypersonalai.android.capabilities.NetworkInfo
import com.jason.mypersonalai.android.capabilities.NetworkInfoProvider
import com.jason.mypersonalai.android.capabilities.StorageInfo
import com.jason.mypersonalai.android.capabilities.StorageInfoProvider
import com.jason.mypersonalai.android.capabilities.DeviceInfo
import com.jason.mypersonalai.android.capabilities.DeviceInfoProvider
import com.jason.mypersonalai.android.capabilities.NetworkSpeedInfo
import com.jason.mypersonalai.android.capabilities.NetworkSpeedTestProvider
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeBatteryInfoProvider(private val info: BatteryInfo) : BatteryInfoProvider {
    override fun getBatteryInfo(): BatteryInfo = info
}

private class FakeNetworkInfoProvider(private val info: NetworkInfo) : NetworkInfoProvider {
    override fun getNetworkInfo(): NetworkInfo = info
}

private class FakeStorageInfoProvider(private val info: StorageInfo) : StorageInfoProvider {
    override fun getStorageInfo(): StorageInfo = info
}

private class FakeDeviceInfoProvider(private val info: DeviceInfo) : DeviceInfoProvider {
    override fun getDeviceInfo(): DeviceInfo = info
}

private class FakeNetworkSpeedTestProvider(private val info: NetworkSpeedInfo = NetworkSpeedInfo()) : NetworkSpeedTestProvider {
    override suspend fun runSpeedTest(): NetworkSpeedInfo = info
}

class DeviceInfoToolTest {

    private fun buildTool(
        battery: BatteryInfo = BatteryInfo(percentage = 80, isCharging = false, chargeSource = "Not charging"),
        network: NetworkInfo = NetworkInfo(isConnected = true, type = "wifi"),
        storage: StorageInfo = StorageInfo(availableBytes = 2_000_000_000L, totalBytes = 8_000_000_000L),
        device: DeviceInfo = DeviceInfo(
            manufacturer = "Test", brand = "Test", model = "TestPhone", device = "test",
            product = "test", board = "test", hardware = "test", androidVersion = "14",
            apiLevel = 34, buildId = "TEST", securityPatch = "2026-01-01", kernelVersion = "test-kernel",
            supportedAbis = listOf("arm64-v8a"), cpuCores = 8, totalRamBytes = 8L * 1024 * 1024 * 1024,
            availableRamBytes = 4L * 1024 * 1024 * 1024, screenWidthPixels = 1080, screenHeightPixels = 2400,
            densityDpi = 420, refreshRateHz = 120f, socManufacturer = "TestSoC", socModel = "TestChip",
            uptimeMillis = 3_600_000L, hasCamera = true, hasFlash = true, hasGps = true,
            hasBluetooth = true, hasWifi = true, hasNfc = false, hasBiometric = true,
            hasUsbHost = true, sensorCount = 12
        ),
        speed: NetworkSpeedInfo = NetworkSpeedInfo(downloadMbps = 120.0, uploadMbps = 35.0, latencyMs = 18L, endpoint = "test")
    ) = DeviceInfoTool(
        batteryInfoProvider = FakeBatteryInfoProvider(battery),
        networkInfoProvider = FakeNetworkInfoProvider(network),
        storageInfoProvider = FakeStorageInfoProvider(storage),
        deviceInfoProvider = FakeDeviceInfoProvider(device),
        networkSpeedTestProvider = FakeNetworkSpeedTestProvider(speed)
    )

    @Test
    fun `battery query returns only battery info`() = runTest {
        val tool = buildTool(
            battery = BatteryInfo(
                percentage = 42,
                isCharging = true,
                chargeSource = "Wireless",
                health = "Good"
            )
        )

        val result = tool.execute(ToolInput(raw = "battery status"))

        assertTrue(result is ToolExecutionResult.Success)
        val output = (result as ToolExecutionResult.Success).output
        assertTrue(output.contains("42%"))
        assertTrue(output.contains("Wireless"))
        assertTrue(output.contains("Good"))
        assertTrue(!output.contains("Network"))
    }

    @Test
    fun `battery detail fields are omitted when unavailable`() = runTest {
        // temperatureCelsius/voltageMillivolts/technology all null by default.
        val tool = buildTool(battery = BatteryInfo(percentage = 55, isCharging = false, chargeSource = "Not charging"))

        val result = tool.execute(ToolInput(raw = "battery"))

        assertTrue(result is ToolExecutionResult.Success)
        val output = (result as ToolExecutionResult.Success).output
        assertTrue(!output.contains("Temperature"))
        assertTrue(!output.contains("Voltage"))
        assertTrue(!output.contains("Technology"))
    }

    @Test
    fun `network query returns only network info`() = runTest {
        val tool = buildTool(network = NetworkInfo(isConnected = true, type = "cellular"))

        val result = tool.execute(ToolInput(raw = "network status"))

        assertTrue(result is ToolExecutionResult.Success)
        val output = (result as ToolExecutionResult.Success).output
        assertTrue(output.contains("cellular"))
        assertTrue(!output.contains("Battery"))
    }

    @Test
    fun `disconnected network is reported clearly`() = runTest {
        val tool = buildTool(network = NetworkInfo(isConnected = false, type = "none"))

        val result = tool.execute(ToolInput(raw = "network"))

        assertTrue(result is ToolExecutionResult.Success)
        assertTrue((result as ToolExecutionResult.Success).output.contains("disconnected"))
    }

    @Test
    fun `wifi network with ssid granted shows the network name`() = runTest {
        val tool = buildTool(
            network = NetworkInfo(
                isConnected = true,
                type = "wifi",
                ssid = "HomeWifi",
                linkSpeedMbps = 433,
                frequencyMhz = 5180
            )
        )

        val result = tool.execute(ToolInput(raw = "network"))

        assertTrue(result is ToolExecutionResult.Success)
        val output = (result as ToolExecutionResult.Success).output
        assertTrue(output.contains("HomeWifi"))
        assertTrue(output.contains("433 Mbps"))
        assertTrue(output.contains("5180 MHz"))
    }

    @Test
    fun `wifi network without location permission shows unavailable, not a failure`() = runTest {
        val tool = buildTool(
            network = NetworkInfo(isConnected = true, type = "wifi", ssid = null)
        )

        val result = tool.execute(ToolInput(raw = "network"))

        assertTrue(result is ToolExecutionResult.Success)
        val output = (result as ToolExecutionResult.Success).output
        assertTrue(output.contains("unavailable"))
    }

    @Test
    fun `storage query returns only storage info`() = runTest {
        val tool = buildTool()

        val result = tool.execute(ToolInput(raw = "storage status"))

        assertTrue(result is ToolExecutionResult.Success)
        val output = (result as ToolExecutionResult.Success).output
        assertTrue(output.contains("GB"))
        assertTrue(!output.contains("Battery"))
        assertTrue(!output.contains("Network"))
    }

    @Test
    fun `device info returns device details without battery network or storage`() = runTest {
        val tool = buildTool()

        val result = tool.execute(ToolInput(raw = "device info"))

        assertTrue(result is ToolExecutionResult.Success)
        val output = (result as ToolExecutionResult.Success).output
        assertTrue(output.contains("Manufacturer: Test"))
        assertTrue(output.contains("Model: TestPhone"))
        assertTrue(output.contains("Android: 14"))
        assertTrue(output.contains("RAM total"))
        assertTrue(output.contains("Hardware capabilities"))
        assertTrue(!output.contains("Battery:"))
        assertTrue(!output.contains("Network:"))
        assertTrue(!output.contains("Storage:"))
    }

    @Test
    fun `speed test query returns measured download upload and latency`() = runTest {
        val tool = buildTool()

        val result = tool.execute(ToolInput(raw = "speed test"))

        assertTrue(result is ToolExecutionResult.Success)
        val output = (result as ToolExecutionResult.Success).output
        assertTrue(output.contains("Download: 120.0 Mbps"))
        assertTrue(output.contains("Upload: 35.0 Mbps"))
        assertTrue(output.contains("Latency: 18 ms"))
    }
}
