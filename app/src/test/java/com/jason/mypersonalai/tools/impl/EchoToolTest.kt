package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoToolTest {

    private val tool = EchoTool()

    @Test
    fun `echoes input back unchanged`() = runTest {
        val result = tool.execute(ToolInput(raw = "hello there"))
        assertTrue(result is ToolExecutionResult.Success)
        assertEquals("hello there", (result as ToolExecutionResult.Success).output)
    }

    @Test
    fun `empty input is a structured failure`() = runTest {
        val result = tool.execute(ToolInput(raw = "   "))
        assertTrue(result is ToolExecutionResult.Failure)
    }
}
