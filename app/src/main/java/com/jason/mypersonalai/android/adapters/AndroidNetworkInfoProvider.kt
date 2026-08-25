package com.jason.mypersonalai.android.adapters

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.location.LocationManager
import com.jason.mypersonalai.android.capabilities.NetworkInfo
import com.jason.mypersonalai.android.capabilities.NetworkInfoProvider

/** Real Android implementation of the read-only network status capability. */
class AndroidNetworkInfoProvider(
    private val context: Context
) : NetworkInfoProvider {

    override fun getNetworkInfo(): NetworkInfo {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetworkInfo(isConnected = false, type = "unknown")

        val activeNetwork = connectivityManager.activeNetwork
            ?: return NetworkInfo(isConnected = false, type = "none")

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkInfo(isConnected = false, type = "unknown")

        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }

        // ConnectivityManager reports these values in Kbps. Keep the
        // fractional part when converting to Mbps; integer division was
        // responsible for losing values such as 108.4 Mbps -> 108 Mbps.
        val downstreamMbps = capabilities.linkDownstreamBandwidthKbps
            .takeIf { it > 0 }
            ?.div(1000.0)
        val upstreamMbps = capabilities.linkUpstreamBandwidthKbps
            .takeIf { it > 0 }
            ?.div(1000.0)

        if (type != "wifi") {
            return NetworkInfo(
                isConnected = true,
                type = type,
                downstreamMbps = downstreamMbps,
                upstreamMbps = upstreamMbps
            )
        }

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiInfo = wifiManager?.connectionInfo

        val linkSpeedMbps = wifiInfo?.linkSpeed?.takeIf { it >= 0 }
        val frequencyMhz = wifiInfo?.frequency?.takeIf { it > 0 }

        val hasLocationPermission = context.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationServicesEnabled = locationManager?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                it.isLocationEnabled
            } else {
                true
            }
        } ?: true

        val rawSsid = wifiInfo?.ssid?.removeSurrounding("\"")?.trim()
        val (ssid, ssidUnavailableReason) = when {
            !hasLocationPermission -> null to "location permission not granted"
            !locationServicesEnabled -> null to "location services are turned off"
            rawSsid.isNullOrBlank() || rawSsid == "<unknown ssid>" ->
                null to "Android did not provide the Wi-Fi name"
            else -> rawSsid to null
        }

        return NetworkInfo(
            isConnected = true,
            type = type,
            ssid = ssid,
            ssidUnavailableReason = ssidUnavailableReason,
            linkSpeedMbps = linkSpeedMbps,
            frequencyMhz = frequencyMhz,
            downstreamMbps = downstreamMbps,
            upstreamMbps = upstreamMbps
        )
    }
}
