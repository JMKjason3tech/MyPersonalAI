package com.jason.mypersonalai.android.capabilities

interface AndroidSettingsLauncher {
    fun open(request: String): SettingsLaunchResult
}

data class SettingsLaunchResult(
    val success: Boolean,
    val destination: String,
    val message: String,
    val usedFallback: Boolean = false
)
