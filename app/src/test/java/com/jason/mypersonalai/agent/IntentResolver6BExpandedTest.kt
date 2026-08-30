package com.jason.mypersonalai.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentResolver6BExpandedTest {
    @Test fun stopSpeakingIsExplicit() = assertEquals(IntentResolver.Intent.STOP_SPEAKING, IntentResolver.resolve("stop speaking").intent)
    @Test fun callRequestsUseContactsCapability() = assertEquals(IntentResolver.Intent.CONTACTS, IntentResolver.resolve("call John").intent)
    @Test fun missedCallsUseCallLogCapability() = assertEquals(IntentResolver.Intent.CALL_LOG, IntentResolver.resolve("show my missed calls").intent)
    @Test fun filesUseFilesCapability() = assertEquals(IntentResolver.Intent.FILES, IntentResolver.resolve("open my documents").intent)
    @Test fun mediaUsesMediaCapability() = assertEquals(IntentResolver.Intent.MEDIA, IntentResolver.resolve("open my pictures").intent)
    @Test fun bluetoothControlUsesConnectivityCapability() = assertEquals(IntentResolver.Intent.WIFI_BLUETOOTH, IntentResolver.resolve("scan nearby bluetooth devices").intent)
    @Test fun volumeRelativeCommandsResolve() = assertEquals(IntentResolver.Intent.DEVICE_CONTROL, IntentResolver.resolve("make the volume louder").intent)
    @Test fun brightnessRelativeCommandsResolve() = assertEquals(IntentResolver.Intent.DEVICE_CONTROL, IntentResolver.resolve("make the screen dimmer").intent)
}
