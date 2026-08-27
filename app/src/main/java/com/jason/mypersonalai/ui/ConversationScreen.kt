package com.jason.mypersonalai.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.jason.mypersonalai.agent.Message
import com.jason.mypersonalai.agent.Role
import com.jason.mypersonalai.conversation.ConversationUiState
import com.jason.mypersonalai.conversation.ConversationViewModel
import com.jason.mypersonalai.voice.VoiceInputController
import com.jason.mypersonalai.voice.VoiceOutputController

/**
 * Milestone 5F assistant entry point.
 *
 * Text and voice both submit through ConversationViewModel, so the UI does
 * not create a second command-processing path for speech.
 */
@Composable
fun ConversationRoute() {
    val appContext = LocalContext.current.applicationContext

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val viewModel: ConversationViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ConversationViewModel(context = appContext) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    ConversationScreen(
        uiState = uiState,
        onSendMessage = viewModel::sendMessage,
        onDismissError = viewModel::dismissError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    uiState: ConversationUiState,
    onSendMessage: (String) -> Unit,
    onDismissError: () -> Unit = {}
) {
    var voiceOutputEnabled by remember { mutableStateOf(true) }
    var isSpeaking by remember { mutableStateOf(false) }
    val latestAssistantMessage = uiState.messages.lastOrNull { it.role == Role.ASSISTANT }

    VoiceOutputController(
        text = latestAssistantMessage?.text,
        enabled = voiceOutputEnabled,
        onSpeakingChanged = { isSpeaking = it }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MyPersonalAI",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when {
                                isSpeaking -> "Speaking…"
                                uiState.isBusy -> "Thinking…"
                                else -> "Ready to help"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { voiceOutputEnabled = !voiceOutputEnabled }) {
                        Text(if (voiceOutputEnabled) "🔊 On" else "🔇 Off")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    WelcomeState()
                } else {
                    MessageList(
                        messages = uiState.messages,
                        isBusy = uiState.isBusy,
                        isSpeaking = isSpeaking
                    )
                }
            }

            if (uiState.errorMessage != null) {
                ErrorBanner(message = uiState.errorMessage, onDismiss = onDismissError)
            }

            MessageInputBar(
                enabled = !uiState.isBusy,
                onSend = onSendMessage
            )
        }
    }
}

@Composable
private fun WelcomeState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "AI", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "How can I help?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Type a message or tap Talk to speak directly to MyPersonalAI.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Try:  Open Wi-Fi settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    isBusy: Boolean,
    isSpeaking: Boolean
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isBusy, isSpeaking) {
        val lastIndex = messages.size - 1 + if (isBusy) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(message = message)
        }
        if (isBusy) {
            item(key = "assistant-busy-indicator") {
                AssistantBusyRow()
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == Role.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "AI", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AssistantBusyRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "AI", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("MyPersonalAI is thinking…")
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun MessageInputBar(
    enabled: Boolean,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message MyPersonalAI") },
                    enabled = enabled,
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = enabled && text.isNotBlank(),
                    onClick = {
                        onSend(text)
                        text = ""
                    },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Send")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            VoiceInputController(
                enabled = enabled,
                onTextResult = onSend,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
