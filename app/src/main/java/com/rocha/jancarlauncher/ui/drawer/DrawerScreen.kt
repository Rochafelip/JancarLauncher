package com.rocha.jancarlauncher.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.R
import com.rocha.jancarlauncher.launch.Kind
import com.rocha.jancarlauncher.launch.PackageGateway
import com.rocha.jancarlauncher.ui.UiActions
import com.rocha.jancarlauncher.ui.components.AppTile
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.components.OnPhotoSurfaces
import com.rocha.jancarlauncher.ui.components.PhotoBackdrop
import com.rocha.jancarlauncher.ui.components.Pressable
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import com.rocha.jancarlauncher.ui.theme.screenStyles
import com.rocha.jancarlauncher.ui.theme.ScreenStyles

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

/** Grupos da versão "Por grupos" (ids do `DrawerCatalog`). */
private val GROUPS = listOf(
    "MÍDIA" to listOf("vlc", "newpipe", "gallery", "bt_music", "eq"),
    "CARRO" to listOf("vehicle", "obd", "bluetooth"),
    "FERRAMENTAS" to listOf("files", "fdroid", "lightning", "scrcpy"),
)

/** Um app da gaveta pronto para desenhar: ícone, nome, etiqueta e se está instalado. */
private class DrawerItem(val entry: DrawerEntry, val enabled: Boolean, val onClick: () -> Unit) {
    val icon get() = entry.icon.vector()
    val tag get() = when {
        !enabled -> "NÃO INSTALADO"
        entry.tag == Kind.App -> "APP"
        else -> "SISTEMA"
    }
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
    val items = DrawerCatalog.entries.map { e -> DrawerItem(e, availability.getValue(e.id)) { actions.openDrawerEntry(e) } }
    // Versão escolhida na galeria (aba Gaveta).
    val style = screenStyles().drawer

    Box(Modifier.fillMaxSize().background(c.bg)) {
        if (style == ScreenStyles.Drawer.Pilulas) {
            // Pílulas: o carro aparece em cima; escurece só onde ficam os apps.
            Image(painterResource(R.drawable.home_wallpaper), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to c.bg.copy(alpha = .80f), .22f to c.bg.copy(alpha = .15f), .45f to c.bg.copy(alpha = .30f),
                        .70f to c.bg.copy(alpha = .90f), 1f to c.bg.copy(alpha = .94f),
                    ),
                ),
            )
        } else {
            PhotoBackdrop(dim = if (style == ScreenStyles.Drawer.Grupos) 0.80f else 0.84f)
        }
        OnPhotoSurfaces {
            when (style) {
                ScreenStyles.Drawer.Atual -> DrawerAtual(items, installed, onToggleTheme, onBack)
                ScreenStyles.Drawer.Grade -> DrawerGrade(items, installed, onBack)
                ScreenStyles.Drawer.Grupos -> DrawerGrupos(items, onBack)
                ScreenStyles.Drawer.Pilulas -> DrawerPilulas(items, onBack)
            }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String?, onBack: () -> Unit, round: Boolean = false) {
    val c = JTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        JIconButton(JIcons.Back, "Voltar para a tela inicial", onBack, size = 52.dp, iconSize = 22.dp, shape = if (round) CircleShape else RoundedCornerShape(14.dp))
        BasicText(title, style = JancarType.h1.copy(color = c.fg, fontSize = 26.sp))
        if (subtitle != null) BasicText(subtitle, style = JancarType.body.copy(color = c.muted, fontSize = 15.sp))
    }
}

/** Atual: 6 colunas em cima. */
@Composable
private fun DrawerAtual(items: List<DrawerItem>, installed: Int, onToggleTheme: () -> Unit, onBack: () -> Unit) {
    val c = JTheme.colors
    Column(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp, top = 44.dp, bottom = 40.dp)) {
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
            items.chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { item ->
                        AppTile(
                            icon = item.icon, name = item.entry.name,
                            tag = if (item.entry.tag == Kind.App) "App" else "Sistema",
                            enabled = item.enabled, onClick = item.onClick, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Grade grande: 4×3 blocos na tela toda; app não instalado diz "NÃO INSTALADO". */
@Composable
private fun DrawerGrade(items: List<DrawerItem>, installed: Int, onBack: () -> Unit) {
    val c = JTheme.colors
    Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 22.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header("Todos os apps", "$installed de ${items.size} instalados", onBack)
        items.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { item ->
                    Pressable(onClick = item.onClick, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).fillMaxHeight().alpha(if (item.enabled) 1f else 0.5f), description = item.entry.name) { _, _ ->
                        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(Modifier.size(56.dp).background(c.surfaceHover, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                JIcon(item.icon, c.icon, 28.dp)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                BasicText(item.entry.name, style = JancarType.tileLabel.copy(color = c.fg, fontSize = 19.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                BasicText(item.tag, style = JancarType.appTag.copy(color = c.muted, letterSpacing = 0.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Por grupos: Mídia, Carro e Ferramentas em listas. */
@Composable
private fun DrawerGrupos(items: List<DrawerItem>, onBack: () -> Unit) {
    val c = JTheme.colors
    val byId = items.associateBy { it.entry.id }
    // Algum app que não esteja em grupo nenhum (catálogo mudou) vai para Ferramentas.
    val grouped = GROUPS.map { (t, ids) -> t to ids.mapNotNull { byId[it] } }.toMutableList()
    val leftovers = items.filter { i -> GROUPS.none { i.entry.id in it.second } }
    if (leftovers.isNotEmpty()) grouped[2] = grouped[2].first to (grouped[2].second + leftovers)
    Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 22.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Header("Todos os apps", null, onBack)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            grouped.forEach { (title, list) ->
                Column(
                    Modifier.weight(1f).fillMaxHeight().background(c.surface, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BasicText(title, style = JancarType.caption.copy(color = c.muted, fontSize = 12.sp), modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                    list.forEach { item ->
                        Pressable(onClick = item.onClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(58.dp).alpha(if (item.enabled) 1f else 0.5f), description = item.entry.name) { _, _ ->
                            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(40.dp).background(c.surfaceHover, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    JIcon(item.icon, c.icon, 22.dp)
                                }
                                BasicText(item.entry.name, style = JancarType.body.copy(color = c.fg), modifier = Modifier.weight(1f), maxLines = 1)
                                if (!item.enabled) BasicText("NÃO INSTALADO", style = JancarType.appTag.copy(color = c.faint, fontSize = 11.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Pílulas sobre o carro: o carro aparece em cima, os apps em pílulas embaixo. */
@Composable
private fun BoxScope.DrawerPilulas(items: List<DrawerItem>, onBack: () -> Unit) {
    val c = JTheme.colors
    Box(Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 22.dp)) {
        Header("Todos os apps", null, onBack, round = true)
    }
    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 40.dp, end = 40.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    Pressable(onClick = item.onClick, shape = CircleShape, modifier = Modifier.height(62.dp).alpha(if (item.enabled) 1f else 0.5f), description = item.entry.name) { _, _ ->
                        Row(Modifier.padding(start = 9.dp, end = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(44.dp).background(c.surfaceHover, CircleShape), contentAlignment = Alignment.Center) {
                                JIcon(item.icon, c.icon, 22.dp)
                            }
                            BasicText(item.entry.name, style = JancarType.body.copy(color = c.fg), maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.width(0.dp))
            }
        }
    }
}
