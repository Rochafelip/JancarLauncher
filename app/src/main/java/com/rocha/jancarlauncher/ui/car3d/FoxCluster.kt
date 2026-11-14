package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.obd.Trip
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.Inter
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Painel de instrumentos original do Fox G2 redesenhado a partir da foto do dono (2026-11-14; arranjo "A" escolhido
 * no Claude Design: relógios dos lados, foto do carro e visor âmbar no meio). Regras puras, testáveis.
 *
 * Fiel ao painel: conta-giros 0–8 (×1000) com faixa vermelha a partir de 6,5 e, no lugar do relógio da água (sem
 * fonte nesta central), o de **consumo instantâneo 0–30 km/L** pelo OBD (pedido do dono, 2026-11-14);
 * velocímetro 0–200 com o do combustível embutido; ponteiros vermelhos; visor âmbar com hora, consumo, temperatura
 * externa, km e trip. Honesto: sem dado = sem ponteiro / "---" (como o próprio visor do carro mostra).
 */
object FoxCluster {
    /** Tanque do Fox (50 L, ficha técnica informada pelo dono). */
    const val FUEL_TANK_L = 50f

    /** Varredura dos relógios grandes, em graus a partir do meio-dia no sentido horário (simétrica, como no carro). */
    const val SWEEP_START = -125f
    const val SWEEP_END = 125f

    fun angle(value: Float, min: Float, max: Float): Float =
        SWEEP_START + ((value - min) / (max - min)).coerceIn(0f, 1f) * (SWEEP_END - SWEEP_START)

    fun fuelFraction(liters: Float?): Float? = liters?.let { (it / FUEL_TANK_L).coerceIn(0f, 1f) }

    /** Fundo de escala do relógio de consumo (no lugar do da água). */
    const val KMPL_MAX = 30f

    /**
     * Ponteiro do consumo: fração 0..1 de [KMPL_MAX]. Sem OBD = `null` (sem ponteiro). Conectado e parado = 0 (parado
     * não anda: 0 km/L é o valor real, o gasto em L/h fica no Painel).
     */
    fun kmplFraction(live: ObdClient.Live): Float? = when {
        live.status != ObdClient.Status.Connected || live.litersPerHour == null -> null
        else -> ((live.kmPerLiter ?: 0f) / KMPL_MAX).coerceIn(0f, 1f)
    }

    // Visor: o painel do carro usa ponto decimal e "---" sem dado; o visor daqui faz igual.
    fun lcdTemp(c: Float?): String = c?.let { String.format(Locale.US, "%.1f °C", it) } ?: "--- °C"
    fun lcdKm(odo: Int?): String = odo?.toString() ?: "-----"
    fun lcdTrip(t: Trip): String = String.format(Locale.US, "%.1f", t.km)
    fun lcdConsumption(t: Trip): String = t.kmPerLiter?.let { String.format(Locale.US, "%.1f", it.coerceAtMost(99.9)) } ?: "---"
}

private val Amber = Color(0xFFFFB43A)
private val NeedleRed = Color(0xFFFF2B2B)
private val ZoneRed = Color(0xFFC8141D)
private val TickWhite = Color(0xFFF2F3F4)
private val LampGreen = Color(0xFF2FD06B)
private val LampOff = Color(0xFF1D2A22)
private val Chrome = Brush.verticalGradient(listOf(Color(0xFFE9ECEF), Color(0xFF7D838B), Color(0xFFD5D9DE)))

enum class GaugeKind { Tach, Speed }

/**
 * Um relógio do painel. [value] em rpm (conta-giros) ou km/h; `null` = sem ponteiro. [sub] = fração 0..1 do relógio
 * pequeno (consumo no conta-giros, combustível no velocímetro); `null` = sem ponteiro.
 * [lamp] acende a luz da lanterna no conta-giros (onde fica a do farol no painel original), `null` = não desenha.
 */
