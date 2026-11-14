package com.rocha.jancarlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.launch.AppTargets
import com.rocha.jancarlauncher.launch.SettingsActions
import com.rocha.jancarlauncher.media.NowPlayingState
import com.rocha.jancarlauncher.system.AdbStatus
import com.rocha.jancarlauncher.system.WifiStatus
import com.rocha.jancarlauncher.ui.UiActions
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.components.MainTile
import com.rocha.jancarlauncher.ui.components.Pressable
import com.rocha.jancarlauncher.ui.components.QuickChip
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/** Um botão da Home: ícone, nome e ação (apps grandes e atalhos). */
data class HomeItem(val key: String, val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * Tudo que a Home mostra e faz, para cada tema ([HomeLayout]) só arrumar as peças do seu jeito: os mesmos dados e as
 * mesmas ações em todos os temas.
 */
class HomeParts(
    val clock: ClockInfo,
    val wifi: WifiStatus,
    val adbStatus: AdbStatus,
    val actions: UiActions,
    val nowPlaying: NowPlayingState,
    val onPlay: () -> Unit,
    val onPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onToggleTheme: () -> Unit,
    /** Toque longo no botão de tema (ou o atalho do painel de brilho): abre a galeria de temas. */
    val onOpenThemes: () -> Unit,
    val onToggleBrightness: () -> Unit,
    val onOpenDrawer: () -> Unit,
    val onOpenDash: () -> Unit,
    val onScreenOff: () -> Unit,
    val onOpenAdbSettings: () -> Unit,
) {
    val apps = listOf(
        HomeItem("auto", JIcons.Auto, "Auto") { actions.openAuto() },
        HomeItem("music", JIcons.Music, "Música") { actions.open(AppTargets.MUSIC) },
        HomeItem("radio", JIcons.Radio, "Rádio") { actions.open(AppTargets.RADIO) },
        HomeItem("video", JIcons.Video, "Vídeo") { actions.open(AppTargets.VIDEO) },
    )
    val shortcuts = listOf(
        HomeItem("wifi", JIcons.Wifi, "Wi-Fi") { actions.openSettings(SettingsActions.WIFI) },
        HomeItem("bt", JIcons.Bluetooth, "Bluetooth") { actions.openSettings(SettingsActions.BLUETOOTH) },
        HomeItem("gps", JIcons.Gps, "GPS") { actions.openSettings(SettingsActions.LOCATION) },
        HomeItem("settings", JIcons.Settings, "Ajustes") { actions.openSettings(SettingsActions.MAIN) },
    )
}

// ---------- Peças ----------

@Composable
fun HomeParts.Clock(timeSize: Int = 92, horizontal: Alignment.Horizontal = Alignment.Start, dateExtra: String? = null) {
    val c = JTheme.colors
    Column(horizontalAlignment = horizontal, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(clock.time, style = JancarType.clockLarge.copy(color = c.fg, fontSize = timeSize.sp, lineHeight = timeSize.sp))
        BasicText(
            if (dateExtra != null) "${clock.date} · $dateExtra" else clock.date,
            style = JancarType.date.copy(color = c.muted),
        )
    }
}

/** Linha de status: Wi-Fi (ícone + nome, toque abre o Wi-Fi), ADB e "MULTIMÍDIA". */
@Composable
fun HomeParts.StatusLine(showLabel: Boolean = true) {
    val c = JTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (wifi.connected) {
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Configurações de Wi-Fi",
                    onClick = { actions.openSettings(SettingsActions.WIFI) },
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                JIcon(JIcons.Wifi, c.muted, 14.dp)
                BasicText(
                    wifi.ssid ?: "Wi-Fi conectado",
                    style = JancarType.appTag.copy(color = c.muted),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        if (adbStatus != AdbStatus.Disabled) {
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenAdbSettings,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                JIcon(JIcons.Usb, c.muted, 13.dp)
                BasicText(
                    if (adbStatus is AdbStatus.EnabledConnected) "ADB conectado" else "ADB ativo",
                    style = JancarType.appTag.copy(color = c.muted),
                )
            }
        }
        if (showLabel) BasicText("MULTIMÍDIA", style = JancarType.caption.copy(color = c.faint))
    }
}

/** Os botões do topo: tema (toque = claro/escuro, segurar = escolher tema), brilho, apps, Painel, apagar tela. */
@Composable
fun HomeParts.TopButtons(size: Dp = Dimens.TopButton, iconSize: Dp = Dimens.TopButtonIcon, vertical: Boolean = false) {
    val buttons: @Composable () -> Unit = {
        JIconButton(
            JIcons.ThemeToggle, "Alternar tema claro/escuro (segure para escolher o tema da Home)", onToggleTheme,
            size = size, iconSize = iconSize, onLongClick = onOpenThemes,
        )
        JIconButton(JIcons.Brightness, "Ajustar brilho", onToggleBrightness, size = size, iconSize = iconSize)
        JIconButton(JIcons.Grid, "Ver todos os apps", onOpenDrawer, size = size, iconSize = iconSize)
        JIconButton(JIcons.Gauge, "Painel de velocidade", onOpenDash, size = size, iconSize = iconSize)
        JIconButton(JIcons.Power, "Apagar tela", onScreenOff, size = size, iconSize = iconSize)
    }
    if (vertical) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { buttons() }
    else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { buttons() }
}

@Composable
fun HomeParts.Chips() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        shortcuts.forEach { QuickChip(it.icon, it.label, it.onClick) }
    }
}

