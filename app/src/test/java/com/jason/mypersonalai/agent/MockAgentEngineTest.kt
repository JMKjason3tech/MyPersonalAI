package com.jason.mypersonalai.agent

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAgentEngineTest {

    private val engine = MockAgentEngine(responseDelayMillis = 0L)

    @Test
    fun `responds successfully to a single user message`() = runTest {
        val history = listOf(
            Message(id = "1", role = Role.USER, text = "hello", timestampMillis = 0L)
        )

        val result = engine.generateResponse(history)

        assertTrue(result is AgentResult.Success)
        val message = (result as AgentResult.Success).message
        assertEquals(Role.ASSISTANT, message.role)
        assertTrue(message.text.contains("hello"))
    }

    @Test
    fun `fails when there is no user message in history`() = runTest {
        val result = engine.generateResponse(emptyList())

        assertTrue(result is AgentResult.Failure)
    }

    @Test
    fun `response references the most recent user message, not an earlier one`() = runTest {
        val history = listOf(
            Message(id = "1", role = Role.USER, text = "first", timestampMillis = 0L),
            Message(id = "2", role = Role.ASSISTANT, text = "reply", timestampMillis = 1L),
            Message(id = "3", role = Role.USER, text = "second", timestampMillis = 2L)
        )

        val result = engine.generateResponse(history) as AgentResult.Success

        assertTrue(result.message.text.contains("second"))
        assertTrue(!result.message.text.contains("first"))
    }

    // --- Milestone 3: tool routing ---

    @Test
    fun `a pure arithmetic expression is routed to the calculator tool`() = runTest {
        val history = listOf(
            Message(id = "1", role = Role.USER, text = "2 + 2", timestampMillis = 0L)
        )

        val result = engine.generateResponse(history)

        assertTrue(result is AgentResult.Success)
        assertEquals("4", (result as AgentResult.Success).message.text)
    }

    @Test
    fun `a natural-language message is NOT routed to the calculator tool`() = runTest {
        val history = listOf(
            Message(id = "1", role = Role.USER, text = "what is 2 + 2", timestampMillis = 0L)
        )

        val result = engine.generateResponse(history)

        assertTrue(result is AgentResult.Success)
        // Falls back to the generic mock reply, not a computed "4".
        assertTrue((result as AgentResult.Success).message.text.contains("Mock response"))
    }

    @Test
    fun `an echo-prefixed message is routed to the echo tool`() = runTest {
        val history = listOf(
            Message(id = "1", role = Role.USER, text = "echo hello there", timestampMillis = 0L)
        )

        val result = engine.generateResponse(history)

        assertTrue(result is AgentResult.Success)
        assertEquals("hello there", (result as AgentResult.Success).message.text)
    }

    @Test
    fun `an invalid expression surfaces as a structured failure, not a crash`() = runTest {
        val history = listOf(
            Message(id = "1", role = Role.USER, text = "2 + / 3", timestampMillis = 0L)
        )

        val result = engine.generateResponse(history)

        assertTrue(result is AgentResult.Failure)
    }
}
