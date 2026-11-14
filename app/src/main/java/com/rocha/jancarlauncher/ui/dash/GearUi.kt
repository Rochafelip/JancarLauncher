package com.rocha.jancarlauncher.ui.dash

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import kotlinx.coroutines.delay

/** Marcha estável para a tela: reavalia quando a leitura muda e de novo depois de [GearEstimator.STABLE_MS]. */
@Composable
fun rememberGear(rpm: Float?, speed: Float?): Int? {
    val estimator = remember { GearEstimator() }
    var gear by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(rpm, speed) {
        gear = estimator.update(rpm, speed, SystemClock.uptimeMillis())
        delay(GearEstimator.STABLE_MS + 50)
        gear = estimator.update(rpm, speed, SystemClock.uptimeMillis())
    }
    return gear
}

/** Cor da seta: subir segue o aviso de troca (âmbar perto, vermelho no ponto); descer, o azul do acento. */
@Composable
fun hintColor(s: GearModel.Suggestion, stage: DashModel.ShiftStage): Color = when {
    s.hint == GearModel.Hint.Down -> dash.normal
    stage == DashModel.ShiftStage.Shift -> dash.red
    else -> dash.warn
}

/** Painel: marcha num quadradinho + a seta da sugestão. Sem marcha (embreagem, ponto morto, parado) = some. */
@Composable
fun GearBadge(gear: Int?, suggestion: GearModel.Suggestion, stage: DashModel.ShiftStage) {
    if (gear == null) return
    val c = JTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                .background(c.surface, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                gear.toString(),
                style = JancarType.tileLabel.copy(color = c.fg, fontSize = 28.sp, fontWeight = FontWeight.W600),
            )
        }
        GearModel.hintText(suggestion)?.let {
            Spacer(Modifier.width(8.dp))
            BasicText(
                it,
                style = JancarType.tileLabel.copy(color = hintColor(suggestion, stage), fontSize = 26.sp, fontWeight = FontWeight.W600),
            )
        }
    }
}