/** Os 4 blocos grandes (o tocado fica meio apagado 0,8 s, como antes, para mostrar que abriu). */
@Composable
fun HomeParts.Tiles(modifier: Modifier = Modifier, spacing: Dp = 18.dp) {
    var opening by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(opening) {
        if (opening != null) { kotlinx.coroutines.delay(800); opening = null }
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
        apps.forEach { app ->
            MainTile(
                app.icon, app.label, { opening = app.key; app.onClick() },
                Modifier.weight(1f).alpha(if (opening == app.key) 0.6f else 1f),
            )
        }
    }
}

@Composable
fun HomeParts.NowPlaying(modifier: Modifier = Modifier) {
    NowPlayingCard(nowPlaying, onPlay, onPause, onNext, onPrevious, modifier)
}

/** Botão do dock: ícone + nome lado a lado, altura de toque folgada (68dp). */
@Composable
fun DockButton(item: HomeItem, modifier: Modifier = Modifier, height: Dp = 68.dp) {
    val c = JTheme.colors
    Pressable(onClick = item.onClick, shape = RoundedCornerShape(16.dp), modifier = modifier.height(height), description = item.label) { pressed, focused ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            JIcon(item.icon, if (pressed || focused) c.accent else c.icon, 28.dp)
            BasicText(item.label, style = JancarType.tileLabel.copy(color = c.fg, fontSize = 19.sp))
        }
    }
}

/** Botão redondo só com ícone (atalhos), com o nome para leitor de tela. */
@Composable
fun RoundIcon(item: HomeItem, size: Dp = 56.dp, iconSize: Dp = 22.dp, onLongClick: (() -> Unit)? = null) {
    val c = JTheme.colors
    Pressable(onClick = item.onClick, shape = CircleShape, modifier = Modifier.size(size), description = item.label, onLongClick = onLongClick) { pressed, _ ->
        JIcon(item.icon, if (pressed) c.fg else c.icon, iconSize)
    }
}

@Composable
private fun Divider(vertical: Boolean = true, length: Dp = 44.dp) {
    val c = JTheme.colors
    Box(
        (if (vertical) Modifier.width(1.dp).height(length) else Modifier.height(1.dp).width(length))
            .background(c.fg.copy(alpha = 0.12f)),
    )
}

/** Faixa de fundo arredondada (dock, barras). */
@Composable
private fun Bar(modifier: Modifier = Modifier, radius: Dp = 26.dp, content: @Composable () -> Unit) {
    val c = JTheme.colors
    val shape = RoundedCornerShape(radius)
    Box(modifier.background(c.bg.copy(alpha = 0.86f), shape).border(1.dp, c.fg.copy(alpha = 0.07f), shape)) { content() }
}

