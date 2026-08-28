package com.jason.mypersonalai.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentResolverSpeedTest {
    @Test fun networkSpeedIsNotDeviceInfo() {
        val result = IntentResolver.resolve("check my network speed")
        assertEquals(IntentResolver.Intent.DEVICE_INFO, result.intent)
        assertEquals("speed_test", result.target)
    }

    @Test fun internetSpeedIsNotNetworkStatus() {
        val result = IntentResolver.resolve("run an internet speed test")
        assertEquals(IntentResolver.Intent.DEVICE_INFO, result.intent)
    }

    @Test fun networkStatusRemainsDeviceInfo() {
        val result = IntentResolver.resolve("what is my network status")
        assertEquals(IntentResolver.Intent.DEVICE_INFO, result.intent)
        assertEquals("network", result.target)
    }
}
