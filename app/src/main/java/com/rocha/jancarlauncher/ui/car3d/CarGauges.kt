package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.dash.blinking
import com.rocha.jancarlauncher.ui.dash.dash
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/**
 * Mostradores da tela do Carro no estilo painel de competição (proposta "C" escolhida pelo dono no Claude
 * Design, 2026-11-13): faixa de RPM atravessando o topo, segmentos que crescem em altura com o giro, e os
 * números grandes dos lados do carro. Cores do Painel: azul-aço normal, âmbar perto da troca, vermelho na
 * troca ([DashModel.DriveMode]). Inspiração de estilo apenas — sem marca de terceiros.
 */
object CarGauges {
    /** Escala da faixa: 0–8 mil rpm. */
    const val RPM_SCALE_MAX = 8000f
    const val STRIP_SEGMENTS = 48

    /** Tanque do Fox G2 (ficha técnica: 50 L). */
    const val FUEL_TANK_L = 50f
    const val FUEL_SEGMENTS = 10

    enum class Zone { Normal, Near, Shift }

    /** Segmento [i] da faixa: aceso ou não, e a faixa de cor pelo giro em que ele fica (ponto de troca do [mode]). */
    data class Segment(val lit: Boolean, val zone: Zone, val heightFraction: Float)

    fun stripSegments(rpm: Float?, mode: DashModel.DriveMode, n: Int = STRIP_SEGMENTS): List<Segment> =
        List(n) { i ->
            val v = RPM_SCALE_MAX * (i + 0.5f) / n
            val zone = when {
                v >= mode.shiftRpm -> Zone.Shift
                v >= mode.shiftRpm - DashModel.SHIFT_APPROACH_RPM -> Zone.Near
                else -> Zone.Normal
            }
            // Cresce da esquerda para a direita: a faixa "sobe" junto com o giro.
            Segment(lit = rpm != null && v <= rpm, zone = zone, heightFraction = 0.45f + 0.55f * i / (n - 1))
        }

    /** Quantos dos [FUEL_SEGMENTS] ficam acesos; `null` = sem dado. */
    fun fuelSegments(liters: Float?): Int? =
        liters?.let { Math.round((it / FUEL_TANK_L).coerceIn(0f, 1f) * FUEL_SEGMENTS) }
}

/** Faixa de RPM do topo: segmentos crescentes + escala 0–8 (a parte da troca em vermelho). */
@Composable
fun RpmStrip(rpm: Float?, mode: DashModel.DriveMode, blink: State<Float>?, modifier: Modifier = Modifier) {
    val c = JTheme.colors
    val p = dash
    val segs = CarGauges.stripSegments(rpm, mode)
    val stripHeight = 52.dp
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(stripHeight),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (s in segs) {
                val zoneColor = when (s.zone) {
                    CarGauges.Zone.Shift -> p.red
                    CarGauges.Zone.Near -> p.warn
                    CarGauges.Zone.Normal -> p.normal
                }
                Spacer(
                    Modifier
                        .weight(1f)
                        .height(stripHeight * s.heightFraction)
                        .then(if (s.lit) Modifier.blinking(if (s.zone == CarGauges.Zone.Shift) blink else null) else Modifier)
                        .then(if (s.lit) Modifier.shadow(4.dp, RoundedCornerShape(2.dp), ambientColor = zoneColor, spotColor = zoneColor) else Modifier)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (s.lit) zoneColor else p.off),
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(22.dp).offset(y = 4.dp)) {
            val steps = (CarGauges.RPM_SCALE_MAX / 1000).toInt()
            for (k in 0..steps) {
                val red = k * 1000 >= mode.shiftRpm
                BasicText(
                    "$k",
                    style = JancarType.appTag.copy(color = if (red) p.red else c.faint, fontFeatureSettings = "tnum"),
                    modifier = Modifier
                        .offset(x = (maxWidth * (k.toFloat() / steps) - 4.dp).coerceIn(0.dp, maxWidth - 10.dp))
                        .then(if (red) Modifier.blinking(blink) else Modifier),
                )
            }
        }
    }
}

/** Barrinha de combustível (10 segmentos), âmbar na reserva, com os litros ao lado. */
@Composable
fun FuelBar(liters: Float?, low: Boolean) {
    val c = JTheme.colors
    val p = dash
    val lit = CarGauges.fuelSegments(liters)
    Row(verticalAlignment = Alignment.CenterVertically) {
        JIcon(JIcons.Fuel, tint = if (low) p.warn else c.muted, size = 22.dp)
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 0 until CarGauges.FUEL_SEGMENTS) {
                val on = lit != null && i < lit
                Spacer(
                    Modifier.size(width = 7.dp, height = 13.dp).clip(RoundedCornerShape(1.dp))
                        .background(if (on) (if (low) p.warn else c.icon) else p.off),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        BasicText(
            "${DashModel.valueText(liters)} L",
            style = JancarType.body.copy(color = if (liters == null) c.faint else c.muted, fontFeatureSettings = "tnum"),
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Número grande com a unidade embaixo (velocidade à esquerda, giro à direita). */
@Composable
fun BigReadout(
    value: String,
    unit: String,
    sizeSp: Int,
    color: Color,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = JTheme.colors
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        BasicText(value, style = bigDigits(sizeSp).copy(color = color))
        BasicText(unit, style = JancarType.caption.copy(color = c.muted))
    }
}

private fun bigDigits(sizeSp: Int) = JancarType.clockLarge.copy(
    fontSize = androidx.compose.ui.unit.TextUnit(sizeSp.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp),
    lineHeight = androidx.compose.ui.unit.TextUnit(sizeSp.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp),
    fontWeight = androidx.compose.ui.text.font.FontWeight.W600,
    letterSpacing = androidx.compose.ui.unit.TextUnit(-1f, androidx.compose.ui.unit.TextUnitType.Sp),
    fontFeatureSettings = "tnum",
)