// ---------- Temas (arranjos) ----------

/** Véu de cada tema por cima da foto (alfas de cima para baixo), para o texto ficar legível onde ele está. */
fun HomeLayout.veil(): List<Pair<Float, Float>> = when (this) {
    HomeLayout.Dock -> listOf(0f to .78f, .30f to .15f, .50f to 0f, .72f to .15f, 1f to .80f)
    HomeLayout.Vitrine -> listOf(0f to .85f, .14f to .55f, .26f to 0f, .74f to 0f, 1f to .62f)
    HomeLayout.Coluna -> listOf(0f to .45f, .25f to .10f, 1f to .45f)
    HomeLayout.Tocando -> listOf(0f to .80f, .35f to .45f, .60f to .35f, 1f to .80f)
    HomeLayout.Central -> listOf(0f to .80f, .34f to .30f, .55f to .10f, 1f to .78f)
    HomeLayout.Carro -> listOf(0f to .82f, .34f to .50f, .55f to .30f, 1f to .80f)
    HomeLayout.Grade -> listOf(0f to .55f, .30f to .10f, .65f to .25f, 1f to .88f)
    HomeLayout.Trilho -> listOf(0f to .78f, .30f to .10f, .65f to .05f, 1f to .72f)
    HomeLayout.Widgets -> listOf(0f to .75f, .40f to .30f, .65f to .20f, 1f to .80f)
    HomeLayout.Oito -> listOf(0f to .80f, .32f to .20f, .58f to .10f, 1f to .82f)
    HomeLayout.Pilulas -> listOf(0f to .78f, .30f to .15f, .70f to .15f, 1f to .78f)
    HomeLayout.Atual -> listOf(0f to .82f, .32f to .66f, .55f to .40f, 1f to .75f)
}

/** Desenha o tema escolhido. */
@Composable
fun BoxScope.HomeArrangement(layout: HomeLayout, p: HomeParts) {
    when (layout) {
        HomeLayout.Atual -> LayoutAtual(p)
        HomeLayout.Dock -> LayoutDock(p)
        HomeLayout.Coluna -> LayoutColuna(p)
        HomeLayout.Tocando -> LayoutTocando(p)
        HomeLayout.Central -> LayoutCentral(p)
        HomeLayout.Carro -> LayoutCarro(p)
        HomeLayout.Grade -> LayoutGrade(p)
        HomeLayout.Trilho -> LayoutTrilho(p)
        HomeLayout.Widgets -> LayoutWidgets(p)
        HomeLayout.Oito -> LayoutOito(p)
        HomeLayout.Pilulas -> LayoutPilulas(p)
        HomeLayout.Vitrine -> LayoutVitrine(p)
    }
}

/** Temas que já têm arranjo no app (a galeria apaga os outros com "em breve"). Hoje: todos. */
val READY_LAYOUTS: Set<HomeLayout> = HomeLayout.entries.toSet()

/** A · o arranjo de sempre: relógio e botões em cima, "Tocando agora", atalhos e os 4 blocos embaixo. */
@Composable
private fun LayoutAtual(p: HomeParts) {
    Column(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp, top = 40.dp, bottom = 36.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            p.Clock()
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                p.StatusLine()
                p.TopButtons()
            }
        }
        p.NowPlaying(Modifier.padding(top = 20.dp))
        Spacer(Modifier.weight(1f))
        Box(Modifier.padding(bottom = 22.dp)) { p.Chips() }
        p.Tiles()
    }
}

/** B · dock embaixo: os 4 apps e os atalhos numa barra só; o meio da tela fica livre para o carro. */
@Composable
private fun BoxScope.LayoutDock(p: HomeParts) {
    Row(
        Modifier.fillMaxWidth().align(Alignment.TopStart).padding(start = 48.dp, end = 48.dp, top = 30.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            p.Clock(timeSize = 72)
            p.NowPlaying(Modifier.widthIn(max = 560.dp))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            p.StatusLine(showLabel = false)
            p.TopButtons(size = 56.dp, iconSize = 22.dp)
        }
    }
    Bar(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 22.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            p.apps.forEach { DockButton(it, Modifier.weight(1f)) }
            Divider()
            p.shortcuts.forEach { RoundIcon(it) }
        }
    }
}

