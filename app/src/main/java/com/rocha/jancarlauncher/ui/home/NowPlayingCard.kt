package com.rocha.jancarlauncher.ui.home

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.media.NowPlayingState
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.components.TransportButton
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/**
 * Card "Tocando agora": capa/título/artista + transporte. Some quando não há sessão de mídia ativa
 * ([NowPlayingState.EMPTY]) — não ocupa espaço nem pisca enquanto nada está tocando.
 */
@Composable
fun NowPlayingCard(
    nowPlaying: NowPlayingState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = nowPlaying.title != null || nowPlaying.artist != null
    AnimatedVisibility(visible = visible, modifier = modifier) {
        val c = JTheme.colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusTile))
                .background(c.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val artBitmap = remember(nowPlaying.art) {
                    nowPlaying.art?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                }
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(Dimens.RadiusButton)).background(c.surfaceHover),
                    contentAlignment = Alignment.Center,
                ) {
                    if (artBitmap != null) {
                        Image(artBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp))
                    } else {
                        JIcon(JIcons.NoteMusic, c.muted, 22.dp)
                    }
                }
                androidx.compose.foundation.layout.Column {
                    BasicText(
                        nowPlaying.title ?: "",
                        style = JancarType.chip.copy(color = c.fg),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        nowPlaying.artist ?: "",
                        style = JancarType.appTag.copy(color = c.muted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransportButton(JIcons.Shuffle, "Aleatório", {}, enabled = nowPlaying.supportsShuffle)
                TransportButton(JIcons.Previous, "Faixa anterior", onPrevious)
                TransportButton(
                    if (nowPlaying.isPlaying) JIcons.Pause else JIcons.Play,
                    if (nowPlaying.isPlaying) "Pausar" else "Tocar",
                    { if (nowPlaying.isPlaying) onPause() else onPlay() },
                    accent = true,
                )
                TransportButton(JIcons.Next, "Próxima faixa", onNext)
            }
        }
    }
}
