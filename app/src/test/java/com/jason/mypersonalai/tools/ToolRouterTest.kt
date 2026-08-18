package com.jason.mypersonalai.tools

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class EchoStubTool : Tool {
    override val id: String = "echo-stub"
    override val description: String = "stub"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override suspend fun execute(input: ToolInput): ToolExecutionResult =
        ToolExecutionResult.Success(output = input.raw)
}

private class ThrowingStubTool : Tool {
    override val id: String = "throwing-stub"
    override val description: String = "stub"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override suspend fun execute(input: ToolInput): ToolExecutionResult =
        throw RuntimeException("boom")
}

class ToolRouterTest {

    @Test
    fun `routes to the correct tool and returns its result`() = runTest {
        val registry = ToolRegistry().apply { register(EchoStubTool()) }
        val router = ToolRouter(registry)

        val result = router.route("echo-stub", ToolInput(raw = "hello"))

        assertTrue(result is ToolExecutionResult.Success)
        assertEquals("hello", (result as ToolExecutionResult.Success).output)
    }

    @Test
    fun `unknown tool id returns a structured failure, not a crash`() = runTest {
        val router = ToolRouter(ToolRegistry())

        val result = router.route("does-not-exist", ToolInput(raw = "x"))

        assertTrue(result is ToolExecutionResult.Failure)
    }

    @Test
    fun `a tool throwing is caught and converted to a structured failure`() = runTest {
        val registry = ToolRegistry().apply { register(ThrowingStubTool()) }
        val router = ToolRouter(registry)

        val result = router.route("throwing-stub", ToolInput(raw = "x"))

        assertTrue(result is ToolExecutionResult.Failure)
    }
}
