package com.jason.mypersonalai.android.capabilities

/**
 * Platform-neutral launcher for Android system Settings destinations.
 *
 * The tool layer depends on this interface rather than Android framework
 * classes, which keeps routing deterministic and unit-testable.
 */
interface AndroidSettingsLauncher {
    /** Opens the best matching Android Settings screen for [request]. */
    fun open(request: String): SettingsLaunchResult
}

data class SettingsLaunchResult(
    val success: Boolean,
    val destination: String,
    val message: String,
    val usedFallback: Boolean = false
)
