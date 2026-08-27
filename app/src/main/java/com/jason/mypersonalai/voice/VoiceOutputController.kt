package com.jason.mypersonalai.voice

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * MyPersonalAI voice-output boundary.
 *
 * Speaks only newly supplied assistant text. The controller owns the Android
 * TextToSpeech lifecycle so the Compose conversation UI remains declarative.
 */
@Composable
fun VoiceOutputController(
    text: String?,
    enabled: Boolean,
    onSpeakingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var initialized by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            initialized = status == TextToSpeech.SUCCESS
            if (initialized) {
                engineSafeSetLanguage(tts)
            }
        }
        tts = engine
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeakingChanged(false)
            }

            override fun onError(utteranceId: String?) {
                onSpeakingChanged(false)
            }
        })

        onDispose {
            onSpeakingChanged(false)
            engine.stop()
            engine.shutdown()
            tts = null
            initialized = false
        }
    }

    LaunchedEffect(text, enabled, initialized) {
        val spokenText = text?.trim().orEmpty()
        if (!enabled || !initialized || spokenText.isBlank()) return@LaunchedEffect

        engineSafeSetLanguage(tts)
        tts?.speak(
            spokenText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "mypersonalai-response"
        )
    }
}

private fun engineSafeSetLanguage(engine: TextToSpeech?) {
    engine?.setLanguage(Locale.getDefault())
}
