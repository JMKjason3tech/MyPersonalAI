package com.jason.mypersonalai.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.jason.mypersonalai.agent.Message
import com.jason.mypersonalai.agent.Role
import com.jason.mypersonalai.conversation.ConversationSummary
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
        onNewChat = viewModel::newChat,
        onResetChat = viewModel::resetCurrentChat,
        onOpenChat = viewModel::openConversation,
        onDeleteChat = viewModel::deleteConversation,
        onDismissError = viewModel::dismissError
    )
}

@Composable
fun ConversationScreen(
    uiState: ConversationUiState,
    onSendMessage: (String) -> Unit,
    onNewChat: () -> Unit = {},
    onResetChat: () -> Unit = {},
    onOpenChat: (String) -> Unit = {},
    onDeleteChat: (String) -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    var voiceOutputEnabled by remember { mutableStateOf(true) }
    var isSpeaking by remember { mutableStateOf(false) }
    var stopToken by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val latestAssistantMessage = uiState.messages.lastOrNull { it.role == Role.ASSISTANT }

    VoiceOutputController(latestAssistantMessage?.text, voiceOutputEnabled, stopToken) { isSpeaking = it }

    MaterialTheme(colorScheme = darkColorScheme(background = AiDark, surface = AiPanel, surfaceVariant = AiPanel2, primary = AiPurple, secondary = AiBlue, tertiary = AiGreen)) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                HistoryDrawer(
                    currentId = uiState.conversationId,
                    history = uiState.history,
                    onNewChat = { onNewChat(); drawerScope.launch { drawerState.close() } },
                    onOpenChat = { onOpenChat(it); drawerScope.launch { drawerState.close() } },
                    onDeleteChat = onDeleteChat
                )
            }
        ) {
            Scaffold(containerColor = Color.Transparent) { padding ->
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AiDark, Color(0xFF07172A), AiDark))).padding(padding)) {
                    Column(Modifier.fillMaxSize()) {
                        TopHeader(uiState.conversationTitle, isSpeaking, voiceOutputEnabled, onToggleVoice = { voiceOutputEnabled = !voiceOutputEnabled }, onOpenHistory = { drawerScope.launch { drawerState.open() } })
                        if (uiState.messages.isEmpty()) WelcomeHero()
                        else MessageList(uiState.messages, uiState.isBusy, isSpeaking)
                        if (uiState.messages.isEmpty()) CapabilityGrid(onSendMessage)
                        else CompactSuggestions(onSendMessage)
                        if (uiState.errorMessage != null) ErrorBanner(uiState.errorMessage, onDismissError)
                        VoiceDock(!uiState.isBusy, isSpeaking, { stopToken++ }, onSendMessage)
                    }
                    if (uiState.messages.isNotEmpty()) {
                        Box(Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 58.dp).clip(RoundedCornerShape(12.dp)).background(AiPanel2).clickable { showResetDialog = true }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("Reset chat", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD7E5F2))
                        }
                    }
                }
            }
        }
    }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset this chat?") },
            text = { Text("This removes the current conversation from chat history and starts a blank chat. Other saved chats are kept.") },
            confirmButton = { TextButton(onClick = { showResetDialog = false; onResetChat() }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HistoryDrawer(currentId: String, history: List<ConversationSummary>, onNewChat: () -> Unit, onOpenChat: (String) -> Unit, onDeleteChat: (String) -> Unit) {
    ModalDrawerSheet(drawerContainerColor = AiPanel) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AiPurple, AiBlue))), contentAlignment = Alignment.Center) { Text("AI", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column { Text("MyPersonalAI", fontWeight = FontWeight.Bold); Text("Conversation history", color = Color(0xFF9BB0C6), style = MaterialTheme.typography.labelMedium) }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onNewChat, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AiPurple)) { Text("＋  New chat") }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = .08f))
            Text("Recent chats", color = Color(0xFF9BB0C6), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
            if (history.isEmpty()) {
                Text("Your saved conversations will appear here.", color = Color(0xFF9BB0C6), modifier = Modifier.padding(8.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(history, key = { it.id }) { chat ->
                        NavigationDrawerItem(
                            label = { Text(chat.title, maxLines = 1) },
                            selected = chat.id == currentId,
                            onClick = { onOpenChat(chat.id) },
                            badge = { Text(chat.messageCount.toString(), style = MaterialTheme.typography.labelSmall) }
                        )
                        TextButton(onClick = { onDeleteChat(chat.id) }, modifier = Modifier.align(Alignment.End)) { Text("Delete", color = Color(0xFFFF9CA8), style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeader(title: String, isSpeaking: Boolean, voiceEnabled: Boolean, onToggleVoice: () -> Unit, onOpenHistory: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("☰", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clickable(onClick = onOpenHistory).padding(8.dp))
        Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AiPurple, AiBlue))), contentAlignment = Alignment.Center) { Text("AI", fontWeight = FontWeight.Bold, color = Color.White) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (isSpeaking) "Speaking…" else "Personal AI • Ready", style = MaterialTheme.typography.labelMedium, color = if (isSpeaking) AiBlue else Color(0xFF9BB0C6))
        }
        TextButton(onClick = onToggleVoice) { Text(if (voiceEnabled) "Sound" else "Muted") }
    }
}

@Composable
private fun WelcomeHero() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(108.dp).clip(CircleShape).background(Brush.radialGradient(listOf(AiPurple.copy(alpha = .7f), AiBlue.copy(alpha = .12f), Color.Transparent))), contentAlignment = Alignment.Center) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF182E4B), Color(0xFF0A1828)))), contentAlignment = Alignment.Center) { Text("◉", style = MaterialTheme.typography.displaySmall, color = AiBlue) }
        }
        Spacer(Modifier.height(10.dp))
        Text("How can I help?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Ask naturally. You can type, use the microphone, or tap an action below.", textAlign = TextAlign.Center, color = Color(0xFF9BB0C6), modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ColumnScope.MessageList(messages: List<Message>, isBusy: Boolean, isSpeaking: Boolean) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isBusy, isSpeaking) {
        val index = messages.size - 1 + if (isBusy) 1 else 0
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(messages, key = { it.id }) { MessageBubble(it) }
        if (isBusy) item("busy") { BusyCard() }
    }
}

