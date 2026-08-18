package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorToolTest {

    private val tool = CalculatorTool()

    @Test
    fun `simple addition`() = runTest {
        val result = tool.execute(ToolInput(raw = "2 + 2"))
        assertTrue(result is ToolExecutionResult.Success)
        assertEquals("4", (result as ToolExecutionResult.Success).output)
    }

    @Test
    fun `respects operator precedence`() = runTest {
        val result = tool.execute(ToolInput(raw = "2+5*3"))
        assertTrue(result is ToolExecutionResult.Success)
        assertEquals("17", (result as ToolExecutionResult.Success).output)
    }

    @Test
    fun `parentheses override precedence`() = runTest {
        val result = tool.execute(ToolInput(raw = "(4 + 6) / 2"))
        assertTrue(result is ToolExecutionResult.Success)
        assertEquals("5", (result as ToolExecutionResult.Success).output)
    }

    @Test
    fun `decimal result is preserved`() = runTest {
        val result = tool.execute(ToolInput(raw = "10 - 3.5"))
        assertTrue(result is ToolExecutionResult.Success)
        assertEquals("6.5", (result as ToolExecutionResult.Success).output)
    }

    @Test
    fun `division by zero is a structured failure`() = runTest {
        val result = tool.execute(ToolInput(raw = "5 / 0"))
        assertTrue(result is ToolExecutionResult.Failure)
    }

    @Test
    fun `malformed expression is a structured failure`() = runTest {
        val result = tool.execute(ToolInput(raw = "2 + / 3"))
        assertTrue(result is ToolExecutionResult.Failure)
    }

    @Test
    fun `unbalanced parentheses is a structured failure`() = runTest {
        val result = tool.execute(ToolInput(raw = "(4 + 6"))
        assertTrue(result is ToolExecutionResult.Failure)
    }
}
