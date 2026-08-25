package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.android.capabilities.AndroidSettingsLauncher
import com.jason.mypersonalai.android.capabilities.SettingsLaunchResult
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSettingsLauncher(
    private val result: SettingsLaunchResult
) : AndroidSettingsLauncher {
    var lastRequest: String? = null

    override fun open(request: String): SettingsLaunchResult {
        lastRequest = request
        return result
    }
}

class OpenSettingsToolTest {
    @Test
    fun `settings tool passes natural language request to launcher`() = runTest {
        val launcher = FakeSettingsLauncher(
            SettingsLaunchResult(true, "Wi-Fi settings", "Opening Wi-Fi settings.")
        )
        val tool = OpenSettingsTool(launcher)

        val result = tool.execute(ToolInput(raw = "Take me to Wi-Fi settings"))

        assertTrue(result is ToolExecutionResult.Success)
        assertEquals("Take me to Wi-Fi settings", launcher.lastRequest)
        assertEquals("Opening Wi-Fi settings.", (result as ToolExecutionResult.Success).output)
    }

    @Test
    fun `settings tool reports launcher failure safely`() = runTest {
        val launcher = FakeSettingsLauncher(
            SettingsLaunchResult(false, "Android Settings", "I couldn't open Android Settings on this device.")
        )
        val tool = OpenSettingsTool(launcher)

        val result = tool.execute(ToolInput(raw = "Open settings"))

        assertTrue(result is ToolExecutionResult.Failure)
        assertEquals(
            "I couldn't open Android Settings on this device.",
            (result as ToolExecutionResult.Failure).error.displayMessage
        )
    }

    @Test
    fun `blank settings request is rejected`() = runTest {
        val launcher = FakeSettingsLauncher(SettingsLaunchResult(true, "Settings", "Opening Settings."))
        val tool = OpenSettingsTool(launcher)

        val result = tool.execute(ToolInput(raw = "   "))

        assertTrue(result is ToolExecutionResult.Failure)
        assertTrue((result as ToolExecutionResult.Failure).error.displayMessage.contains("which Settings"))
    }
}
