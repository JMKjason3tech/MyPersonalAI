package com.jason.mypersonalai.tools.impl

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.jason.mypersonalai.tools.RiskLevel
import com.jason.mypersonalai.tools.Tool
import com.jason.mypersonalai.tools.ToolError
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import kotlin.math.roundToInt

class DeviceControlTool(private val context: Context) : Tool {
    override val id = "device_control"
    override val description = "Adjust device volume or screen brightness."
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun execute(input: ToolInput): ToolExecutionResult {
        val raw = input.raw.trim().lowercase()
        return when {
            raw.contains("volume") || raw.contains("louder") || raw.contains("quieter") || raw.contains("mute") || raw.contains("unmute") -> controlVolume(raw)
            raw.contains("brightness") || raw.contains("brighter") || raw.contains("dimmer") -> controlBrightness(raw)
            else -> ToolExecutionResult.Failure(ToolError("I can adjust volume or screen brightness. Tell me which one you want to change."))
        }
    }

    private fun controlVolume(text: String): ToolExecutionResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ToolExecutionResult.Failure(ToolError("Android did not provide volume control on this device."))
        val stream = AudioManager.STREAM_MUSIC
        val max = audio.getStreamMaxVolume(stream).coerceAtLeast(1)
        val percent = Regex("(?:to|at|=|)\\s*(\\d{1,3})\\s*%?").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return try {
            when {
                text.contains("mute") && !text.contains("unmute") -> audio.setStreamVolume(stream, 0, 0)
                percent != null -> audio.setStreamVolume(stream, (max * (percent.coerceIn(0, 100) / 100f)).roundToInt(), 0)
                listOf("turn up", "turn volume up", "increase", "raise", "louder", "volume up").any(text::contains) -> audio.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, 0)
                listOf("turn down", "turn volume down", "decrease", "lower", "quieter", "volume down").any(text::contains) -> audio.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, 0)
                text.contains("unmute") -> audio.setStreamVolume(stream, max / 2, 0)
                else -> return ToolExecutionResult.Failure(ToolError("Tell me how you want the volume changed, for example 60% or louder."))
            }
            val current = audio.getStreamVolume(stream)
            ToolExecutionResult.Success("Volume is now about ${(current * 100f / max).roundToInt()}%.")
        } catch (t: Throwable) {
            ToolExecutionResult.Failure(ToolError("Android did not permit me to change the volume on this device.", t))
        }
    }

    private fun controlBrightness(text: String): ToolExecutionResult {
        if (!Settings.System.canWrite(context)) {
            return try {
                context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                ToolExecutionResult.Success("Android has not permitted MyPersonalAI to change screen brightness yet. I opened the permission screen. Allow it, then ask me again.")
            } catch (t: Throwable) {
                ToolExecutionResult.Failure(ToolError("Android has not permitted MyPersonalAI to change screen brightness, and I could not open the permission screen.", t))
            }
        }
        val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val currentPct = (current / 255f * 100f).roundToInt()
        val percent = Regex("(?:to|at|=|)\\s*(\\d{1,3})\\s*%?").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val next = when {
            percent != null -> percent.coerceIn(0, 100)
            listOf("brighter", "increase", "raise", "up").any(text::contains) -> (currentPct + 10).coerceAtMost(100)
            listOf("dimmer", "decrease", "lower", "down").any(text::contains) -> (currentPct - 10).coerceAtLeast(0)
            else -> return ToolExecutionResult.Failure(ToolError("Tell me how bright you want the screen, for example 60% or brighter."))
        }
        return try {
            if (Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, next * 255 / 100)) ToolExecutionResult.Success("Screen brightness is now $next%.")
            else ToolExecutionResult.Failure(ToolError("Android did not permit me to change screen brightness."))
        } catch (t: Throwable) { ToolExecutionResult.Failure(ToolError("Android did not permit me to change screen brightness.", t)) }
    }
}