/** L · vitrine: o carro em tela cheia, só relógio pequeno em cima e um dock flutuante com tudo. */
@Composable
private fun BoxScope.LayoutVitrine(p: HomeParts) {
    val c = JTheme.colors
    Row(
        Modifier.fillMaxWidth().align(Alignment.TopStart).padding(start = 36.dp, end = 36.dp, top = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(p.clock.time, style = JancarType.clockLarge.copy(color = c.fg, fontSize = 56.sp, lineHeight = 56.sp))
        Spacer(Modifier.width(16.dp))
        BasicText(p.clock.date, style = JancarType.date.copy(color = c.muted), modifier = Modifier.weight(1f))
        p.StatusLine(showLabel = false)
    }
    Bar(Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp), radius = 28.dp) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val square = RoundedCornerShape(20.dp)
            p.apps.forEach { app ->
                JIconButton(app.icon, app.label, app.onClick, size = 64.dp, iconSize = 28.dp, shape = square)
            }
            Divider(length = 40.dp)
            JIconButton(JIcons.Gauge, "Painel de velocidade", p.onOpenDash, size = 64.dp, iconSize = 26.dp, shape = square)
            JIconButton(JIcons.Grid, "Ver todos os apps", p.onOpenDrawer, size = 64.dp, iconSize = 26.dp, shape = square)
            JIconButton(
                JIcons.ThemeToggle, "Alternar tema claro/escuro (segure para escolher o tema da Home)", p.onToggleTheme,
                size = 64.dp, iconSize = 24.dp, shape = square, onLongClick = p.onOpenThemes,
            )
            JIconButton(JIcons.Brightness, "Ajustar brilho", p.onToggleBrightness, size = 64.dp, iconSize = 24.dp, shape = square)
            JIconButton(JIcons.Power, "Apagar tela", p.onScreenOff, size = 64.dp, iconSize = 24.dp, shape = square)
        }
    }
}

// ---------- Peças dos temas C–K ----------

/** Bloco compacto (ícone + nome), altura escolhida pelo tema; [topLeft] = ícone em cima à esquerda, nome embaixo. */
@Composable
fun CompactTile(item: HomeItem, modifier: Modifier = Modifier, height: Dp = 110.dp, iconSize: Dp = 30.dp, topLeft: Boolean = false) {
    val c = JTheme.colors
    Pressable(onClick = item.onClick, shape = RoundedCornerShape(Dimens.RadiusTile), modifier = modifier.height(height), description = item.label) { pressed, focused ->
        val tint = if (pressed || focused) c.accent else c.icon
        if (topLeft) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                JIcon(item.icon, tint, iconSize)
                BasicText(item.label, style = JancarType.tileLabel.copy(color = c.fg, fontSize = 19.sp))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                JIcon(item.icon, tint, iconSize)
                BasicText(item.label, style = JancarType.tileLabel.copy(color = c.fg, fontSize = 18.sp))
            }
        }
    }
}

/** Botão redondo grande com o nome embaixo (tema E). */
@Composable
fun BigRound(item: HomeItem, size: Dp = 92.dp) {
    val c = JTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Pressable(onClick = item.onClick, shape = CircleShape, modifier = Modifier.size(size), description = item.label) { pressed, focused ->
            JIcon(item.icon, if (pressed || focused) c.accent else c.fg, 34.dp)
        }
        BasicText(item.label, style = JancarType.chip.copy(color = c.fg))
    }
}

