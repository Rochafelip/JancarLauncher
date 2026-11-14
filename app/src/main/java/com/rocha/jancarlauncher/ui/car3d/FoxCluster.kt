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
import androidx.compose.runtime.State
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
import com.rocha.jancarlauncher.ui.dash.DashModel
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
 * Fiel ao painel: conta-giros 0–8 (×1000) com faixa vermelha a partir de 6,5 e o relógio da **temperatura da água**
 * 50–90–130 °C embutido, pelo OBD (PID 05; o CAN/a central não têm; pedido do dono, 2026-11-14 — antes era km/L);
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

    /**
     * Shift light no aro do conta-giros (pedido do dono, 2026-11-14: "shift light somente no RPM, dependendo do modo"):
     * [SHIFT_LEDS] LEDs acendem um a um nos [SHIFT_RANGE_RPM] antes do ponto de troca do modo (DIA A DIA 1500→3000,
     * BRAVO 2350→3850, o torque máximo); no ponto, todos vermelhos piscando. Fora da faixa o aro fica cromado, como o original.
     */
    const val SHIFT_LEDS = 8
    const val SHIFT_RANGE_RPM = 1500f

    fun shiftLeds(rpm: Float?, mode: DashModel.DriveMode): Int {
        if (rpm == null) return 0
        val start = mode.shiftRpm - SHIFT_RANGE_RPM
        return ((rpm - start) / (SHIFT_RANGE_RPM / SHIFT_LEDS)).toInt().coerceIn(0, SHIFT_LEDS)
    }

    /** Cor de cada LED: 3 verdes, 3 âmbar, 2 vermelhos (como um shift light de corrida). */
    fun ledZone(index: Int): Int = when {
        index < 3 -> 0
        index < 6 -> 1
        else -> 2
    }

    /** Escala do relógio da água, como no painel: 50 a 130 °C (90 no topo). */
    const val COOLANT_MIN = 50f
    const val COOLANT_MAX = 130f

    /**
     * Ponteiro da água: fração 0..1 da escala (abaixo de 50 fica encostado no começo, como o de verdade com o motor
     * frio). Sem OBD ou o carro não informa = `null` (sem ponteiro).
     */
    fun coolantFraction(live: ObdClient.Live): Float? =
        if (live.status != ObdClient.Status.Connected) null
        else live.coolantC?.let { ((it - COOLANT_MIN) / (COOLANT_MAX - COOLANT_MIN)).coerceIn(0f, 1f) }

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
private val LedGreen = Color(0xFF2FD06B)
private val LedAmber = Color(0xFFF2A93B)
private val LedRed = Color(0xFFFF2B2B)
private val Chrome = Brush.verticalGradient(listOf(Color(0xFFE9ECEF), Color(0xFF7D838B), Color(0xFFD5D9DE)))

enum class GaugeKind { Tach, Speed }

/**
 * Um relógio do painel. [value] em rpm (conta-giros) ou km/h; `null` = sem ponteiro. [sub] = fração 0..1 do relógio
 * pequeno (água no conta-giros, combustível no velocímetro); `null` = sem ponteiro.
 * [lamp] acende a luz da lanterna no conta-giros (onde fica a do farol no painel original), `null` = não desenha.
 */
@Composable
fun AnalogGauge(
    kind: GaugeKind, value: Float?, sub: Float?, lamp: Boolean?, size: Dp, modifier: Modifier = Modifier,
    /** Só conta-giros: modo do shift light (`null` = sem shift light). Os LEDs seguem o ponteiro, não o giro cru. */
    shiftMode: DashModel.DriveMode? = null,
    /** No ponto de troca: pisca (lido só no desenho), todos os LEDs vermelhos. */
    shiftBlink: State<Float>? = null,
) {
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

        // Shift light: LEDs embutidos no topo do aro (só aparecem quando acendem).
        val shiftLeds = if (tach && shiftMode != null && shown != null) FoxCluster.shiftLeds(needle * 1000f, shiftMode) else 0
        if (shiftLeds > 0 || shiftBlink != null) {
            val full = shiftBlink != null
            val alpha = shiftBlink?.value ?: 1f
            for (i in 0 until FoxCluster.SHIFT_LEDS) {
                val at = p(160f, 160f, 152f, -42f + i * 12f)
                drawCircle(Color(0xFF050506), 8.5f * k, at)
                if (i >= shiftLeds && shiftBlink == null) continue
                val color = if (full) LedRed else when (FoxCluster.ledZone(i)) { 0 -> LedGreen; 1 -> LedAmber; else -> LedRed }
                drawCircle(color.copy(alpha = 0.35f * alpha), 13f * k, at)
                drawCircle(color.copy(alpha = alpha), 6f * k, at)
            }
        }

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

        // Relógio pequeno (água no conta-giros, combustível no velocímetro).
        val sc = Offset(160f * k, 254f * k)
        drawCircle(Color(0xFF08090A), 52f * k, sc)
        drawCircle(Color(0xFFB9BEC5), 52f * k, sc, style = Stroke(2.4f * k))
        // Faixa vermelha: água quente (120–130 °C) ou reserva do combustível.
        if (tach) arc(sc, 43f * k, 75f, 100f, ZoneRed, 6f * k) else arc(sc, 43f * k, -100f, -76f, ZoneRed, 6f * k)
        for (i in 0..8) {
            val a = -100f + i * 25f
            drawLine(TickWhite, p(160f, 254f, 46f, a), p(160f, 254f, if (i % 4 == 0) 36f else 41f, a), 2.2f * k)
        }
        val subLabels = if (tach) listOf("50" to -100f, "90" to 0f, "130" to 100f) else listOf("0" to -100f, "1/2" to 0f, "1" to 100f)
        for ((t, a) in subLabels) label(measurer, t, p(160f, 254f, 28f, a), 13f * k)
        if (tach) label(measurer, "°C", Offset(160f * k, 280f * k), 11f * k, FontWeight.W400)
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

/**
 * Visor âmbar do meio, como o do painel: marcha e sugestão de troca (como o indicador de marcha dos VW mais novos),
 * hora, consumo médio (OBD), temperatura externa, km e trip.
 */
@Composable
fun ClusterLcd(
    outsideTempC: Float?, odoKm: Int?, trip: Trip, modifier: Modifier = Modifier,
    gear: Int? = null, hint: String? = null,
) {
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText(gear?.toString() ?: "", style = st(24, align = TextAlign.Start), modifier = Modifier.weight(1f))
            BasicText(clock, style = st(24))
            BasicText(hint ?: "", style = st(20, align = TextAlign.End), modifier = Modifier.weight(1f))
        }
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

/** Seta do painel (dos lados do visor): verde acesa, quase invisível apagada. 48dp (pedido do dono: "aumentar as setas"). */
@Composable
fun ClusterArrow(left: Boolean, on: Boolean, modifier: Modifier = Modifier) {
    val painter = rememberVectorPainter(if (left) JIcons.TurnLeft else JIcons.TurnRight)
    Canvas(modifier.size(48.dp)) {
        with(painter) { draw(this@Canvas.size, colorFilter = ColorFilter.tint(if (on) LampGreen else LampOff)) }
    }
}
