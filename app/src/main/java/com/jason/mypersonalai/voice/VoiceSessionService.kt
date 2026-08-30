package com.jason.mypersonalai.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Keeps an explicitly started voice session alive while MyPersonalAI opens
 * another activity (for example Android Settings). It is intentionally a
 * foreground service: Android 10 permits a user-started microphone foreground
 * service and newer Android versions can enforce additional foreground rules.
 */
class VoiceSessionService : Service() {
    companion object {
        const val ACTION_START = "com.jason.mypersonalai.voice.START"
        const val ACTION_STOP = "com.jason.mypersonalai.voice.STOP"
        const val ACTION_RESULT = "com.jason.mypersonalai.voice.RESULT"
        const val ACTION_ERROR = "com.jason.mypersonalai.voice.ERROR"
        const val EXTRA_TEXT = "text"
        const val EXTRA_ERROR = "error"
        private const val CHANNEL_ID = "mypersonalai_voice"
        private const val NOTIFICATION_ID = 6101
    }

    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private var restarting = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSession()
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, notification())
                active = true
                listen()
            }
        }
        return START_NOT_STICKY
    }

    private fun listen() {
        if (!active || restarting) return
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null

        val available = SpeechRecognizer.isRecognitionAvailable(this)
        if (!available) {
            sendError("Speech recognition is unavailable on this device.")
            return
        }

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        } else {
            SpeechRecognizer.createSpeechRecognizer(this)
        }
        recognizer = r

        val online = NetworkState.isValidated(this)
        val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // On Android 10 there is no createOnDeviceSpeechRecognizer API.
            // EXTRA_PREFER_OFFLINE is the correct request to a recognizer that
            // provides a local language model. It may still fail if the phone
            // has no installed offline model.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, !online)
        }

        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                if (text.isNotBlank()) {
                    sendBroadcast(Intent(ACTION_RESULT).setPackage(packageName).putExtra(EXTRA_TEXT, text))
                } else {
                    sendError("I couldn't understand that.")
                }
                scheduleRestart()
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't understand that. Please repeat."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything. Please repeat."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        if (online) "The network speech service is unavailable. Please try again."
                        else "Offline speech recognition needs an installed offline language model on this device."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Please repeat."
                    else -> "Voice input failed. Please repeat."
                }
                sendError(message)
                scheduleRestart()
            }
        })
        try { r.startListening(request) } catch (_: Throwable) { sendError("Couldn't start voice input. Please repeat."); scheduleRestart() }
    }

    private fun scheduleRestart() {
        if (!active || restarting) return
        restarting = true
        android.os.Handler(mainLooper).postDelayed({
            restarting = false
            if (active) listen()
        }, 650L)
    }

    private fun sendError(message: String) {
        sendBroadcast(Intent(ACTION_ERROR).setPackage(packageName).putExtra(EXTRA_ERROR, message))
    }

    private fun stopSession() {
        active = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        active = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "MyPersonalAI voice", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID)
        else Notification.Builder(this)
        return builder.setContentTitle("MyPersonalAI is listening")
            .setContentText("Voice session is active. Say a command or say stop listening.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }
}
