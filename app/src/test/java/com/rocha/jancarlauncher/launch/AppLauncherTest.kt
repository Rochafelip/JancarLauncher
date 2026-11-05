package com.rocha.jancarlauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLauncherTest {
    private class FakeGateway(
        private val installed: Set<String> = emptySet(),
        private val disabled: Set<String> = emptySet(),
    ) : PackageGateway {
        override fun isInstalled(pkg: String) = pkg in installed
        override fun isEnabled(pkg: String) = pkg !in disabled
    }

    private class FakeStarter(
        private val throwOnStart: Throwable? = null,
        private val launchIntentAvailable: Boolean = true,
    ) : ActivityStarter {
        val components = mutableListOf<Pair<String, String>>()
        val packages = mutableListOf<String>()
        override fun startComponent(pkg: String, activityClass: String) {
            throwOnStart?.let { throw it }
            components += pkg to activityClass
        }
        override fun startLaunchIntentFor(pkg: String): Boolean {
            throwOnStart?.let { throw it }
            if (launchIntentAvailable) packages += pkg
            return launchIntentAvailable
        }
    }

    private val radio = AppTarget("radio", "Rádio", Kind.System, "com.jancar.radio", "com.jancar.radio.activity.RadioMainActivity")

    @Test fun installedTarget_startsExplicitComponent() {
        val starter = FakeStarter()
        val result = AppLauncher(FakeGateway(installed = setOf("com.jancar.radio")), starter).launch(radio)
        assertEquals(LaunchResult.Started, result)
        assertEquals(listOf("com.jancar.radio" to "com.jancar.radio.activity.RadioMainActivity"), starter.components)
    }

    @Test fun missingPackage_returnsNotInstalled_andDoesNotStart() {
        val starter = FakeStarter()
        val result = AppLauncher(FakeGateway(), starter).launch(radio)
        assertEquals(LaunchResult.NotInstalled, result)
        assertTrue(starter.components.isEmpty())
    }

    @Test fun disabledPackage_returnsDisabled_andDoesNotStart() {
        val starter = FakeStarter()
        val gateway = FakeGateway(installed = setOf("com.jancar.radio"), disabled = setOf("com.jancar.radio"))
        assertEquals(LaunchResult.Disabled, AppLauncher(gateway, starter).launch(radio))
        assertTrue(starter.components.isEmpty())
    }

    @Test fun unconfiguredTarget_returnsNotConfigured() {
        val music = AppTarget.unconfigured("music", "Música", Kind.System)
        assertEquals(LaunchResult.NotConfigured, AppLauncher(FakeGateway(), FakeStarter()).launch(music))
    }

    @Test fun packageOnlyTarget_usesLaunchIntent() {
        val starter = FakeStarter()
        val cam = AppTarget("cam", "Câmera", Kind.System, "com.autochips.backcarapp", null)
        val result = AppLauncher(FakeGateway(installed = setOf("com.autochips.backcarapp")), starter).launch(cam)
        assertEquals(LaunchResult.Started, result)
        assertEquals(listOf("com.autochips.backcarapp"), starter.packages)
    }

    @Test fun packageOnlyTarget_withoutLaunchIntent_returnsNotInstalled() {
        val cam = AppTarget("cam", "Câmera", Kind.System, "com.autochips.backcarapp", null)
        val starter = FakeStarter(launchIntentAvailable = false)
        val result = AppLauncher(FakeGateway(installed = setOf("com.autochips.backcarapp")), starter).launch(cam)
        assertEquals(LaunchResult.NotInstalled, result)
    }

    @Test fun starterThrowingSecurityException_returnsFailed_notCrash() {
        val boom = SecurityException("negado")
        val result = AppLauncher(FakeGateway(installed = setOf("com.jancar.radio")), FakeStarter(throwOnStart = boom)).launch(radio)
        assertEquals(LaunchResult.Failed(boom), result)
    }

    @Test fun starterThrowingActivityNotFound_returnsFailed_notCrash() {
        val boom = android.content.ActivityNotFoundException("sumiu")
        val result = AppLauncher(FakeGateway(installed = setOf("com.jancar.radio")), FakeStarter(throwOnStart = boom)).launch(radio)
        assertEquals(LaunchResult.Failed(boom), result)
    }

    @Test fun starterThrowingRuntimeException_returnsFailed_notCrash() {
        val boom = IllegalStateException("estado ruim")
        val result = AppLauncher(FakeGateway(installed = setOf("com.jancar.radio")), FakeStarter(throwOnStart = boom)).launch(radio)
        assertEquals(LaunchResult.Failed(boom), result)
    }
}
