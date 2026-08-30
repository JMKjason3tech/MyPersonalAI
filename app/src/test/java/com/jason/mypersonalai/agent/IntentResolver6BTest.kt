package com.jason.mypersonalai.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentResolver6BTest {
    @Test fun brightnessCommandsResolveToDeviceControl() {
        assertEquals(IntentResolver.Intent.DEVICE_CONTROL, IntentResolver.resolve("increase brightness").intent)
        assertEquals("brightness", IntentResolver.resolve("set brightness to 50%").target)
    }
    @Test fun volumeCommandsResolveToDeviceControl() {
        assertEquals(IntentResolver.Intent.DEVICE_CONTROL, IntentResolver.resolve("turn volume up").intent)
        assertEquals("volume", IntentResolver.resolve("set volume to 50%").target)
    }
    @Test fun appCommandsResolveToOpenApp() {
        val result = IntentResolver.resolve("open WhatsApp")
        assertEquals(IntentResolver.Intent.OPEN_APP, result.intent)
        assertEquals("whatsapp", result.target)
    }
}
