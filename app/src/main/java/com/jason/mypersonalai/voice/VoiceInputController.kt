package com.jason.mypersonalai.voice

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import kotlinx.coroutines.delay

/** User-started voice session with automatic re-listening after each result. */
@Composable
fun VoiceInputController(
    enabled: Boolean,
    onTextResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentResult by rememberUpdatedState(onTextResult)
    var listening by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun stop() {
        context.startService(Intent(context, VoiceSessionService::class.java).setAction(VoiceSessionService.ACTION_STOP))
        listening = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startForegroundService(context, Intent(context, VoiceSessionService::class.java).setAction(VoiceSessionService.ACTION_START))
            listening = true
            error = null
        } else {
            error = "Microphone permission is required for voice input."
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    VoiceSessionService.ACTION_RESULT -> {
                        val text = intent.getStringExtra(VoiceSessionService.EXTRA_TEXT).orEmpty()
                        if (text.isNotBlank()) currentResult(text)
                        listening = true
                        error = null
                    }
                    VoiceSessionService.ACTION_ERROR -> {
                        error = intent.getStringExtra(VoiceSessionService.EXTRA_ERROR) ?: "Voice input failed."
                        listening = true
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(VoiceSessionService.ACTION_RESULT)
            addAction(VoiceSessionService.ACTION_ERROR)
        }
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    DisposableEffect(enabled) {
        if (!enabled && listening) stop()
        onDispose { }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!listening) {
            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        startForegroundService(context, Intent(context, VoiceSessionService::class.java).setAction(VoiceSessionService.ACTION_START))
                        listening = true
                        error = null
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) { Text("🎙️  Talk to MyPersonalAI", fontWeight = FontWeight.SemiBold) }
        } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.clip(CircleShape)) {
                        Text("🎙️", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Listening…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        error ?: "You can continue talking after MyPersonalAI opens Settings or another app.",
                        textAlign = TextAlign.Center,
                        color = if (error == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = ::stop) { Text("Stop listening") }
                }
            }
        }
    }
}