@Composable
fun AnalogGauge(kind: GaugeKind, value: Float?, sub: Float?, lamp: Boolean?, size: Dp, modifier: Modifier = Modifier) {
    val tach = kind == GaugeKind.Tach
    val max = if (tach) 8f else 200f
    // Ponteiro suave (250 ms), como o de verdade; leve: só anima quando o valor muda.
    val shown = (if (tach) value?.div(1000f) else value)
    val needle by animateFloatAsState(shown ?: 0f, tween(250), label = "ponteiro")
    val subShown by animateFloatAsState(sub ?: 0f, tween(400), label = "ponteiro pequeno")
    val measurer = rememberTextMeasurer()
    val lampPainter = rememberVectorPainter(JIcons.Headlight)

    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 320f
        fun p(cx: Float, cy: Float, r: Float, deg: Float): Offset {
            val t = Math.toRadians(deg.toDouble())
            return Offset((cx + r * sin(t)).toFloat() * k, (cy - r * cos(t)).toFloat() * k)
        }
        val c = Offset(160f * k, 160f * k)

        // Caixa, fundo e aro cromado.
        drawCircle(Color(0xFF020203), 157f * k, c)
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFF17191D), Color(0xFF060708)), Offset(160f * k, 135f * k), 190f * k),
            152f * k, c,
        )
        drawCircle(Chrome, 152f * k, c, style = Stroke(5f * k))

        // Faixa vermelha do conta-giros (6,5 a 8).
        if (tach) arc(c, 141f * k, FoxCluster.angle(6.5f, 0f, max), FoxCluster.angle(8f, 0f, max), ZoneRed, 9f * k)

        // Marcas: conta-giros longa a cada 1, curta a cada 0,25; velocímetro longa a cada 10, curta a cada 5.
        val majorStep = if (tach) 1f else 10f
        val minorStep = if (tach) 0.25f else 5f
        val n = (max / minorStep).toInt()
        for (i in 0..n) {
            val v = i * minorStep
            val major = (v / majorStep).let { kotlin.math.abs(it - Math.round(it)) < 1e-4 }
            val a = FoxCluster.angle(v, 0f, max)
            drawLine(TickWhite, p(160f, 160f, 146f, a), p(160f, 160f, if (major) 126f else 136f, a),
                (if (major) 3.6f else 1.8f) * k)
        }
        // Números.
        val labelStep = if (tach) 1f else 20f
        var v = 0f
        while (v <= max + 0.01f) {
            label(measurer, Math.round(v).toString(), p(160f, 160f, 104f, FoxCluster.angle(v, 0f, max)),
                (if (tach) 34f else 27f) * k)
            v += labelStep
        }
        // Unidade.
        val unit = if (tach) "1/min\nX1000" else "km/h"
        label(measurer, unit, Offset(108f * k, (if (tach) 200f else 206f) * k), 12f * k, FontWeight.W400)

        // Relógio pequeno (consumo km/L no conta-giros, combustível no velocímetro).
        val sc = Offset(160f * k, 254f * k)
        drawCircle(Color(0xFF08090A), 52f * k, sc)
        drawCircle(Color(0xFFB9BEC5), 52f * k, sc, style = Stroke(2.4f * k))
        if (!tach) arc(sc, 43f * k, -100f, -76f, ZoneRed, 6f * k) // reserva do combustível
        for (i in 0..8) {
            val a = -100f + i * 25f
            drawLine(TickWhite, p(160f, 254f, 46f, a), p(160f, 254f, if (i % 4 == 0) 36f else 41f, a), 2.2f * k)
        }
        val subLabels = if (tach) listOf("0" to -100f, "15" to 0f, "30" to 100f) else listOf("0" to -100f, "1/2" to 0f, "1" to 100f)
        for ((t, a) in subLabels) label(measurer, t, p(160f, 254f, 28f, a), 13f * k)
        if (tach) label(measurer, "km/L", Offset(160f * k, 280f * k), 10f * k, FontWeight.W400)
        if (sub != null) {
            drawLine(NeedleRed, sc, p(160f, 254f, 40f, -100f + subShown.coerceIn(0f, 1f) * 200f), 4f * k, StrokeCap.Round)
        }
        drawCircle(Color(0xFF1A1C20), 9f * k, sc)
        drawCircle(Color(0xFF9AA0A7), 9f * k, sc, style = Stroke(2f * k))

        // Luz da lanterna (verde: o fio do farol não diz se é alto ou baixo, então não usa o azul do farol alto).
        if (tach && lamp != null) {
            val s = 26f * k
            translate(218f * k, 104f * k) {
                with(lampPainter) { draw(Size(s, s), colorFilter = ColorFilter.tint(if (lamp) LampGreen else LampOff)) }
            }
        }

        // Ponteiro (com um brilho leve) e a tampa cromada.
        if (shown != null) {
            val a = FoxCluster.angle(needle, 0f, max)
            val tip = p(160f, 160f, 134f, a)
            val tail = p(160f, 160f, -20f, a)
            drawLine(NeedleRed.copy(alpha = 0.28f), tail, tip, 12f * k, StrokeCap.Round)
            drawLine(NeedleRed, tail, tip, 6f * k, StrokeCap.Round)
        }
        drawCircle(Color(0xFF14161A), 22f * k, c)
        drawCircle(Chrome, 22f * k, c, style = Stroke(4f * k))
    }
}

