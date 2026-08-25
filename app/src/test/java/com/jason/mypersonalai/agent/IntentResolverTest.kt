package com.jason.mypersonalai.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentResolverTest {

    @Test
    fun configurationSynonymsOpenSettings() {
        val requests = listOf(
            "modify my wifi settings",
            "configure my wireless connection",
            "adjust my battery settings",
            "manage my notification settings",
            "customize my display",
            "change my sound settings"
        )

        requests.forEach { request ->
            val result = IntentResolver.resolve(request)
            assertEquals(IntentResolver.Intent.OPEN_SETTINGS, result.intent)
            assertTrue(result.confidence >= 95)
        }
    }

    @Test
    fun navigationSynonymsOpenSettings() {
        val requests = listOf(
            "open wifi settings",
            "access bluetooth settings",
            "take me to storage settings",
            "show me accessibility settings",
            "go to security settings",
            "bring up date and time"
        )

        requests.forEach { request ->
            assertEquals(
                IntentResolver.Intent.OPEN_SETTINGS,
                IntentResolver.resolve(request).intent
            )
        }
    }

    @Test
    fun arbitraryWordsDoNotFallBackToDeviceInfo() {
        val requests = listOf(
            "hello battery",
            "please battery now",
            "something about storage for me",
            "random network sentence"
        )

        requests.forEach { request ->
            assertEquals(
                IntentResolver.Intent.UNKNOWN,
                IntentResolver.resolve(request).intent
            )
        }
    }

    @Test
    fun simpleDeviceTargetsRemainDeviceInfo() {
        assertEquals(IntentResolver.Intent.DEVICE_INFO, IntentResolver.resolve("battery").intent)
        assertEquals(IntentResolver.Intent.DEVICE_INFO, IntentResolver.resolve("storage").intent)
        assertEquals(IntentResolver.Intent.DEVICE_INFO, IntentResolver.resolve("wifi").intent)
        assertEquals(IntentResolver.Intent.DEVICE_INFO, IntentResolver.resolve("device info").intent)
    }

    @Test
    fun informationRequestsRemainDeviceInfo() {
        val requests = listOf(
            "check my battery",
            "how much storage do I have",
            "what is my network status",
            "give me my device information"
        )

        requests.forEach { request ->
            assertEquals(
                IntentResolver.Intent.DEVICE_INFO,
                IntentResolver.resolve(request).intent
            )
        }
    }
}
