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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.R
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/**
 * Galeria "Escolher tema" (desenho do Claude Design): abre segurando o botão de tema da Home ou pelo painel de
 * brilho. Miniatura de cada tema = a foto do carro com o esquema do arranjo por cima; toque aplica e fecha.
 * Temas ainda sem arranjo no app aparecem apagados com "em breve".
 */
@Composable
fun ThemePicker(current: HomeLayout, ready: Set<HomeLayout>, onPick: (HomeLayout) -> Unit, onClose: () -> Unit) {
    val c = JTheme.colors
    val photo = painterResource(R.drawable.home_wallpaper)
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            // Engole os toques: nada da Home por baixo reage com a galeria aberta.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.88f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 26.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JIconButton(JIcons.Close, "Fechar", onClose, size = 52.dp, iconSize = 22.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText("Escolher tema", style = JancarType.panelTitle.copy(color = c.fg, fontSize = 24.sp))
                    BasicText(
                        "Toque para aplicar. O toque simples no botão continua trocando claro / escuro.",
                        style = JancarType.appTag.copy(color = c.muted, fontSize = 14.sp),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            HomeLayout.entries.chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { layout ->
                        ThemeCard(layout, layout == current, layout in ready, photo, Modifier.weight(1f)) {
                            onPick(layout)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth()) {
                BasicText("Tema atual: ", style = JancarType.appTag.copy(color = c.muted, fontSize = 14.sp))
                BasicText("${current.letter} · ${current.label}", style = JancarType.appTag.copy(color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.W600))
                Spacer(Modifier.weight(1f))
                BasicText("A tela do Fox fica fiel ao carro em qualquer tema.", style = JancarType.appTag.copy(color = c.faint, fontSize = 14.sp))
            }
        }
    }
}

@Composable
private fun ThemeCard(
    layout: HomeLayout,
    selected: Boolean,
    ready: Boolean,
    photo: androidx.compose.ui.graphics.painter.Painter,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = JTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(c.surface)
            .border(2.dp, if (selected) c.accent else c.fg.copy(alpha = 0.08f), shape)
            .clickable(enabled = ready, role = Role.Button, onClickLabel = "Usar o tema ${layout.label}", onClick = onClick)
            .alpha(if (ready) 1f else 0.45f),
    ) {
        Box(Modifier.fillMaxWidth().height(86.dp)) {
            Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x730A0B0F)))
            Canvas(Modifier.fillMaxSize()) {
                for (b in layout.preview) {
                    drawRoundRect(
                        color = if (b.dark) Color(0xD90A0B0F) else Color(0xD9EEF0F2),
                        topLeft = Offset(b.x * size.width, b.y * size.height),
                        size = Size(b.w * size.width, b.h * size.height),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
            }
        }
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(layout.letter, style = JancarType.body.copy(color = c.fg, fontWeight = FontWeight.W600))
            Spacer(Modifier.width(6.dp))
            BasicText(if (ready) layout.label else "em breve", style = JancarType.appTag.copy(color = c.muted))
        }
    }
}
