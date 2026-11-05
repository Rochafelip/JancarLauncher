package com.rocha.jancarlauncher.ui.drawer

import com.rocha.jancarlauncher.launch.AppTarget
import com.rocha.jancarlauncher.launch.Kind
import com.rocha.jancarlauncher.launch.PackageGateway
import com.rocha.jancarlauncher.launch.SettingsActions

enum class DrawerIcon { Vlc, NewPipe, FDroid, Lightning, Gallery, Folder, Bluetooth, BtMusic, Eq, Vehicle, Scrcpy, Obd }

/**
 * Um item da gaveta. Exatamente um destes: [target] (app), [settingsAction] (tela de Settings) ou nenhum
 * (ainda não configurado — pacote a descobrir na Fase 0).
 */
data class DrawerEntry(
    val id: String,
    val name: String,
    val tag: Kind,
    val icon: DrawerIcon,
    val target: AppTarget? = null,
    val settingsAction: String? = null,
)

object DrawerCatalog {
    private fun app(id: String, name: String, pkg: String) = AppTarget(id, name, Kind.App, pkg, null)

    /** Ordem e tags do preview visual. Pacotes só onde são conhecidos; o resto fica "não configurado". */
    val entries: List<DrawerEntry> = listOf(
        DrawerEntry("vlc", "VLC", Kind.App, DrawerIcon.Vlc, app("vlc", "VLC", "org.videolan.vlc")),
        DrawerEntry("newpipe", "NewPipe", Kind.App, DrawerIcon.NewPipe, app("newpipe", "NewPipe", "org.schabi.newpipe")),
        DrawerEntry("fdroid", "F-Droid", Kind.App, DrawerIcon.FDroid, app("fdroid", "F-Droid", "org.fdroid.fdroid")),
        DrawerEntry("lightning", "Lightning", Kind.App, DrawerIcon.Lightning, app("lightning", "Lightning", "acr.browser.lightning")),
        DrawerEntry("gallery", "Galeria", Kind.System, DrawerIcon.Gallery),
        DrawerEntry("files", "Arquivos", Kind.System, DrawerIcon.Folder),
        DrawerEntry("bluetooth", "Bluetooth", Kind.System, DrawerIcon.Bluetooth, settingsAction = SettingsActions.BLUETOOTH),
        DrawerEntry("bt_music", "Música BT", Kind.System, DrawerIcon.BtMusic),
        DrawerEntry("eq", "EQ", Kind.System, DrawerIcon.Eq),
        DrawerEntry("vehicle", "Veículo", Kind.System, DrawerIcon.Vehicle),
        DrawerEntry("scrcpy", "Scrcpy", Kind.App, DrawerIcon.Scrcpy),
        DrawerEntry("obd", "OBD-II", Kind.App, DrawerIcon.Obd),
    )

    fun isAvailable(entry: DrawerEntry, gateway: PackageGateway): Boolean {
        if (entry.settingsAction != null) return true
        val pkg = entry.target?.pkg ?: return false
        return gateway.isInstalled(pkg) && gateway.isEnabled(pkg)
    }

    fun installedCount(gateway: PackageGateway): Int = entries.count { isAvailable(it, gateway) }

    fun installedLabel(count: Int): String = if (count == 1) "1 app instalado" else "$count apps instalados"
}