@Composable private fun MessageBubble(message: Message) {
    val user = message.role == Role.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!user) { Box(Modifier.size(30.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AiPurple, AiBlue))), contentAlignment = Alignment.Center) { Text("AI", style = MaterialTheme.typography.labelSmall, color = Color.White) }; Spacer(Modifier.width(8.dp)) }
        Surface(color = if (user) Color(0xFF35216D) else AiPanel2, shape = RoundedCornerShape(20.dp), modifier = Modifier.widthIn(max = 330.dp)) { Text(message.text, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color(0xFFE8F1FA), style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable private fun BusyCard() { Surface(color = AiPanel2, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Column { Text("Thinking & processing…", fontWeight = FontWeight.SemiBold); Text("Finding the right capability", color = Color(0xFF9BB0C6), style = MaterialTheme.typography.labelMedium) } } } }

@Composable
private fun CapabilityGrid(onSendMessage: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text("Try something", color = Color(0xFF9BB0C6), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard("🔋", "Battery", "What is my battery status?", Modifier.weight(1f), onSendMessage)
            ActionCard("📶", "Network", "What is my network status?", Modifier.weight(1f), onSendMessage)
            ActionCard("📱", "Device", "Show my device information", Modifier.weight(1f), onSendMessage)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard("⚙", "Settings", "Open settings", Modifier.weight(1f), onSendMessage)
            ActionCard("🔆", "Brightness", "Increase brightness", Modifier.weight(1f), onSendMessage)
            ActionCard("🔊", "Volume", "Increase volume", Modifier.weight(1f), onSendMessage)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard("📱", "Apps", "Open an app", Modifier.weight(1f), onSendMessage)
            ActionCard("🧮", "Calculate", "Calculate 25 * 4", Modifier.weight(1f), onSendMessage)
            ActionCard("💡", "Help", "What can you help me with?", Modifier.weight(1f), onSendMessage)
        }
    }
}

@Composable private fun ActionCard(icon: String, title: String, command: String, modifier: Modifier, onSend: (String) -> Unit) { Surface(color = AiPanel, shape = RoundedCornerShape(16.dp), modifier = modifier.clickable { onSend(command) }) { Column(Modifier.padding(11.dp)) { Text(icon); Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge); Text("Tap to ask", color = AiBlue, style = MaterialTheme.typography.labelSmall) } } }

@Composable private fun CompactSuggestions(onSend: (String) -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Check battery", "Network status", "Open settings").forEach { suggestion -> Surface(color = AiPanel, shape = RoundedCornerShape(50.dp), modifier = Modifier.clickable { onSend(suggestion) }) { Text(suggestion, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFFBDD2E5), style = MaterialTheme.typography.labelMedium) } } } }

@Composable private fun ErrorBanner(message: String, onDismiss: () -> Unit) { Surface(color = Color(0xFF4B2028), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f), color = Color(0xFFFFD8DD), style = MaterialTheme.typography.bodySmall); TextButton(onClick = onDismiss) { Text("Dismiss") } } } }

@Composable private fun VoiceDock(enabled: Boolean, isSpeaking: Boolean, onStopSpeaking: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Surface(color = Color(0xDD081727), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().imePadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (isSpeaking) { SpeakingCard(onStopSpeaking); Spacer(Modifier.height(8.dp)) }
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(value = text, onValueChange = { text = it }, enabled = enabled, modifier = Modifier.weight(1f), placeholder = { Text("Ask anything…") }, maxLines = 3, shape = RoundedCornerShape(18.dp))
                Spacer(Modifier.width(7.dp))
                Button(enabled = enabled && text.isNotBlank(), onClick = { onSend(text); text = "" }, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = AiPurple)) { Text("Send") }
            }
            Spacer(Modifier.height(7.dp))
            VoiceInputController(enabled = enabled && !isSpeaking, onTextResult = onSend, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun SpeakingCard(onStop: () -> Unit) { Surface(color = Color(0xFF101F34), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("🔊  MyPersonalAI is speaking…", Modifier.weight(1f), color = Color(0xFFD7E5F2)); Button(onClick = onStop, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D3A)), contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) { Text("■") } } } }

