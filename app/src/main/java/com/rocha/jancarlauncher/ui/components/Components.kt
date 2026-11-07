package com.rocha.jancarlauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/** Ícone vetorial tingido; sem dependência de Material. */
@Composable
fun JIcon(icon: ImageVector, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/**
 * Superfície clicável do launcher: escala 0,98 ao pressionar (sem ripple, mais barato) e
 * borda de acento quando focada (D-pad/botões físicos da central).
 */
@Composable
private fun Pressable(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable (pressed: Boolean, focused: Boolean) -> Unit,
) {
    val colors = JTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed && enabled) 0.98f else 1f, tween(100), label = "press")
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(if (pressed || focused) colors.surfaceHover else colors.surface)
            .border(1.dp, if (focused) colors.accent else if (pressed) colors.surfaceHoverBorder else colors.surfaceBorder, shape)
            .onFocusChanged { focused = it.isFocused }
            .semantics { if (description != null) contentDescription = description }
            .combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { content(pressed, focused) }
}

/** Botão quadrado com ícone (padrão 48×48: voltar etc.; a fileira da Home usa [Dimens.TopButton]). */
@Composable
fun JIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.MinTouch,
    iconSize: Dp = 18.dp,
) {
    val colors = JTheme.colors
    Pressable(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.RadiusButton),
        modifier = modifier.size(size),
        description = description,
    ) { pressed, _ ->
        JIcon(icon, if (pressed) colors.fg else colors.muted, iconSize)
    }
}

/** Chip em pílula (Wi-Fi, Bluetooth, GPS, Ajustes), com altura mínima [Dimens.ChipHeight]. */
@Composable
fun QuickChip(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = JTheme.colors
    Pressable(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier.heightIn(min = Dimens.ChipHeight),
        description = label,
    ) { _, _ ->
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JIcon(icon, colors.icon, 18.dp)
            BasicText(label, style = JancarType.chip.copy(color = colors.fg))
        }
    }
}

/** Bloco grande da Home (Auto, Música, Rádio, Vídeo). O ícone vira acento ao pressionar/focar. */
@Composable
fun MainTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = JTheme.colors
    Pressable(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.RadiusTile),
        modifier = modifier,
        description = label,
        onLongClick = onLongClick,
    ) { pressed, focused ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            JIcon(icon, if (pressed || focused) colors.accent else colors.icon, 40.dp)
            BasicText(label, style = JancarType.tileLabel.copy(color = colors.fg))
        }
    }
}

/** Botão circular pequeno do card "Tocando agora" (transporte); [accent]=true para o play/pause central. */
@Composable
fun TransportButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
) {
    val colors = JTheme.colors
    Pressable(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier.size(if (accent) Dimens.TransportAccent else Dimens.TransportButton).alpha(if (enabled) 1f else 0.35f),
        enabled = enabled,
        description = description,
    ) { pressed, _ ->
        if (accent) {
            Box(Modifier.size(Dimens.TransportAccent).clip(CircleShape).background(colors.accent), contentAlignment = Alignment.Center) {
                JIcon(icon, Color.White, Dimens.TransportAccentIcon)
            }
        } else {
            JIcon(icon, if (pressed) colors.fg else colors.icon, Dimens.TransportIcon)
        }
    }
}

/** Tile da gaveta de apps; [enabled]=false quando o app não está instalado/habilitado. */
@Composable
fun AppTile(
    icon: ImageVector,
    name: String,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JTheme.colors
    Pressable(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.RadiusAppTile),
        modifier = modifier.alpha(if (enabled) 1f else 0.4f),
        description = name,
    ) { _, _ ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JIcon(icon, colors.icon, 30.dp)
            BasicText(
                name,
                style = JancarType.appName.copy(color = colors.fg, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(tag.uppercase(), style = JancarType.appTag.copy(color = colors.faint))
        }
    }
}
