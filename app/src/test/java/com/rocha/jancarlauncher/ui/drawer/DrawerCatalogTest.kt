package com.rocha.jancarlauncher.ui.drawer

import com.rocha.jancarlauncher.launch.AppTargets
import com.rocha.jancarlauncher.launch.Kind
import com.rocha.jancarlauncher.launch.PackageGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerCatalogTest {
    private class FakeGateway(
        private val installed: Set<String> = emptySet(),
        private val disabled: Set<String> = emptySet(),
    ) : PackageGateway {
        override fun isInstalled(pkg: String) = pkg in installed
        override fun isEnabled(pkg: String) = pkg !in disabled
    }

    private fun entry(id: String) = DrawerCatalog.entries.first { it.id == id }

    @Test fun hasFourteenEntries_inPreviewOrder() {
        // Vídeo veio da Home (o bloco virou Chamadas) e fica ao lado da Galeria.
        assertEquals(
            listOf("VLC", "NewPipe", "F-Droid", "Lightning", "Galeria", "Vídeo", "Arquivos", "Bluetooth", "Música BT", "EQ", "Veículo", "Scrcpy", "OBD-II", "Jogos"),
            DrawerCatalog.entries.map { it.name },
        )
    }

    @Test fun tags_matchPreview() {
        val apps = listOf("vlc", "newpipe", "fdroid", "lightning", "scrcpy", "obd", "retroarch")
        val system = listOf("gallery", "video", "files", "bluetooth", "bt_music", "eq", "vehicle")
        assertTrue(apps.all { entry(it).tag == Kind.App })
        assertTrue(system.all { entry(it).tag == Kind.System })
    }

    @Test fun idsAreUnique() = assertEquals(14, DrawerCatalog.entries.map { it.id }.toSet().size)

    @Test fun videoEntry_opensJancarVideo() = assertEquals(AppTargets.VIDEO, entry("video").target)

    @Test fun knownPackages_areConfigured() {
        assertEquals("org.videolan.vlc", entry("vlc").target?.pkg)
        assertEquals("org.schabi.newpipe", entry("newpipe").target?.pkg)
        assertEquals("org.fdroid.fdroid", entry("fdroid").target?.pkg)
        assertEquals("acr.browser.lightning", entry("lightning").target?.pkg)
    }

    @Test fun bluetoothEntry_opensSystemSettings_andIsAlwaysAvailable() {
        val bt = entry("bluetooth")
        assertEquals("android.settings.BLUETOOTH_SETTINGS", bt.settingsAction)
        assertTrue(DrawerCatalog.isAvailable(bt, FakeGateway()))
    }

    @Test fun unconfiguredEntry_isNotAvailable() {
        assertFalse(DrawerCatalog.isAvailable(entry("scrcpy"), FakeGateway(installed = setOf("anything"))))
    }

    @Test fun installedEntry_isAvailable_missingOrDisabledIsNot() {
        val vlc = entry("vlc")
        assertTrue(DrawerCatalog.isAvailable(vlc, FakeGateway(installed = setOf("org.videolan.vlc"))))
        assertFalse(DrawerCatalog.isAvailable(vlc, FakeGateway()))
        assertFalse(DrawerCatalog.isAvailable(vlc, FakeGateway(installed = setOf("org.videolan.vlc"), disabled = setOf("org.videolan.vlc"))))
    }

    @Test fun installedLabel_usesSingularAndPlural() {
        assertEquals("0 apps instalados", DrawerCatalog.installedLabel(0))
        assertEquals("1 app instalado", DrawerCatalog.installedLabel(1))
        assertEquals("12 apps instalados", DrawerCatalog.installedLabel(12))
    }

    @Test fun installedCount_countsOnlyAvailableEntries() {
        val gateway = FakeGateway(installed = setOf("org.videolan.vlc", "org.schabi.newpipe"))
        // VLC + NewPipe + Bluetooth (atalho do sistema, sempre disponível)
        assertEquals(3, DrawerCatalog.installedCount(gateway))
    }

    @Test fun retroArch_acceptsThe32BitPackage() {
        val e = entry("retroarch")
        val g = FakeGateway(installed = setOf("com.retroarch.ra32"))
        assertTrue(DrawerCatalog.isAvailable(e, g))
        assertEquals("com.retroarch.ra32", DrawerCatalog.resolved(e, g).target?.pkg)
    }

    @Test fun retroArch_missingStaysOnMainPackage() {
        val e = entry("retroarch")
        assertFalse(DrawerCatalog.isAvailable(e, FakeGateway()))
        assertEquals("com.retroarch", DrawerCatalog.resolved(e, FakeGateway()).target?.pkg)
    }
}
