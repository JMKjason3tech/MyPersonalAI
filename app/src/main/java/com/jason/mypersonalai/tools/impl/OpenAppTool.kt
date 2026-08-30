package com.jason.mypersonalai.tools.impl

import android.content.Context
import android.content.Intent
import com.jason.mypersonalai.tools.RiskLevel
import com.jason.mypersonalai.tools.Tool
import com.jason.mypersonalai.tools.ToolError
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput

class OpenAppTool(private val context: Context) : Tool {
    override val id = "open_app"
    override val description = "Find and launch an installed Android application by name."
    override val riskLevel = RiskLevel.LOW

    override suspend fun execute(input: ToolInput): ToolExecutionResult {
        val requested = input.raw.trim().replace(Regex("^(please\\s+)?(open|launch|start|access)\\s+", RegexOption.IGNORE_CASE), "").replace(Regex("\\s+(app|application)$", RegexOption.IGNORE_CASE), "").trim()
        if (requested.isBlank()) return ToolExecutionResult.Failure(ToolError("Tell me which app you want to open."))
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        val exact = apps.firstOrNull { pm.getApplicationLabel(it).toString().equals(requested, true) }
        val contains = apps.filter { pm.getApplicationLabel(it).toString().contains(requested, true) }
        val match = exact ?: contains.singleOrNull()
        if (match == null) {
            if (contains.size > 1) return ToolExecutionResult.Success("I found several apps matching '$requested'. Please tell me which one you mean.")
            return ToolExecutionResult.Failure(ToolError("I couldn't find an installed app called $requested."))
        }
        val label = pm.getApplicationLabel(match).toString()
        val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            ?: return ToolExecutionResult.Failure(ToolError("$label is installed, but Android doesn't provide a launch screen for it."))
        return runCatching {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ToolExecutionResult.Success("Opening $label.")
        }.getOrElse { ToolExecutionResult.Failure(ToolError("I couldn't open $label.")) }
    }
}
