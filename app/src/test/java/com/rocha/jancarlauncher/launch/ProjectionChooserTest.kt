package com.rocha.jancarlauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectionChooserTest {
    private fun choose(
        phones: List<String?> = emptyList(),
        usb: Boolean = false,
        override: Projection? = null,
    ) = ProjectionChooser.choose(ProjectionInput(phones.map { BondedPhone(it) }, usb, override))

    @Test fun pairedIphone_choosesCarPlay() = assertEquals(Projection.CarPlay, choose(phones = listOf("iPhone do Felipe")))

    @Test fun pairedIpadOrApple_choosesCarPlay() {
        assertEquals(Projection.CarPlay, choose(phones = listOf("Meu iPad")))
        assertEquals(Projection.CarPlay, choose(phones = listOf("Apple Phone")))
    }

    @Test fun matchingIsCaseInsensitive() = assertEquals(Projection.CarPlay, choose(phones = listOf("IPHONE 15")))

    @Test fun pairedAndroid_choosesAndroidAuto() = assertEquals(Projection.AndroidAuto, choose(phones = listOf("Galaxy S23")))

    @Test fun noPairedPhones_choosesAndroidAuto() = assertEquals(Projection.AndroidAuto, choose())

    @Test fun noPairedPhones_withUsbPhone_choosesAndroidAuto() = assertEquals(Projection.AndroidAuto, choose(usb = true))

    @Test fun nullNames_areIgnored() = assertEquals(Projection.AndroidAuto, choose(phones = listOf(null, "Pixel 8")))

    @Test fun anyIphoneAmongSeveral_choosesCarPlay() =
        assertEquals(Projection.CarPlay, choose(phones = listOf("Pixel 8", "iPhone 13")))

    @Test fun userOverride_winsOverEverything() {
        assertEquals(Projection.AndroidAuto, choose(phones = listOf("iPhone"), override = Projection.AndroidAuto))
        assertEquals(Projection.CarPlay, choose(phones = listOf("Pixel"), override = Projection.CarPlay))
    }
}
