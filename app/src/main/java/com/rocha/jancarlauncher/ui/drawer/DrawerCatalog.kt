package com.rocha.jancarlauncher.ui.drawer

import com.rocha.jancarlauncher.launch.AppTarget
import com.rocha.jancarlauncher.launch.AppTargets
import com.rocha.jancarlauncher.launch.Kind
import com.rocha.jancarlauncher.launch.PackageGateway
import com.rocha.jancarlauncher.launch.SettingsActions

enum class DrawerIcon { Vlc, NewPipe, FDroid, Lightning, Gallery, Video, Folder, Bluetooth, BtMusic, Eq, Vehicle, Scrcpy, Obd, Games }

/**
 * Um item da gaveta. Exatamente um destes: [target] (app), [settingsAction] (tela de Settings) ou nenhum
 * (ainda não configurado -- pacote a descobrir na Fase 0).
 */
data class DrawerEntry(
    val id: String,
    val name: String,
    val tag: Kind,
    val icon: DrawerIcon,
    val target: AppTarget? = null,
    val settingsAction: String? = null,
    /** Outros pacotes do mesmo app (ex.: o RetroArch de 32 bits tem outro nome); vale o primeiro instalado. */
    val altPkgs: List<String> = emptyList(),
)

object DrawerCatalog {
    private fun app(id: String, name: String, pkg: String) = AppTarget(id, name, Kind.App, pkg, null)

    /** Ordem e tags do preview visual. Todos os pacotes confirmados via dumpsys na central (Fase 0 concluída). */
    val entries: List<DrawerEntry> = listOf(
        DrawerEntry("vlc", "VLC", Kind.App, DrawerIcon.Vlc, app("vlc", "VLC", "org.videolan.vlc")),
        DrawerEntry("newpipe", "NewPipe", Kind.App, DrawerIcon.NewPipe, app("newpipe", "NewPipe", "org.schabi.newpipe")),
        DrawerEntry("fdroid", "F-Droid", Kind.App, DrawerIcon.FDroid, app("fdroid", "F-Droid", "org.fdroid.fdroid")),
        DrawerEntry("lightning", "Lightning", Kind.App, DrawerIcon.Lightning, app("lightning", "Lightning", "acr.browser.lightning")),
        DrawerEntry("gallery", "Galeria", Kind.System, DrawerIcon.Gallery, AppTargets.GALLERY),
        // Saiu da Home em 2026-11-15 (o bloco virou Chamadas).
        DrawerEntry("video", "Vídeo", Kind.System, DrawerIcon.Video, AppTargets.VIDEO),
        DrawerEntry("files", "Arquivos", Kind.System, DrawerIcon.Folder, AppTargets.FILES),
        DrawerEntry("bluetooth", "Bluetooth", Kind.System, DrawerIcon.Bluetooth, settingsAction = SettingsActions.BLUETOOTH),
        DrawerEntry("bt_music", "Música BT", Kind.System, DrawerIcon.BtMusic, AppTargets.BT_MUSIC),
        DrawerEntry("eq", "EQ", Kind.System, DrawerIcon.Eq, AppTargets.EQ),
        DrawerEntry("vehicle", "Veículo", Kind.System, DrawerIcon.Vehicle, AppTargets.VEHICLE),
        DrawerEntry("scrcpy", "Scrcpy", Kind.App, DrawerIcon.Scrcpy, AppTargets.SCRCPY),
        DrawerEntry("obd", "OBD-II", Kind.App, DrawerIcon.Obd, AppTargets.OBD),
        // Jogos retrô: só abre o RetroArch (instalado por fora, jogos no pendrive).
        DrawerEntry("retroarch", "Jogos", Kind.App, DrawerIcon.Games, app("retroarch", "Jogos", "com.retroarch"), altPkgs = listOf("com.retroarch.ra32", "com.retroarch.aarch64")),
    )

    /** Pacotes que valem para o item: o principal e os alternativos. */
    private fun packages(entry: DrawerEntry): List<String> = listOfNotNull(entry.target?.pkg) + entry.altPkgs

    fun isAvailable(entry: DrawerEntry, gateway: PackageGateway): Boolean {
        if (entry.settingsAction != null) return true
        return packages(entry).any { gateway.isInstalled(it) && gateway.isEnabled(it) }
    }

    /** O item apontando para o pacote que está instalado (o principal se nenhum estiver). */
    fun resolved(entry: DrawerEntry, gateway: PackageGateway): DrawerEntry {
        val target = entry.target ?: return entry
        val pkg = packages(entry).firstOrNull { gateway.isInstalled(it) && gateway.isEnabled(it) } ?: return entry
        return if (pkg == target.pkg) entry else entry.copy(target = target.copy(pkg = pkg))
    }

    fun installedCount(gateway: PackageGateway): Int = entries.count { isAvailable(it, gateway) }

    fun installedLabel(count: Int): String = if (count == 1) "1 app instalado" else "$count apps instalados"
}
