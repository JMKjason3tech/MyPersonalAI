package com.jason.mypersonalai.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualIntentResolverTest {

    @Test
    fun `open it reuses the previous settings destination`() {
        val history = listOf(
            user("Open notification settings")
        )

        val result = ContextualIntentResolver.resolve("Open it", history)

        assertEquals(IntentResolver.Intent.OPEN_SETTINGS, result.resolution.intent)
        assertEquals("notifications", result.resolution.target)
        assertTrue(result.usedConversationContext)
        assertEquals("open notification settings", ContextualIntentResolver.canonicalCommand(result.resolution))
    }

    @Test
    fun `check that again reuses the previous device information target`() {
        val history = listOf(
            user("battery")
        )

        val result = ContextualIntentResolver.resolve("Check that again", history)

        assertEquals(IntentResolver.Intent.DEVICE_INFO, result.resolution.intent)
        assertEquals("battery", result.resolution.target)
        assertTrue(result.usedConversationContext)
        assertEquals("battery", ContextualIntentResolver.canonicalCommand(result.resolution))
    }

    @Test
    fun `explicit new intent wins over conversation context`() {
        val history = listOf(
            user("Open notification settings")
        )

        val result = ContextualIntentResolver.resolve("Check my battery", history)

        assertEquals(IntentResolver.Intent.DEVICE_INFO, result.resolution.intent)
        assertEquals("battery", result.resolution.target)
        assertFalse(result.usedConversationContext)
    }

    @Test
    fun `unrelated text does not inherit stale intent`() {
        val history = listOf(
            user("Open notification settings")
        )

        val result = ContextualIntentResolver.resolve("Hello there", history)

        assertEquals(IntentResolver.Intent.UNKNOWN, result.resolution.intent)
        assertFalse(result.usedConversationContext)
    }

    @Test
    fun `a new explicit command after an unrelated turn remains independent`() {
        val history = listOf(
            user("Open notification settings"),
            assistant("Notification settings are open."),
            user("Thanks")
        )

        val result = ContextualIntentResolver.resolve("Tell me my storage", history)

        assertEquals(IntentResolver.Intent.DEVICE_INFO, result.resolution.intent)
        assertEquals("storage", result.resolution.target)
        assertFalse(result.usedConversationContext)
    }

    @Test
    fun `settings context does not turn an information follow-up into settings navigation`() {
        val history = listOf(
            user("Open Wi-Fi settings")
        )

        val result = ContextualIntentResolver.resolve("Tell me more about it", history)

        assertEquals(IntentResolver.Intent.UNKNOWN, result.resolution.intent)
        assertFalse(result.usedConversationContext)
    }

    @Test
    fun `device information context accepts a natural repeat request`() {
        val history = listOf(
            user("Check my storage")
        )

        val result = ContextualIntentResolver.resolve("Show that again", history)

        assertEquals(IntentResolver.Intent.DEVICE_INFO, result.resolution.intent)
        assertEquals("storage", result.resolution.target)
        assertTrue(result.usedConversationContext)
    }

    private fun user(text: String) = Message(
        id = text,
        role = Role.USER,
        text = text,
        timestampMillis = 0L
    )

    private fun assistant(text: String) = Message(
        id = text,
        role = Role.ASSISTANT,
        text = text,
        timestampMillis = 0L
    )
}