/** Pílula comprida (tema K). */
@Composable
fun Pill(item: HomeItem, width: Dp = 300.dp, height: Dp = 76.dp) {
    val c = JTheme.colors
    Pressable(onClick = item.onClick, shape = CircleShape, modifier = Modifier.width(width).height(height), description = item.label) { pressed, focused ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            JIcon(item.icon, if (pressed || focused) c.accent else c.icon, 30.dp)
            BasicText(item.label, style = JancarType.tileLabel.copy(color = c.fg, fontSize = 21.sp))
        }
    }
}

/** Botão do trilho (tema H): ícone em cima, nome pequeno embaixo. */
@Composable
fun RailButton(item: HomeItem, selected: Boolean = false) {
    val c = JTheme.colors
    Pressable(onClick = item.onClick, shape = RoundedCornerShape(18.dp), modifier = Modifier.width(84.dp).height(72.dp), description = item.label) { pressed, focused ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            JIcon(item.icon, if (pressed || focused || selected) c.accent else c.fg, 26.dp)
            BasicText(item.label, style = JancarType.appTag.copy(color = c.fg))
        }
    }
}

/** Quadrado com ícone e nome (tema J). */
@Composable
fun SquareButton(item: HomeItem, size: Dp = 104.dp) {
    val c = JTheme.colors
    Pressable(onClick = item.onClick, shape = RoundedCornerShape(22.dp), modifier = Modifier.size(size), description = item.label) { pressed, focused ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JIcon(item.icon, if (pressed || focused) c.accent else c.icon, 28.dp)
            BasicText(item.label, style = JancarType.appTag.copy(color = c.fg, fontSize = 14.sp))
        }
    }
}

/** Faixa escura lateral (temas com coluna/grade/pílulas): escurece só o lado onde ficam os botões. */
@Composable
private fun BoxScope.SideScrim(left: Boolean, from: Float, to: Float) {
    val c = JTheme.colors
    val stops = if (left) arrayOf(0f to c.bg.copy(alpha = .90f), from to c.bg.copy(alpha = .84f), to to c.bg.copy(alpha = 0f))
    else arrayOf((1f - to) to c.bg.copy(alpha = 0f), (1f - from) to c.bg.copy(alpha = .84f), 1f to c.bg.copy(alpha = .90f))
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.horizontalGradient(*stops)))
}

/** Cartão de música quando nada toca (temas D e I): convida a abrir a Música, em vez de deixar o espaço vazio. */
@Composable
private fun HomeParts.MusicOrIdle(modifier: Modifier = Modifier) {
    val c = JTheme.colors
    val playing = nowPlaying.title != null || nowPlaying.artist != null
    if (playing) {
        NowPlaying(modifier)
    } else {
        val music = apps[1]
        Pressable(onClick = music.onClick, shape = RoundedCornerShape(Dimens.RadiusTile), modifier = modifier.height(96.dp), description = "Abrir a Música") { _, _ ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(64.dp).background(c.surfaceHover, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    JIcon(JIcons.Music, c.muted, 28.dp)
                }
                Column {
                    BasicText("Nada tocando", style = JancarType.tileLabel.copy(color = c.fg, fontSize = 19.sp))
                    BasicText("Toque para abrir a Música", style = JancarType.appTag.copy(color = c.muted))
                }
            }
        }
    }
}

/**
 * Resumo do carro (temas F e I): combustível, temperatura externa, km, portas e média da viagem, dos mesmos dados do
 * Painel (CAN contado + OBD). Toque abre o Painel. Sem dado = "—".
 */
