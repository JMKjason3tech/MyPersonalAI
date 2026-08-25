package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.android.capabilities.AndroidSettingsLauncher
import com.jason.mypersonalai.android.capabilities.SettingsLaunchResult
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSettingsLauncher : AndroidSettingsLauncher {
    var lastRequest: String? = null
    var result = SettingsLaunchResult(
        success = true,
        destination = "Wi-Fi settings",
        message = "Opening Wi-Fi settings."
    )

    override fun open(request: String): SettingsLaunchResult {
        lastRequest = request
        return result
    }
}

class OpenSettingsToolTest {

    @Test
    fun blankRequestFailsClearly() = runTest {
        val launcher = FakeSettingsLauncher()
        val result = OpenSettingsTool(launcher).execute(ToolInput(raw = "  "))

        assertTrue(result is ToolExecutionResult.Failure)
        assertTrue((result as ToolExecutionResult.Failure).error.displayMessage.contains("Settings"))
    }

    @Test
    fun successfulRequestUsesLauncher() = runTest {
        val launcher = FakeSettingsLauncher()
        val result = OpenSettingsTool(launcher).execute(ToolInput(raw = "open wifi settings"))

        assertTrue(result is ToolExecutionResult.Success)
        assertTrue(launcher.lastRequest == "open wifi settings")
        assertTrue((result as ToolExecutionResult.Success).output.contains("Wi-Fi"))
    }

    @Test
    fun launcherFailureBecomesToolFailure() = runTest {
        val launcher = FakeSettingsLauncher().apply {
            result = SettingsLaunchResult(
                success = false,
                destination = "Android Settings",
                message = "I couldn't open Android Settings on this device."
            )
        }

        val result = OpenSettingsTool(launcher).execute(ToolInput(raw = "open settings"))

        assertTrue(result is ToolExecutionResult.Failure)
        assertTrue((result as ToolExecutionResult.Failure).error.displayMessage.contains("couldn't open"))
    }
}
