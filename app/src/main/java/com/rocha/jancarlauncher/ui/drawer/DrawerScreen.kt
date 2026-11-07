package com.rocha.jancarlauncher.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.launch.Kind
import com.rocha.jancarlauncher.launch.PackageGateway
import com.rocha.jancarlauncher.ui.UiActions
import com.rocha.jancarlauncher.ui.components.AppTile
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

private fun DrawerIcon.vector(): ImageVector = when (this) {
    DrawerIcon.Vlc -> JIcons.Vlc
    DrawerIcon.NewPipe -> JIcons.NewPipe
    DrawerIcon.FDroid -> JIcons.FDroid
    DrawerIcon.Lightning -> JIcons.Lightning
    DrawerIcon.Gallery -> JIcons.Gallery
    DrawerIcon.Folder -> JIcons.Folder
    DrawerIcon.Bluetooth -> JIcons.Bluetooth
    DrawerIcon.BtMusic -> JIcons.BtMusic
    DrawerIcon.Eq -> JIcons.Eq
    DrawerIcon.Vehicle -> JIcons.Vehicle
    DrawerIcon.Scrcpy -> JIcons.Scrcpy
    DrawerIcon.Obd -> JIcons.Obd
}

@Composable
fun DrawerScreen(
    isDark: Boolean,
    gateway: PackageGateway,
    actions: UiActions,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
) {
    val c = JTheme.colors
    // Recalculado cada vez que a gaveta abre (apps podem ter sido instalados/desativados).
    val availability = remember(gateway) { DrawerCatalog.entries.associate { it.id to DrawerCatalog.isAvailable(it, gateway) } }
    val installed = availability.values.count { it }

    Column(Modifier.fillMaxSize().background(c.bg).padding(start = 64.dp, end = 64.dp, top = 44.dp, bottom = 40.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 34.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                JIconButton(JIcons.Back, "Voltar para a tela inicial", onBack)
                Column {
                    BasicText("Todos os apps", style = JancarType.h1.copy(color = c.fg))
                    BasicText(DrawerCatalog.installedLabel(installed), style = JancarType.body.copy(color = c.muted), modifier = Modifier.padding(top = 2.dp))
                }
            }
            JIconButton(JIcons.ThemeToggle, "Alternar tema claro/escuro", onToggleTheme)
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DrawerCatalog.entries.chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { entry ->
                        AppTile(
                            icon = entry.icon.vector(),
                            name = entry.name,
                            tag = if (entry.tag == Kind.App) "App" else "Sistema",
                            enabled = availability.getValue(entry.id),
                            onClick = { actions.openDrawerEntry(entry) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f))
    }
}
