package com.rocha.jancarlauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchControllerTest {
    private class RecordingStarter : ActivityStarter {
        val components = mutableListOf<Pair<String, String>>()
        override fun startComponent(pkg: String, activityClass: String) { components += pkg to activityClass }
        override fun startLaunchIntentFor(pkg: String) = true
    }

    private val allInstalled = object : PackageGateway {
        override fun isInstalled(pkg: String) = true
        override fun isEnabled(pkg: String) = true
    }

    private fun controller(starter: ActivityStarter, phones: List<String>, usb: Boolean = false) =
        LaunchController(
            launcher = AppLauncher(allInstalled, starter),
            bondedPhones = { phones.map { BondedPhone(it) } },
            usbPhoneAttached = { usb },
        )

    @Test fun openAuto_withAndroidPhone_startsAndroidAuto() {
        val starter = RecordingStarter()
        val result = controller(starter, listOf("Pixel 8")).openAuto()
        assertEquals(LaunchResult.Started, result)
        assertEquals(AppTargets.ANDROID_AUTO.pkg to AppTargets.ANDROID_AUTO.activityClass, starter.components.single())
    }

    @Test fun openAuto_withIphone_startsCarPlay_beforeAnyStartActivity() {
        val starter = RecordingStarter()
        controller(starter, listOf("iPhone 15")).openAuto()
        // Decisão antes do start: só o CarPlay foi iniciado, o Android Auto nunca.
        assertEquals(listOf(AppTargets.CARPLAY.pkg to AppTargets.CARPLAY.activityClass), starter.components)
    }

    @Test fun open_unconfiguredTarget_returnsNotConfigured_withoutStarting() {
        val starter = RecordingStarter()
        // Destino sintético: todos os de AppTargets já estão configurados.
        val unconfigured = AppTarget.unconfigured("music", "Música", Kind.System)
        assertEquals(LaunchResult.NotConfigured, controller(starter, emptyList()).open(unconfigured))
        assertEquals(emptyList<Pair<String, String>>(), starter.components)
    }
}
