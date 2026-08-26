package com.jason.mypersonalai.voice

import android.Manifest
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/**
 * Small UI/controller boundary around Android SpeechRecognizer.
 * Recognition results are returned as plain text so the existing
 * conversation pipeline remains the single command-processing path.
 */
@Composable
fun VoiceInputController(
    enabled: Boolean,
    onTextResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(VoiceInputState.READY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            state = VoiceInputState.ERROR
            errorMessage = "Microphone permission is required for voice input."
        }
    }

    DisposableEffect(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            state = VoiceInputState.ERROR
            errorMessage = "Speech recognition is not available on this device."
        }

        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }

    LaunchedEffect(state) {
        if (state == VoiceInputState.ERROR) {
            delay(2500)
            state = VoiceInputState.READY
            errorMessage = null
        }
    }

    val startListening: () -> Unit = startListening@{
        if (!enabled || state == VoiceInputState.LISTENING) return@startListening

        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!permission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return@startListening
        }

        val speechRecognizer = recognizer
        if (speechRecognizer == null) {
            state = VoiceInputState.ERROR
            errorMessage = "Speech recognition is unavailable."
            return@startListening
        }

        errorMessage = null
        state = VoiceInputState.LISTENING

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit

            override fun onError(error: Int) {
                state = VoiceInputState.ERROR
                errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't understand that."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission was denied."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Try again."
                    else -> "Voice input failed. Please try again."
                }
            }

            override fun onResults(results: android.os.Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (text.isBlank()) {
                    state = VoiceInputState.ERROR
                    errorMessage = "I couldn't understand that."
                    return
                }

                state = VoiceInputState.PROCESSING
                onTextResult(text)
            }
        })

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.startListening(intent)
    }

    val stopListening: () -> Unit = {
        recognizer?.cancel()
        state = VoiceInputState.READY
        errorMessage = null
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            VoiceInputState.READY -> {
                Button(onClick = startListening, enabled = enabled) {
                    Text("Mic")
                }
            }
            VoiceInputState.LISTENING -> {
                Button(onClick = stopListening, enabled = true) {
                    Text("Stop")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Listening…")
            }
            VoiceInputState.PROCESSING -> {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                Text("Processing voice…")
                LaunchedEffect(Unit) {
                    delay(500)
                    state = VoiceInputState.READY
                }
            }
            VoiceInputState.ERROR -> {
                Button(onClick = startListening, enabled = enabled) {
                    Text("Retry")
                }
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(errorMessage!!)
                }
            }
        }
    }
}
