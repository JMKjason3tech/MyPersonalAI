package com.jason.mypersonalai.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

private val AiDark = Color(0xFF06101E)
private val AiPanel = Color(0xFF0B1A2B)
private val AiPanel2 = Color(0xFF10263B)
private val AiPurple = Color(0xFF8B5CF6)
private val AiBlue = Color(0xFF16B9F2)
private val AiGreen = Color(0xFF4ADE80)

@Composable
fun ConversationRoute() {
    val appContext = LocalContext.current.applicationContext
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    val viewModel: ConversationViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ConversationViewModel(context = appContext) as T
    })
    val uiState by viewModel.uiState.collectAsState()

    ConversationScreen(
        uiState = uiState,
        onSendMessage = viewModel::sendMessage,
        onDismissError = viewModel::dismissError
    )
}

@Composable
fun ConversationScreen(
    uiState: ConversationUiState,
    onSendMessage: (String) -> Unit,
    onDismissError: () -> Unit = {}
) {
    var voiceOutputEnabled by remember { mutableStateOf(true) }
    var isSpeaking by remember { mutableStateOf(false) }
    var stopToken by remember { mutableIntStateOf(0) }
    val latestAssistantMessage = uiState.messages.lastOrNull { it.role == Role.ASSISTANT }

    VoiceOutputController(
        text = latestAssistantMessage?.text,
        enabled = voiceOutputEnabled,
        stopToken = stopToken,
        onSpeakingChanged = { isSpeaking = it }
    )

    MaterialTheme(colorScheme = darkColorScheme(
        background = AiDark,
        surface = AiPanel,
        surfaceVariant = AiPanel2,
        primary = AiPurple,
        secondary = AiBlue,
        tertiary = AiGreen
    )) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AiDark, Color(0xFF07172A), AiDark)))) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopHeader(isSpeaking, voiceOutputEnabled) { voiceOutputEnabled = !voiceOutputEnabled }
                if (uiState.messages.isEmpty()) WelcomeHero()
                else MessageList(uiState.messages, uiState.isBusy, isSpeaking)

                CapabilityStrip(onSendMessage)

                if (uiState.errorMessage != null) ErrorBanner(uiState.errorMessage, onDismissError)

                VoiceDock(
                    enabled = !uiState.isBusy,
                    isSpeaking = isSpeaking,
                    onStopSpeaking = { stopToken++ },
                    onSend = onSendMessage
                )
            }
        }
    }
}

@Composable
private fun TopHeader(isSpeaking: Boolean, voiceEnabled: Boolean, onToggleVoice: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AiPurple, AiBlue))), contentAlignment = Alignment.Center) {
            Text("AI", fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("MyPersonalAI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (isSpeaking) "Speaking…" else "Personal AI • Ready", style = MaterialTheme.typography.labelMedium, color = if (isSpeaking) AiBlue else Color(0xFF9BB0C6))
        }
        TextButton(onClick = onToggleVoice) { Text(if (voiceEnabled) "Sound" else "Muted") }
        Text("⋮", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun WelcomeHero() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(126.dp).clip(CircleShape).background(Brush.radialGradient(listOf(AiPurple.copy(alpha = .7f), AiBlue.copy(alpha = .12f), Color.Transparent))), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(88.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF182E4B), Color(0xFF0A1828)))), contentAlignment = Alignment.Center) {
                Text("◉", style = MaterialTheme.typography.displaySmall, color = AiBlue)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Good to see you", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Talk to MyPersonalAI or type a request. I’ll report what I found and what I did.", textAlign = TextAlign.Center, color = Color(0xFF9BB0C6), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ColumnScope.MessageList(messages: List<Message>, isBusy: Boolean, isSpeaking: Boolean) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isBusy, isSpeaking) {
        val index = messages.size - 1 + if (isBusy) 1 else 0
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages, key = { it.id }) { MessageBubble(it) }
        if (isBusy) item("busy") { BusyCard() }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val user = message.role == Role.USER
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!user) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AiPurple, AiBlue))), contentAlignment = Alignment.Center) { Text("AI", style = MaterialTheme.typography.labelSmall, color = Color.White) }
            Spacer(Modifier.width(8.dp))
        }
        Surface(color = if (user) Color(0xFF35216D) else AiPanel2, shape = RoundedCornerShape(20.dp), modifier = Modifier.widthIn(max = 330.dp)) {
            Text(message.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color(0xFFE8F1FA), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun BusyCard() {
    Surface(color = AiPanel2, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Thinking & processing…", fontWeight = FontWeight.SemiBold)
                Text("Finding the right capability", color = Color(0xFF9BB0C6), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CapabilityStrip(onSendMessage: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text("Quick capabilities", color = Color(0xFF9BB0C6), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CapabilityCard("Battery", "Check", "What is my battery status?", Modifier.weight(1f), onSendMessage)
            CapabilityCard("Network", "Status", "What is my network status?", Modifier.weight(1f), onSendMessage)
            CapabilityCard("Speed", "Test", "Run a network speed test", Modifier.weight(1f), onSendMessage)
        }
    }
}

@Composable
private fun CapabilityCard(title: String, subtitle: String, command: String, modifier: Modifier, onSendMessage: (String) -> Unit) {
    Surface(color = AiPanel, shape = RoundedCornerShape(14.dp), modifier = modifier.clickable { onSendMessage(command) }) {
        Column(Modifier.padding(10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, color = AiBlue, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(color = Color(0xFF4B2028), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = Color(0xFFFFD8DD), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun VoiceDock(enabled: Boolean, isSpeaking: Boolean, onStopSpeaking: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Surface(color = Color(0xDD081727), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().imePadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (isSpeaking) {
                SpeakingCard(onStopSpeaking)
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask MyPersonalAI…") },
                    maxLines = 3,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Button(
                    enabled = enabled && text.isNotBlank(),
                    onClick = { onSend(text); text = "" },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AiPurple)
                ) { Text("Send") }
            }
            Spacer(Modifier.height(7.dp))
            VoiceInputController(enabled = enabled && !isSpeaking, onTextResult = onSend, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SpeakingCard(onStop: () -> Unit) {
    Surface(color = Color(0xFF101F34), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Waveform(modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = onStop, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D3A)), contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) { Text("■") }
        }
    }
}

@Composable
private fun Waveform(modifier: Modifier = Modifier) {
    Canvas(modifier.height(48.dp)) {
        val bars = 28
        val center = size.height / 2f
        for (i in 0 until bars) {
            val x = size.width * (i + .5f) / bars
            val factor = .25f + ((i * 17) % 9) / 12f
            val half = size.height * .42f * factor
            drawLine(if (i % 2 == 0) AiPurple else AiBlue, Offset(x, center - half), Offset(x, center + half), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}
