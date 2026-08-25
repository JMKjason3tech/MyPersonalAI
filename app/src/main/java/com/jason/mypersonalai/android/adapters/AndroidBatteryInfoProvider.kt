package com.jason.mypersonalai.android.adapters

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.jason.mypersonalai.android.capabilities.BatteryInfo
import com.jason.mypersonalai.android.capabilities.BatteryInfoProvider

/**
 * Real Android implementation of [BatteryInfoProvider].
 *
 * Percentage and charging state come from [BatteryManager] directly.
 * Temperature, voltage, health, and technology are only available via
 * the sticky ACTION_BATTERY_CHANGED broadcast, read synchronously here
 * with a null receiver (the standard way to read a sticky broadcast's
 * last value without actually registering a listener).
 *
 * Permission: NONE required for any of this. All battery properties
 * are public system information.
 */
class AndroidBatteryInfoProvider(
    private val context: Context
) : BatteryInfoProvider {

    override fun getBatteryInfo(): BatteryInfo {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging

        val stickyIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val temperatureTenthsCelsius = stickyIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val voltage = stickyIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val healthCode = stickyIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val technology = stickyIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        val plugged = stickyIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargeSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Unknown source" else "Not charging"
        }

        return BatteryInfo(
            percentage = percentage,
            isCharging = isCharging,
            temperatureCelsius = if (temperatureTenthsCelsius >= 0) temperatureTenthsCelsius / 10.0f else null,
            voltageMillivolts = if (voltage >= 0) voltage else null,
            health = healthCodeToString(healthCode),
            technology = technology,
            chargeSource = chargeSource
        )
    }

    private fun healthCodeToString(code: Int): String = when (code) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Unknown"
    }
}
