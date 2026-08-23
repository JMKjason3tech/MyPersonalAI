package com.jason.mypersonalai.tools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmationPolicyTest {

    @Test
    fun `low risk does not require confirmation`() {
        assertFalse(ConfirmationPolicy.requiresConfirmation(RiskLevel.LOW))
    }

    @Test
    fun `medium risk requires confirmation`() {
        assertTrue(ConfirmationPolicy.requiresConfirmation(RiskLevel.MEDIUM))
    }

    @Test
    fun `high risk requires confirmation`() {
        assertTrue(ConfirmationPolicy.requiresConfirmation(RiskLevel.HIGH))
    }
}
