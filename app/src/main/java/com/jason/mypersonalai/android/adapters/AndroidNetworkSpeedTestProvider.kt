package com.jason.mypersonalai.android.adapters

import com.jason.mypersonalai.android.capabilities.NetworkSpeedInfo
import com.jason.mypersonalai.android.capabilities.NetworkSpeedTestProvider
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small, explicit Internet throughput probe.
 *
 * This is intentionally separate from NetworkInfoProvider: Android's
 * link-downstream/upstream values are estimates, while this provider
 * performs real network traffic. The test is capped at 512 KiB in each
 * direction so a normal status query never has to transfer large files.
 */
class AndroidNetworkSpeedTestProvider : NetworkSpeedTestProvider {

    companion object {
        private const val ENDPOINT = "https://speed.cloudflare.com"
        private const val DOWNLOAD_URL = "$ENDPOINT/__down?bytes=524288"
        private const val UPLOAD_URL = "$ENDPOINT/__up"
        private const val LATENCY_URL = "$ENDPOINT/__down?bytes=0"
        private const val TEST_BYTES = 512 * 1024L
        private const val TIMEOUT_MS = 8_000
    }

    override suspend fun runSpeedTest(): NetworkSpeedInfo = withContext(Dispatchers.IO) {
        try {
            val latency = measureLatency()
            val download = measureDownload()
            val upload = measureUpload()
            NetworkSpeedInfo(
                downloadMbps = download,
                uploadMbps = upload,
                latencyMs = latency,
                endpoint = ENDPOINT
            )
        } catch (e: Exception) {
            NetworkSpeedInfo(
                endpoint = ENDPOINT,
                errorMessage = e.message?.takeIf { it.isNotBlank() } ?: "Speed test failed."
            )
        }
    }

    private fun measureLatency(): Long? {
        val start = System.nanoTime()
        val connection = (URL(LATENCY_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            useCaches = false
        }
        return try {
            connection.responseCode
            ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(0L)
        } finally {
            connection.disconnect()
        }
    }

    private fun measureDownload(): Double? {
        val connection = (URL(DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            useCaches = false
        }
        return try {
            val start = System.nanoTime()
            var bytesRead = 0L
            BufferedInputStream(connection.inputStream).use { input ->
                val buffer = ByteArray(16 * 1024)
                while (bytesRead < TEST_BYTES) {
                    val remaining = (TEST_BYTES - bytesRead).coerceAtMost(buffer.size.toLong()).toInt()
                    val count = input.read(buffer, 0, remaining)
                    if (count <= 0) break
                    bytesRead += count
                }
            }
            throughputMbps(bytesRead, System.nanoTime() - start)
        } finally {
            connection.disconnect()
        }
    }

    private fun measureUpload(): Double? {
        val payload = ByteArray(TEST_BYTES.toInt())
        val connection = (URL(UPLOAD_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setFixedLengthStreamingMode(payload.size)
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            useCaches = false
            setRequestProperty("Content-Type", "application/octet-stream")
        }
        return try {
            val start = System.nanoTime()
            connection.outputStream.use { it.write(payload) }
            connection.responseCode
            throughputMbps(payload.size.toLong(), System.nanoTime() - start)
        } finally {
            connection.disconnect()
        }
    }

    private fun throughputMbps(bytes: Long, elapsedNanos: Long): Double? {
        if (bytes <= 0L || elapsedNanos <= 0L) return null
        val seconds = elapsedNanos / 1_000_000_000.0
        return max(0.0, bytes * 8.0 / seconds / 1_000_000.0)
    }
}
