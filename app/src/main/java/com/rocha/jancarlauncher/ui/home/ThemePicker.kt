package com.rocha.jancarlauncher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.R
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import com.rocha.jancarlauncher.ui.theme.ScreenStyles

/** Abas da galeria. */
private enum class PickerTab(val label: String) { Home("Home"), Welcome("Boas-vindas"), Drawer("Gaveta"), Dash("Painel") }

/**
 * Galeria "Escolher tema" (desenho do Claude Design): abre segurando o botão de tema da Home ou pelo painel de
 * brilho. Abas: **Home** (os 12 arranjos), **Boas-vindas**, **Gaveta** e **Painel** (versões das outras telas, pedido
 * do dono: "o usuário clica para escolher e fica livre"). Miniatura = a foto do carro com o esquema por cima; toque
 * aplica na hora e fica salvo. Temas da Home ainda sem arranjo aparecem apagados com "em breve".
 */
@Composable
fun ThemePicker(
    current: HomeLayout,
    ready: Set<HomeLayout>,
    onPick: (HomeLayout) -> Unit,
    onClose: () -> Unit,
    styles: ScreenStyles.Set = ScreenStyles.Set(),
    onStyleChange: (key: String, value: String) -> Unit = { _, _ -> },
) {
    val c = JTheme.colors
    val photo = painterResource(R.drawable.home_wallpaper)
    var tab by remember { mutableStateOf(PickerTab.Home) }
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            // Engole os toques: nada da Home por baixo reage com a galeria aberta.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.88f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JIconButton(JIcons.Close, "Fechar", onClose, size = 52.dp, iconSize = 22.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    BasicText("Escolher tema", style = JancarType.panelTitle.copy(color = c.fg, fontSize = 24.sp))
                    BasicText(
                        "Toque para aplicar. O botão ao lado da paleta troca claro / escuro.",
                        style = JancarType.appTag.copy(color = c.muted, fontSize = 14.sp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickerTab.entries.forEach { t -> TabChip(t.label, t == tab) { tab = t } }
            }
            Spacer(Modifier.height(16.dp))
            when (tab) {
                PickerTab.Home -> HomeLayout.entries.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { layout ->
                            ChoiceCard(
                                layout.letter, if (layout in ready) layout.label else "em breve",
                                layout.preview.map { ScreenStyles.Block(it.x, it.y, it.w, it.h, it.dark) },
                                layout == current, layout in ready, photo, Modifier.weight(1f), thumb = 86.dp,
                            ) { onPick(layout) }
                        }
                    }
                }
                PickerTab.Welcome -> ChoiceRow(ScreenStyles.Welcome.entries, styles.welcome, photo, 130.dp) { onStyleChange("welcome", it.name) }
                PickerTab.Drawer -> ChoiceRow(ScreenStyles.Drawer.entries, styles.drawer, photo, 130.dp) { onStyleChange("drawer", it.name) }
                PickerTab.Dash -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashGroup("Avisos (porta aberta, reserva)", ScreenStyles.Alert.entries, styles.alert, photo) { onStyleChange("alert", it.name) }
                    DashGroup("Ponto de troca no BRAVO", ScreenStyles.Shift.entries, styles.shift, photo) { onStyleChange("shift", it.name) }
                    DashGroup("Sem dados do carro", ScreenStyles.Empty.entries, styles.empty, photo) { onStyleChange("empty", it.name) }
                    DashGroup("Painel no tema claro", ScreenStyles.LightDash.entries, styles.lightDash, photo) { onStyleChange("light_dash", it.name) }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth()) {
                BasicText("Tema da Home: ", style = JancarType.appTag.copy(color = c.muted, fontSize = 14.sp))
                BasicText("${current.letter} · ${current.label}", style = JancarType.appTag.copy(color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.W600))
                Spacer(Modifier.weight(1f))
                BasicText("A tela do Fox fica fiel ao carro em qualquer tema.", style = JancarType.appTag.copy(color = c.faint, fontSize = 14.sp))
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = JTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) c.fg else c.surface)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = JancarType.chip.copy(color = if (selected) c.bg else c.fg))
    }
}

@Composable
private fun <T : ScreenStyles.Choice> ChoiceRow(options: List<T>, current: T, photo: Painter, thumb: Dp, onPick: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        options.forEach { o ->
            ChoiceCard(null, o.label, o.preview, o == current, true, photo, Modifier.weight(1f), thumb = thumb) { onPick(o) }
        }
        // Mantém os cartões do mesmo tamanho quando há poucas opções.
        repeat(4 - options.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun <T : ScreenStyles.Choice> DashGroup(title: String, options: List<T>, current: T, photo: Painter, onPick: (T) -> Unit) {
    val c = JTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(title, style = JancarType.body.copy(color = c.muted, fontSize = 15.sp), modifier = Modifier.width(220.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { o ->
                ChoiceCard(null, o.label, o.preview, o == current, true, photo, Modifier.weight(1f), thumb = 46.dp) { onPick(o) }
            }
            repeat(3 - options.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Cartão de uma opção: miniatura (foto + esquema) e o nome; borda de acento na escolhida. */
@Composable
private fun ChoiceCard(
    letter: String?,
    label: String,
    preview: List<ScreenStyles.Block>,
    selected: Boolean,
    ready: Boolean,
    photo: Painter,
    modifier: Modifier,
    thumb: Dp,
    onClick: () -> Unit,
) {
    val c = JTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(c.surface)
            .border(2.dp, if (selected) c.accent else c.fg.copy(alpha = 0.08f), shape)
            .clickable(enabled = ready, role = Role.Button, onClickLabel = "Usar: $label", onClick = onClick)
            .alpha(if (ready) 1f else 0.45f),
    ) {
        Box(Modifier.fillMaxWidth().height(thumb)) {
            Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x730A0B0F)))
            Canvas(Modifier.fillMaxSize()) {
                for (b in preview) {
                    drawRoundRect(
                        color = when {
                            b.accent -> Color(0xE6EF4444)
                            b.dark -> Color(0xD90A0B0F)
                            else -> Color(0xD9EEF0F2)
                        },
                        topLeft = Offset(b.x * size.width, b.y * size.height),
                        size = Size(b.w * size.width, b.h * size.height),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
            }
        }
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (letter != null) {
                BasicText(letter, style = JancarType.body.copy(color = c.fg, fontWeight = FontWeight.W600))
                Spacer(Modifier.width(6.dp))
            }
            BasicText(label, style = JancarType.appTag.copy(color = if (selected) c.fg else c.muted), maxLines = 1)
        }
    }
}
