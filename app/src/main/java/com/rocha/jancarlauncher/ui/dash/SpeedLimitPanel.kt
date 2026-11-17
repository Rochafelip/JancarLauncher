package com.rocha.jancarlauncher.ui.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.obd.stepLimit
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/** Chip do topo do Painel: "LIMITE 110" ou "SEM LIMITE"; toque abre o [SpeedLimitPanel]. */
@Composable
fun SpeedLimitChip(limit: Int?, onClick: () -> Unit) {
    val c = JTheme.colors
    Row(
        Modifier
            .height(Dimens.MinTouch)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(c.surface)
            .clickable(onClickLabel = "Limite de velocidade", role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            if (limit == null) "SEM LIMITE" else "LIMITE $limit",
            style = JancarType.chip.copy(color = if (limit == null) c.muted else c.fg, letterSpacing = 1.sp),
        )
    }
}

/** Limite do aviso sonoro: −/+ de 10 em 10 (40..150 km/h) e Desligar. Muda na hora; tocar fora fecha. */
@Composable
fun SpeedLimitPanel(limit: Int?, onChange: (Int?) -> Unit, onClose: () -> Unit) {
    val c = JTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(Dimens.RadiusPanel)
        Column(
            Modifier
                .width(440.dp)
                .clip(shape)
                .background(c.surface.copy(alpha = 1f))
                .border(1.dp, c.surfaceBorder, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText("AVISO ACIMA DE", style = JancarType.caption.copy(color = c.muted))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Step("−", "Diminuir limite") { onChange(stepLimit(limit, -10)) }
                BasicText(
                    limit?.let { "$it km/h" } ?: "desligado",
                    style = JancarType.panelTitle.copy(color = c.fg, fontWeight = FontWeight.W600, fontSize = 26.sp, textAlign = TextAlign.Center),
                    modifier = Modifier.width(190.dp),
                )
                Step("+", "Aumentar limite") { onChange(stepLimit(limit, +10)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Sempre os dois botões, no mesmo lugar (o OK não pode pular de posição quando o limite liga).
                PanelButton("Desligar", enabled = limit != null) { onChange(null) }
                PanelButton("OK", accent = true, onClick = onClose)
            }
        }
    }
}

@Composable
private fun Step(label: String, description: String, onClick: () -> Unit) {
    val c = JTheme.colors
    Box(
        Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(c.surfaceHover)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { BasicText(label, style = JancarType.panelTitle.copy(color = c.fg, fontSize = 26.sp)) }
}

@Composable
private fun PanelButton(label: String, accent: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val c = JTheme.colors
    Box(
        Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(if (accent) c.accent.copy(alpha = 0.28f) else c.surfaceHover)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) { BasicText(label, style = JancarType.chip.copy(color = if (enabled) c.fg else c.faint, letterSpacing = 1.sp)) }
}