@Composable
private fun CarSummaryCard(onOpen: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val c = JTheme.colors
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.rocha.jancarlauncher.JancarApp
    val can = app.canBus
    androidx.compose.runtime.DisposableEffect(can) {
        can.connect()
        onDispose { can.disconnect() }
    }
    val car by app.carService.state.collectAsStateValue()
    val dash by can.dashboard.collectAsStateValue()
    val cabin by can.cabin.collectAsStateValue()
    val lights by can.lights.collectAsStateValue()
    val trip by app.obd.trip.collectAsStateValue()
    val r = com.rocha.jancarlauncher.ui.dash.DashModel.reading(car, dash, cabin, lights)
    val doors = when {
        r.doors.anyOpen -> "Aberto: ${com.rocha.jancarlauncher.ui.dash.DashModel.openDoorsText(r.doors)}"
        r.doors.known -> "Portas fechadas"
        else -> null
    }
    val avg = com.rocha.jancarlauncher.ui.dash.ConsumptionText.trip(trip)
    Pressable(onClick = onOpen, shape = RoundedCornerShape(20.dp), modifier = modifier, description = "Abrir o Painel") { _, _ ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText("FOX", style = JancarType.caption.copy(color = c.faint), modifier = Modifier.weight(1f))
                BasicText("Painel ›", style = JancarType.appTag.copy(color = c.accent))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 28.dp), verticalAlignment = Alignment.Bottom) {
                Stat(com.rocha.jancarlauncher.ui.dash.DashModel.valueText(r.fuelLiters), "L")
                if (!compact) Stat(com.rocha.jancarlauncher.ui.dash.DashModel.valueText(r.outsideTempC), "°C")
                Stat(com.rocha.jancarlauncher.ui.dash.DashModel.odoText(r.odoKm), "km")
            }
            val line = listOfNotNull(doors, if (avg != com.rocha.jancarlauncher.ui.dash.DashModel.DASH) "viagem $avg km/L" else null).joinToString(" · ")
            if (line.isNotEmpty()) BasicText(line, style = JancarType.appTag.copy(color = c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Stat(value: String, unit: String) {
    val c = JTheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        BasicText(value, style = JancarType.tileLabel.copy(color = if (value == com.rocha.jancarlauncher.ui.dash.DashModel.DASH) c.faint else c.fg, fontSize = 26.sp))
        Spacer(Modifier.width(4.dp))
        BasicText(unit, style = JancarType.appTag.copy(color = c.muted), modifier = Modifier.padding(bottom = 3.dp))
    }
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateValue() = collectAsState()

// ---------- Temas C–K ----------

/** C · coluna à esquerda: relógio, apps 2×2 e atalhos numa coluna escura; o carro fica à direita. */
@Composable
private fun BoxScope.LayoutColuna(p: HomeParts) {
    SideScrim(left = true, from = .36f, to = .56f)
    Column(Modifier.align(Alignment.TopStart).width(368.dp).padding(start = 40.dp, top = 30.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        p.Clock(timeSize = 80)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            p.apps.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { CompactTile(it, Modifier.weight(1f), height = 118.dp, topLeft = true) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            p.shortcuts.forEach { RoundIcon(it) }
        }
    }
    Column(Modifier.align(Alignment.TopEnd).padding(end = 32.dp, top = 30.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        p.TopButtons(size = 56.dp, iconSize = 22.dp)
        p.StatusLine(showLabel = false)
    }
}

/** D · "Tocando agora" em destaque no meio, atalhos ao lado e os 4 apps mais baixos embaixo. */
@Composable
private fun BoxScope.LayoutTocando(p: HomeParts) {
    Row(Modifier.fillMaxWidth().align(Alignment.TopStart).padding(start = 48.dp, end = 48.dp, top = 30.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.weight(1f)) { p.Clock(timeSize = 72) }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            p.StatusLine(showLabel = false)
            p.TopButtons(size = 56.dp, iconSize = 22.dp)
        }
    }
    Row(Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        p.MusicOrIdle(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            p.shortcuts.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { RoundIcon(it) } } }
        }
    }
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 48.dp, end = 48.dp, bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        p.apps.forEach { CompactTile(it, Modifier.weight(1f), height = 108.dp) }
    }
}

