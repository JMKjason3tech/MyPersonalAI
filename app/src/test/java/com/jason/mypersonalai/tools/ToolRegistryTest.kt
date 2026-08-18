package com.jason.mypersonalai.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class StubTool(override val id: String) : Tool {
    override val description: String = "stub"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override suspend fun execute(input: ToolInput): ToolExecutionResult =
        ToolExecutionResult.Success(output = input.raw)
}

class ToolRegistryTest {

    @Test
    fun `registered tool can be found by id`() {
        val registry = ToolRegistry()
        val tool = StubTool(id = "stub")

        registry.register(tool)

        assertEquals(tool, registry.find("stub"))
    }

    @Test
    fun `unregistered tool id returns null`() {
        val registry = ToolRegistry()

        assertNull(registry.find("nonexistent"))
    }

    @Test
    fun `registering a duplicate id throws`() {
        val registry = ToolRegistry()
        registry.register(StubTool(id = "stub"))

        var threw = false
        try {
            registry.register(StubTool(id = "stub"))
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `all returns every registered tool`() {
        val registry = ToolRegistry()
        registry.register(StubTool(id = "a"))
        registry.register(StubTool(id = "b"))

        assertEquals(2, registry.all().size)
    }
}