private fun DrawScope.arc(center: Offset, r: Float, fromDeg: Float, toDeg: Float, color: Color, width: Float) {
    // Graus do relógio (0 = meio-dia) → graus do Compose (0 = 3 horas).
    drawArc(
        color, fromDeg - 90f, toDeg - fromDeg, useCenter = false,
        topLeft = Offset(center.x - r, center.y - r), size = Size(2 * r, 2 * r), style = Stroke(width),
    )
}

private fun DrawScope.label(m: TextMeasurer, text: String, at: Offset, sizePx: Float, weight: FontWeight = FontWeight.W500) {
    val style = TextStyle(
        fontFamily = Inter, fontWeight = weight, color = TickWhite, textAlign = TextAlign.Center,
        fontSize = (sizePx / density / fontScale).sp, lineHeight = (sizePx * 1.15f / density / fontScale).sp,
    )
    val layout = m.measure(text, style)
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

/** Visor âmbar do meio, como o do painel: hora, consumo médio (OBD), temperatura externa, km e trip. */
@Composable
fun ClusterLcd(outsideTempC: Float?, odoKm: Int?, trip: Trip, modifier: Modifier = Modifier) {
    var clock by remember { mutableStateOf(now()) }
    LaunchedEffect(Unit) {
        while (true) { clock = now(); delay(10_000) }
    }
    val glow = Shadow(Amber.copy(alpha = 0.45f), blurRadius = 6f)
    fun st(size: Int, w: FontWeight = FontWeight.W600, align: TextAlign = TextAlign.Center) =
        TextStyle(fontFamily = Inter, fontWeight = w, fontSize = size.sp, color = Amber, shadow = glow, textAlign = align)

    Column(
        modifier
            .width(200.dp)
            .background(Color.Black, RoundedCornerShape(10.dp))
            .border(3.dp, Color(0xFF1D1F23), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(clock, style = st(24))
        LcdRule()
        BasicText("Consumo médio", style = st(17, FontWeight.W500))
        BasicText("${FoxCluster.lcdConsumption(trip)} km/L", style = st(20))
        LcdRule()
        BasicText(FoxCluster.lcdTemp(outsideTempC), style = st(20))
        LcdRule()
        Row(Modifier.fillMaxWidth()) {
            BasicText("km", style = st(12, FontWeight.W500, TextAlign.Start), modifier = Modifier.weight(1f))
            BasicText("trip", style = st(12, FontWeight.W500, TextAlign.End), modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            BasicText(FoxCluster.lcdKm(odoKm), style = st(20, align = TextAlign.Start), modifier = Modifier.weight(1f))
            BasicText(FoxCluster.lcdTrip(trip), style = st(20, align = TextAlign.End), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LcdRule() {
    Box(Modifier.padding(vertical = 6.dp).fillMaxWidth().height(2.dp).background(Amber.copy(alpha = 0.85f)))
}

private fun now(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

/** Seta do painel (acima/ao lado do visor): verde acesa, quase invisível apagada. */
@Composable
fun ClusterArrow(left: Boolean, on: Boolean, modifier: Modifier = Modifier) {
    val painter = rememberVectorPainter(if (left) JIcons.TurnLeft else JIcons.TurnRight)
    Canvas(modifier.size(30.dp)) {
        with(painter) { draw(this@Canvas.size, colorFilter = ColorFilter.tint(if (on) LampGreen else LampOff)) }
    }
}
