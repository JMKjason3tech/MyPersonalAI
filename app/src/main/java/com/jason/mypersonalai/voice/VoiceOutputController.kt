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
 * Owns MyPersonalAI's Text-to-Speech lifecycle.
 *
 * [stopToken] is incremented by the UI when the user explicitly stops the
 * current response. Stopping speech does not remove the assistant message.
 */
@Composable
fun VoiceOutputController(
    text: String?,
    enabled: Boolean,
    stopToken: Int = 0,
    onSpeakingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var initialized by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            initialized = status == TextToSpeech.SUCCESS
        }

        // Set the language after construction as well; Android may finish
        // initialization asynchronously.
        tts = engine
        engine.setLanguage(Locale.getDefault())
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeakingChanged(false)
            }

            @Deprecated("Use onError(String?) when available")
            override fun onError(utteranceId: String?) {
                onSpeakingChanged(false)
            }
        })

        onDispose {
            engine.stop()
            engine.shutdown()
            onSpeakingChanged(false)
            tts = null
            initialized = false
        }
    }

    LaunchedEffect(text, enabled, initialized) {
        val spokenText = text?.trim().orEmpty()
        if (!enabled || !initialized || spokenText.isBlank()) return@LaunchedEffect

        tts?.speak(
            spokenText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "mypersonalai-response"
        )
    }

    LaunchedEffect(stopToken) {
        if (stopToken > 0) {
            tts?.stop()
            onSpeakingChanged(false)
        }
    }
}

