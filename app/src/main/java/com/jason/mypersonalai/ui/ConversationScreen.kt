package com.jason.mypersonalai.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import com.jason.mypersonalai.agent.Message
import com.jason.mypersonalai.agent.Role
import com.jason.mypersonalai.conversation.ConversationUiState
import com.jason.mypersonalai.conversation.ConversationViewModel

/**
 * Stateful entry point: owns/obtains the [ConversationViewModel] and
 * connects it to the stateless [ConversationScreen]. This is the only
 * place in the UI layer that knows a ViewModel exists.
 *
 * As of Milestone 5, this is also the only place in the UI layer that
 * touches [android.content.Context] — it's read here via [LocalContext]
 * purely to forward the application context down to [ConversationViewModel]
 * (and from there to [com.jason.mypersonalai.agent.MockAgentEngine]'s
 * Android-capability tools). Nothing else in the UI layer needs it.
 *
 * Milestone 5b: this is also the only place that requests a runtime
 * permission. ACCESS_FINE_LOCATION is requested once, on first
 * composition, purely so DeviceInfoTool's WiFi-name capability can
 * work -- see AndroidNetworkInfoProvider for what happens if it's
 * denied (it degrades gracefully, never crashes or blocks the app).
 */
@Composable
fun ConversationRoute() {
    val appContext = LocalContext.current.applicationContext

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Result isn't tracked here -- AndroidNetworkInfoProvider re-checks
           permission state itself at query time, so no callback handling
           is needed on this side either way. */ }

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

/**
 * Stateless conversation screen. Pure function of [uiState] plus
 * callbacks — has no knowledge of the ViewModel, AgentEngine, or how
 * responses are produced. This is what makes the UI layer swappable
 * and independently testable/previewable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    uiState: ConversationUiState,
    onSendMessage: (String) -> Unit,
    onDismissError: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MyPersonalAI") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    EmptyConversationState()
                } else {
                    MessageList(
                        messages = uiState.messages,
                        isBusy = uiState.isBusy
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
private fun EmptyConversationState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Send a message below to start a conversation.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    isBusy: Boolean
) {
    val listState = rememberLazyListState()

    // Auto-scroll to the newest message (or the busy indicator) whenever
    // the conversation grows.
    LaunchedEffect(messages.size, isBusy) {
        val lastIndex = messages.size - 1 + if (isBusy) 1 else 0
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AssistantBusyRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MyPersonalAI is thinking…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Dismiss",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(onClick = onDismiss)
            )
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
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message MyPersonalAI") },
                enabled = enabled,
                maxLines = 5
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                enabled = enabled && text.isNotBlank(),
                onClick = {
                    onSend(text)
                    text = ""
                }
            ) {
                Text("Send")
            }
        }
    }
}
