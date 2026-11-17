package com.rocha.jancarlauncher.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.media.NowPlayingState
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.components.Pressable
import com.rocha.jancarlauncher.ui.components.TransportButton
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/** Ações do player (as mesmas do card antigo, mais os atalhos do "Nada tocando"). */
class PlayerActions(
    val onPlay: () -> Unit,
    val onPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onOpenMusic: () -> Unit,
    val onOpenRadio: () -> Unit,
)

/**
 * Player em **Cartão** (temas com espaço): capa/ícone 72dp, título e subtítulo, ⏮ ⏯ ⏭ grandes. Sempre visível:
 * sem nada tocando mostra "Nada tocando" e os atalhos Música · Rádio.
 */
@Composable
fun PlayerCard(np: NowPlayingState, a: PlayerActions, modifier: Modifier = Modifier) {
    val c = JTheme.colors
    val v = PlayerModel.of(np)
    Row(
        modifier
            .heightIn(min = 112.dp)
            .clip(RoundedCornerShape(Dimens.RadiusTile))
            .background(c.surface)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SourceArea(v, a, Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Cover(np, v, 72.dp, 32.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(v.title, style = JancarType.nowPlayingTitle.copy(color = c.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    v.subtitle?.let {
                        BasicText(it, style = JancarType.nowPlayingArtist.copy(color = c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (v.mode == PlayerMode.Idle) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SourceButton(JIcons.Music, "Música", a.onOpenMusic, 56.dp)
                SourceButton(JIcons.Radio, "Rádio", a.onOpenRadio, 56.dp)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TransportButton(JIcons.Previous, prevLabel(v), a.onPrevious)
                TransportButton(
                    if (v.playing) JIcons.Pause else JIcons.Play,
                    if (v.playing) "Pausar" else "Tocar",
                    { if (v.playing) a.onPause() else a.onPlay() },
                    accent = true,
                )
                TransportButton(JIcons.Next, nextLabel(v), a.onNext)
            }
        }
    }
}

/**
 * Player em **Barra** (temas apertados): 64dp de altura, "título · subtítulo" numa linha e ⏮ ⏯ ⏭ de 48dp. Sem nada
 * tocando: "Nada tocando" + Música · Rádio.
 */
@Composable
fun PlayerBar(np: NowPlayingState, a: PlayerActions, modifier: Modifier = Modifier) {
    val c = JTheme.colors
    val v = PlayerModel.of(np)
    Row(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(Dimens.RadiusButton + 4.dp))
            .background(c.surface)
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SourceArea(v, a, Modifier.weight(1f).height(52.dp)) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cover(np, v, 44.dp, 22.dp)
                BasicText(
                    listOfNotNull(v.title, v.subtitle).joinToString(" · "),
                    style = JancarType.body.copy(color = c.fg, fontSize = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (v.mode == PlayerMode.Idle) {
            SourceButton(JIcons.Music, "Música", a.onOpenMusic, 48.dp)
            SourceButton(JIcons.Radio, "Rádio", a.onOpenRadio, 48.dp)
        } else {
            SmallTransport(JIcons.Previous, prevLabel(v), a.onPrevious)
            SmallTransport(if (v.playing) JIcons.Pause else JIcons.Play, if (v.playing) "Pausar" else "Tocar", { if (v.playing) a.onPause() else a.onPlay() }, accent = true)
            SmallTransport(JIcons.Next, nextLabel(v), a.onNext)
        }
    }
}

@Composable
private fun Cover(np: NowPlayingState, v: PlayerView, size: Dp, iconSize: Dp) {
    val c = JTheme.colors
    val bmp = remember(np.art) { np.art?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
    Box(Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(c.surfaceHover), contentAlignment = Alignment.Center) {
        if (bmp != null && v.mode == PlayerMode.Track) {
            Image(bmp.asImageBitmap(), null, Modifier.size(size), contentScale = ContentScale.Crop)
        } else {
            JIcon(if (v.mode == PlayerMode.Radio) JIcons.Radio else JIcons.NoteMusic, c.muted, iconSize)
        }
    }
}

@Composable
private fun SourceButton(icon: ImageVector, label: String, onClick: () -> Unit, height: Dp) {
    val c = JTheme.colors
    Pressable(onClick = onClick, shape = RoundedCornerShape(Dimens.RadiusButton), modifier = Modifier.height(height), description = "Abrir $label") { _, _ ->
        Row(
            Modifier.background(c.surfaceHover, RoundedCornerShape(Dimens.RadiusButton)).height(height).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JIcon(icon, c.fg, 20.dp)
            BasicText(label, style = JancarType.chip.copy(color = c.fg))
        }
    }
}

/** Botão de transporte da Barra: 48dp (o do Cartão é o [TransportButton], 60/64dp). */
@Composable
private fun SmallTransport(icon: ImageVector, description: String, onClick: () -> Unit, accent: Boolean = false) {
    val c = JTheme.colors
    Pressable(onClick = onClick, shape = CircleShape, modifier = Modifier.size(Dimens.MinTouch), description = description) { pressed, _ ->
        if (accent) {
            Box(Modifier.size(Dimens.MinTouch).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                JIcon(icon, Color.White, 22.dp)
            }
        } else {
            JIcon(icon, if (pressed) c.fg else c.icon, 22.dp)
        }
    }
}

private fun openSource(v: PlayerView, a: PlayerActions): () -> Unit = if (v.mode == PlayerMode.Radio) a.onOpenRadio else a.onOpenMusic
private fun sourceLabel(v: PlayerView) = if (v.mode == PlayerMode.Radio) "Abrir o Rádio" else "Abrir a Música"
private fun prevLabel(v: PlayerView) = if (v.mode == PlayerMode.Radio) "Estação anterior" else "Faixa anterior"
private fun nextLabel(v: PlayerView) = if (v.mode == PlayerMode.Radio) "Próxima estação" else "Próxima faixa"


/** Área do texto (capa + título): toque abre o app da fonte. Sem fundo próprio (o cartão já tem). */
@Composable
private fun SourceArea(v: PlayerView, a: PlayerActions, modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .clickable(onClickLabel = sourceLabel(v), role = Role.Button, onClick = openSource(v, a)),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}