/** E · relógio grande no centro, botões pequenos nos cantos e os apps em botões redondos embaixo. */
@Composable
private fun BoxScope.LayoutCentral(p: HomeParts) {
    Box(Modifier.align(Alignment.TopCenter).padding(top = 26.dp)) {
        p.Clock(timeSize = 96, horizontal = Alignment.CenterHorizontally)
    }
    Column(Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JIconButton(JIcons.ThemeToggle, "Alternar tema claro/escuro (segure para escolher o tema da Home)", p.onToggleTheme, size = 52.dp, iconSize = 20.dp, onLongClick = p.onOpenThemes, shape = CircleShape)
        JIconButton(JIcons.Brightness, "Ajustar brilho", p.onToggleBrightness, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
        JIconButton(JIcons.Power, "Apagar tela", p.onScreenOff, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
    }
    Column(Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 24.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JIconButton(JIcons.Grid, "Ver todos os apps", p.onOpenDrawer, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
        JIconButton(JIcons.Gauge, "Painel de velocidade", p.onOpenDash, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
        JIconButton(JIcons.Settings, "Ajustes", p.shortcuts[3].onClick, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
    }
    Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        p.apps.forEach { BigRound(it) }
        BigRound(p.shortcuts[0].copy(label = "Wi-Fi"))
        BigRound(p.shortcuts[1].copy(label = "Bluetooth"))
    }
}

/** F · o arranjo de sempre com o resumo do carro no lugar do "Tocando agora". */
@Composable
private fun LayoutCarro(p: HomeParts) {
    Column(Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = 30.dp, bottom = 30.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                p.Clock(timeSize = 80)
                p.NowPlaying(Modifier.widthIn(max = 460.dp))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                p.StatusLine(showLabel = false)
                p.TopButtons(size = 56.dp, iconSize = 22.dp)
                CarSummaryCard(p.onOpenDash, Modifier.width(380.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.padding(bottom = 18.dp)) { p.Chips() }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            p.apps.forEach { CompactTile(it, Modifier.weight(1f), height = 124.dp, iconSize = 32.dp) }
        }
    }
}

/** G · os 4 apps em quadrado à direita, relógio grande embaixo à esquerda: o carro fica livre à esquerda. */
@Composable
private fun BoxScope.LayoutGrade(p: HomeParts) {
    SideScrim(left = false, from = .38f, to = .55f)
    Row(Modifier.align(Alignment.TopStart).padding(start = 32.dp, top = 28.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        p.shortcuts.forEach { RoundIcon(it, size = 52.dp, iconSize = 20.dp) }
    }
    Box(Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 34.dp)) {
        p.Clock(timeSize = 96, dateExtra = p.wifi.takeIf { it.connected }?.ssid)
    }
    Column(Modifier.align(Alignment.TopEnd).width(380.dp).padding(end = 32.dp, top = 28.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        p.TopButtons(size = 52.dp, iconSize = 20.dp)
        p.apps.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { row.forEach { CompactTile(it, Modifier.weight(1f), height = 150.dp, iconSize = 34.dp, topLeft = true) } }
        }
    }
}

/** H · trilho de ícones à esquerda (como o do Android Auto); relógio à direita e atalhos embaixo. */
@Composable
private fun BoxScope.LayoutTrilho(p: HomeParts) {
    val c = JTheme.colors
    Column(
        Modifier.align(Alignment.CenterStart).fillMaxHeight().width(108.dp).background(c.bg.copy(alpha = 0.90f)).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        p.apps.forEach { RailButton(it) }
        Divider(vertical = false, length = 52.dp)
        RailButton(HomeItem("dash", JIcons.Gauge, "Painel", p.onOpenDash))
        RailButton(HomeItem("drawer", JIcons.Grid, "Apps", p.onOpenDrawer))
    }
    Box(Modifier.align(Alignment.TopEnd).padding(end = 40.dp, top = 28.dp)) {
        p.Clock(timeSize = 84, horizontal = Alignment.End)
    }
    Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 140.dp, end = 40.dp, bottom = 26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        p.shortcuts.forEach { RoundIcon(it, size = 52.dp, iconSize = 20.dp) }
        Spacer(Modifier.width(6.dp))
        p.StatusLine(showLabel = false)
        Spacer(Modifier.weight(1f))
        JIconButton(JIcons.ThemeToggle, "Alternar tema claro/escuro (segure para escolher o tema da Home)", p.onToggleTheme, size = 52.dp, iconSize = 20.dp, onLongClick = p.onOpenThemes, shape = CircleShape)
        JIconButton(JIcons.Brightness, "Ajustar brilho", p.onToggleBrightness, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
        JIconButton(JIcons.Power, "Apagar tela", p.onScreenOff, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
    }
}

/** I · widgets em cima (relógio, música, carro) e os apps embaixo. */
@Composable
private fun BoxScope.LayoutWidgets(p: HomeParts) {
    val c = JTheme.colors
    Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        p.StatusLine(showLabel = false)
        Spacer(Modifier.weight(1f))
        p.shortcuts.forEach { RoundIcon(it, size = 48.dp, iconSize = 20.dp) }
        Divider(length = 28.dp)
        p.TopButtons(size = 48.dp, iconSize = 20.dp)
    }
    Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 80.dp).height(132.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Pressable(onClick = p.onToggleBrightness, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(0.9f).fillMaxHeight(), description = "Relógio") { _, _ ->
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                BasicText(p.clock.time, style = JancarType.clockLarge.copy(color = c.fg, fontSize = 58.sp, lineHeight = 58.sp))
                BasicText(p.clock.date, style = JancarType.appTag.copy(color = c.muted, fontSize = 15.sp))
            }
        }
        Box(Modifier.weight(1.4f).fillMaxHeight(), contentAlignment = Alignment.Center) { p.MusicOrIdle(Modifier.fillMaxWidth()) }
        CarSummaryCard(p.onOpenDash, Modifier.weight(1f).fillMaxHeight(), compact = true)
    }
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        p.apps.forEach { CompactTile(it, Modifier.weight(1f), height = 112.dp) }
    }
}

/** J · apps e atalhos em oito quadrados numa fileira só embaixo. */
@Composable
private fun BoxScope.LayoutOito(p: HomeParts) {
    Row(Modifier.fillMaxWidth().align(Alignment.TopStart).padding(start = 56.dp, end = 56.dp, top = 32.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.weight(1f)) { p.Clock(timeSize = 80, dateExtra = p.wifi.takeIf { it.connected }?.ssid) }
        p.TopButtons(size = 52.dp, iconSize = 20.dp)
    }
    Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        (p.apps + p.shortcuts).forEach { SquareButton(it) }
    }
}

