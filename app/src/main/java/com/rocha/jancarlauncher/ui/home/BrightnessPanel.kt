package com.rocha.jancarlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import kotlin.math.roundToInt

/** Painel de brilho (260 dp). [onChange] dispara durante o arrasto; [onChangeFinished] ao soltar (persistir). */
@Composable
fun BrightnessPanel(
    percent: Int, onChange: (Int) -> Unit, onChangeFinished: () -> Unit, modifier: Modifier = Modifier,
    /** Atalho para a galeria de temas (segurar o botão de tema é difícil de descobrir sozinho). */
    onOpenThemes: (() -> Unit)? = null,
    themeLabel: String? = null,
) {
    val c = JTheme.colors
    val shape = RoundedCornerShape(Dimens.RadiusPanel)
    Column(
        modifier = modifier
            .width(260.dp)
            .shadow(16.dp, shape)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.surfaceBorder, shape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText("Brilho", style = JancarType.panelTitle.copy(color = c.fg))
            BasicText("$percent%", style = JancarType.body.copy(color = c.muted))
        }
        JSlider(percent, BrightnessMapper.MIN, BrightnessMapper.MAX, onChange, onChangeFinished)
        if (onOpenThemes != null) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.surfaceBorder))
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouch)
                    .clip(RoundedCornerShape(Dimens.RadiusButton))
                    .background(c.surfaceHover)
                    .clickable(onClickLabel = "Escolher tema da Home", onClick = onOpenThemes)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("Escolher tema", style = JancarType.body.copy(color = c.fg), modifier = Modifier.weight(1f))
                if (themeLabel != null) BasicText(themeLabel, style = JancarType.appTag.copy(color = c.muted))
            }
        }
    }
}

/** Slider simples (toque, arrasto e D-pad), sem Material. Altura de toque 48 dp. */
@Composable
fun JSlider(value: Int, min: Int, max: Int, onChange: (Int) -> Unit, onChangeFinished: () -> Unit, modifier: Modifier = Modifier) {
    val c = JTheme.colors
    var widthPx by remember { mutableIntStateOf(0) }
    var focused by remember { mutableIntStateOf(0) }
    val thumb = 20.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thumbPx = with(density) { thumb.toPx() }

    fun fromX(x: Float): Int {
        val usable = (widthPx - thumbPx).coerceAtLeast(1f)
        val fraction = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
        return min + (fraction * (max - min)).roundToInt()
    }

    val fraction = (value - min).toFloat() / (max - min)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.MinTouch)
            .onSizeChanged { widthPx = it.width }
            .semantics {
                contentDescription = "Brilho da tela"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), min.toFloat()..max.toFloat())
                setProgress { target -> onChange(target.roundToInt().coerceIn(min, max)); onChangeFinished(); true }
            }
            .pointerInput(min, max, widthPx) {
                detectTapGestures { offset -> onChange(fromX(offset.x)); onChangeFinished() }
            }
            .pointerInput(min, max, widthPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onChange(fromX(offset.x)) },
                    onDragEnd = onChangeFinished,
                    onDragCancel = onChangeFinished,
                    onHorizontalDrag = { change, _ -> change.consume(); onChange(fromX(change.position.x)) },
                )
            }
            .onFocusChanged { focused = if (it.isFocused) 1 else 0 }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { SliderKeys.step(value, -1, min, max, onChange, onChangeFinished); true }
                    Key.DirectionRight -> { SliderKeys.step(value, +1, min, max, onChange, onChangeFinished); true }
                    else -> false
                }
            }
            .focusable(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = thumb / 2).height(4.dp).clip(CircleShape).background(c.surfaceHover))
        Box(
            Modifier
                .padding(start = thumb / 2)
                .width(with(density) { ((widthPx - thumbPx).coerceAtLeast(0f) * fraction).toDp() })
                .height(4.dp)
                .clip(CircleShape)
                .background(c.accent),
        )
        Box(
            Modifier
                .offset { IntOffset(((widthPx - thumbPx).coerceAtLeast(0f) * fraction).roundToInt(), 0) }
                .size(thumb)
                .clip(CircleShape)
                .background(c.accent)
                .then(if (focused == 1) Modifier.border(2.dp, c.fg, CircleShape) else Modifier),
        )
    }
}
