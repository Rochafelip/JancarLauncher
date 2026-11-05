package com.rocha.jancarlauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppTargetsTest {
    @Test fun androidAuto_pointsToConfirmedActivity() {
        assertEquals("com.google.android.projection.sink", AppTargets.ANDROID_AUTO.pkg)
        assertEquals("com.google.android.projection.sink.ui.AndroidAutoActivity", AppTargets.ANDROID_AUTO.activityClass)
    }

    @Test fun carPlay_usesAppPackage_neverBackgroundService() {
        assertEquals("com.autochips.carplayapp", AppTargets.CARPLAY.pkg)
        assertEquals("com.autochips.carplayapp.CarPlayActivity", AppTargets.CARPLAY.activityClass)
        // com.autochips.carplay (sem "app") é só serviço de fundo: jamais pode aparecer na tabela.
        assertFalse(AppTargets.all.any { it.pkg == "com.autochips.carplay" })
    }

    @Test fun radio_pointsToConfirmedActivity() {
        assertEquals("com.jancar.radio", AppTargets.RADIO.pkg)
        assertEquals("com.jancar.radio.activity.RadioMainActivity", AppTargets.RADIO.activityClass)
    }

    @Test fun musicAndVideo_areUnconfiguredUntilUserProvidesActivities() {
        assertNull(AppTargets.MUSIC.pkg)
        assertNull(AppTargets.MUSIC.activityClass)
        assertNull(AppTargets.VIDEO.pkg)
        assertNull(AppTargets.VIDEO.activityClass)
    }

    @Test fun backCamera_isLaunchedByPackageOnly() {
        assertEquals("com.autochips.backcarapp", AppTargets.BACK_CAMERA.pkg)
        assertNull(AppTargets.BACK_CAMERA.activityClass)
    }

    @Test fun ids_areUnique_andLookupWorks() {
        assertEquals(AppTargets.all.size, AppTargets.all.map { it.id }.toSet().size)
        assertNotNull(AppTargets.byId("radio"))
    }
}