/** K · apps em pílulas empilhadas à direita; atalhos em pílulas pequenas embaixo à esquerda. */
@Composable
private fun BoxScope.LayoutPilulas(p: HomeParts) {
    SideScrim(left = false, from = .34f, to = .58f)
    Box(Modifier.align(Alignment.TopStart).padding(start = 44.dp, top = 30.dp)) { p.Clock(timeSize = 84) }
    Column(Modifier.align(Alignment.TopEnd).padding(end = 36.dp, top = 26.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JIconButton(JIcons.ThemeToggle, "Alternar tema claro/escuro (segure para escolher o tema da Home)", p.onToggleTheme, size = 52.dp, iconSize = 20.dp, onLongClick = p.onOpenThemes, shape = CircleShape)
            JIconButton(JIcons.Brightness, "Ajustar brilho", p.onToggleBrightness, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
            JIconButton(JIcons.Grid, "Ver todos os apps", p.onOpenDrawer, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
            JIconButton(JIcons.Gauge, "Painel de velocidade", p.onOpenDash, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
            JIconButton(JIcons.Power, "Apagar tela", p.onScreenOff, size = 52.dp, iconSize = 20.dp, shape = CircleShape)
        }
        p.apps.forEach { Pill(it) }
    }
    Row(Modifier.align(Alignment.BottomStart).padding(start = 44.dp, bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        p.shortcuts.forEach { item ->
            Pressable(onClick = item.onClick, shape = CircleShape, modifier = Modifier.height(52.dp), description = item.label) { _, _ ->
                Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JIcon(item.icon, JTheme.colors.icon, 18.dp)
                    BasicText(if (item.key == "wifi" && p.wifi.connected) p.wifi.ssid ?: item.label else item.label, style = JancarType.chip.copy(color = JTheme.colors.fg), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
                }
            }
        }
    }
}
