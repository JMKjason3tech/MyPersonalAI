package com.jason.mypersonalai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.jason.mypersonalai.ui.ConversationRoute

/**
 * Milestone 2 entry point.
 *
 * Hosts the conversation UI. This activity still knows nothing about
 * how responses are produced -- that's entirely behind
 * [ConversationRoute] / [com.jason.mypersonalai.conversation.ConversationViewModel]
 * / [com.jason.mypersonalai.agent.AgentEngine].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyPersonalAITheme {
                ConversationRoute()
            }
        }
    }
}

@Composable
fun MyPersonalAITheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
