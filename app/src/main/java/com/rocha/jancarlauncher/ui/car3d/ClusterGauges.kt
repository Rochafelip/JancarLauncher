package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.theme.Inter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Mostradores no estilo do painel original do Fox G2 (foto em `docs/Fox-photos/img.png`): conta-giros à
 * esquerda, velocímetro com o marcador de combustível à direita, aro cromado, números brancos e ponteiro
 * vermelho. O carro 3D fica no meio, no lugar do visor do painel original.
 *
 * Só mostra dado real: sem leitura, o ponteiro fica no zero, apagado, e o número vira "—" (sem "varredura" de
 * partida, que pareceria leitura). O mostrador de temperatura da água do painel original não aparece: o CAN do
 * Fox não manda esse dado.
 */
object Cluster {
    /** Arco dos mostradores grandes (graus do Compose: 0 = 3 h, sentido horário): zero às ~8 h, fim às ~4 h. */
    const val START_DEG = 140f
    const val SWEEP_DEG = 260f

    /** Conta-giros em ×1000 rpm, até 8 como no painel original; faixa vermelha a partir de 6,5 (desenho da foto). */
    const val RPM_SCALE_MAX = 8f
    const val RPM_RED_FROM = 6.5f

    const val SPEED_SCALE_MAX = 240f

    /** Tanque do Fox (ficha técnica: 50 L). Ajustar aqui se o marcador não bater com o tanque cheio. */
    const val FUEL_TANK_L = 50f

    /** Marcador pequeno de combustível: arco de 0 (esquerda) a cheio (direita), passando por cima. */
    const val SUB_START_DEG = 160f
    const val SUB_SWEEP_DEG = 220f

    /** Ângulo do ponteiro para [value] numa escala 0..[max]; fora da escala para no batente. */
    fun angle(value: Float, max: Float, start: Float = START_DEG, sweep: Float = SWEEP_DEG): Float =
        start + sweep * (value / max).coerceIn(0f, 1f)

    /** Fração do tanque (0..1) para o marcador; `null` = sem dado. */
    fun fuelFraction(liters: Float?): Float? = liters?.let { (it / FUEL_TANK_L).coerceIn(0f, 1f) }
}

// Cores do mostrador: fixas, como um painel físico (igual nos temas claro e escuro).
private val FaceCenter = Color(0xFF181B20)
private val FaceEdge = Color(0xFF07080A)
private val TickMajor = Color(0xFFEDEFF2)
private val TickMinor = Color(0xFF8A9097)
private val RedZone = Color(0xFFE5322D)
private val Needle = Color(0xFFFF4122)
private val LabelColor = Color(0xFFB5BAC0)
private val Chrome = listOf(
    Color(0xFF4A4F56), Color(0xFFDCE0E5), Color(0xFF3C4046), Color(0xFFB9BEC4),
    Color(0xFF55595F), Color(0xFFE6E9EC), Color(0xFF4A4F56),
)

/** Conta-giros (0–8 ×1000 rpm). */
@Composable
fun Tachometer(rpm: Float?, accent: Color, diameter: Dp, modifier: Modifier = Modifier) {
    DialGauge(
        value = rpm?.div(1000f),
        max = Cluster.RPM_SCALE_MAX,
        majorStep = 1f,
        minorStep = 0.5f,
        labelEvery = 1f,
        redFrom = Cluster.RPM_RED_FROM,
        unit = "1/min\n×1000",
        numeralScale = 0.15f,
        readoutX = 0f,
        readout = DashModel.rpmText(rpm),
        accent = accent,
        diameter = diameter,
        modifier = modifier,
    )
}

/** Velocímetro (0–240 km/h) com o marcador de combustível embaixo, à direita, como no painel original. */
@Composable
fun Speedometer(
    speed: Float?,
    fuelLiters: Float?,
    lowFuel: Boolean,
    accent: Color,
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    DialGauge(
        value = speed,
        max = Cluster.SPEED_SCALE_MAX,
        majorStep = 20f,
        minorStep = 10f,
        labelEvery = 20f,
        redFrom = null,
        unit = "km/h",
        numeralScale = 0.105f,
        readoutX = -0.22f,
        readout = DashModel.speedText(speed),
        accent = accent,
        diameter = diameter,
        modifier = modifier,
    ) {
        val r = diameter / 2
        FuelSubGauge(
            fraction = Cluster.fuelFraction(fuelLiters),
            low = lowFuel,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = r * 0.40f, y = r * 0.50f)
                .size(r * 0.50f),
        )
    }
}

@Composable
private fun DialGauge(
    value: Float?,
    max: Float,
    majorStep: Float,
    minorStep: Float,
    labelEvery: Float,
    redFrom: Float?,
    unit: String,
    numeralScale: Float,
    readoutX: Float,
    readout: String,
    accent: Color,
    diameter: Dp,
    modifier: Modifier = Modifier,
    extra: @Composable BoxScope.() -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    val needle = remember { Animatable(0f) }
    val target = value ?: 0f
    LaunchedEffect(target) { needle.animateTo(target, tween(280, easing = FastOutSlowInEasing)) }
    val live = value != null
    val r = diameter / 2

    Box(modifier.size(diameter)) {
        // Fundo, aro, marcas e números: desenhados uma vez (não dependem do valor).
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val rad = size.minDimension / 2
                val numeralStyle = TextStyle(
                    fontFamily = Inter, fontWeight = FontWeight.Medium,
                    fontSize = (rad * numeralScale).toSp(), color = TickMajor,
                )
                val unitStyle = TextStyle(
                    fontFamily = Inter, fontWeight = FontWeight.Normal,
                    fontSize = (rad * 0.075f).toSp(), lineHeight = (rad * 0.09f).toSp(), color = LabelColor,
                )
                val labels = generateSequence(0f) { it + labelEvery }.takeWhile { it <= max + 0.001f }
                    .map { v -> v to measurer.measure(formatLabel(v), numeralStyle) }.toList()
                val unitLayout = measurer.measure(unit, unitStyle)
                onDrawBehind {
                    drawFace(rad, accent)
                    if (redFrom != null) {
                        val a0 = Cluster.angle(redFrom, max)
                        val a1 = Cluster.angle(max, max)
                        val inset = rad * 0.86f
                        drawArc(
                            RedZone, a0, a1 - a0, useCenter = false,
                            topLeft = Offset(center.x - inset, center.y - inset),
                            size = Size(inset * 2, inset * 2),
                            style = Stroke(rad * 0.05f),
                        )
                    }
                    var v = 0f
                    while (v <= max + 0.001f) {
                        val major = isMultiple(v, majorStep)
                        val red = redFrom != null && v >= redFrom - 0.001f
                        val a = Cluster.angle(v, max)
                        val outer = rad * 0.89f
                        val inner = if (major) rad * 0.76f else rad * 0.82f
                        drawLine(
                            if (red) RedZone else if (major) TickMajor else TickMinor,
                            polar(a, inner), polar(a, outer),
                            strokeWidth = if (major) rad * 0.022f else rad * 0.012f,
                            cap = StrokeCap.Butt,
                        )
                        v += minorStep
                    }
                    for ((lv, layout) in labels) {
                        val p = polar(Cluster.angle(lv, max), rad * 0.63f)
                        drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
                    }
                    drawText(
                        unitLayout,
                        topLeft = Offset(center.x - rad * 0.32f - unitLayout.size.width / 2f, center.y + rad * 0.08f),
                    )
                }
            },
        )

        extra()

        // Leitura digital pequena (o número exato ajuda numa tela de 7").
        BasicText(
            readout,
            style = TextStyle(
                fontFamily = Inter, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum",
                fontSize = (diameter.value * 0.075f).sp, color = if (live) TickMajor else TickMinor,
            ),
            modifier = Modifier.align(Alignment.Center).offset(x = r * readoutX, y = r * 0.62f),
        )

        // Ponteiro: só esta camada gira quando o valor muda (o resto fica em cache).
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                rotationZ = Cluster.angle(needle.value, max)
                alpha = if (live) 1f else 0.35f
            },
        ) { drawNeedle(size.minDimension / 2, length = 0.86f) }

        // Capa do eixo (não gira).
        Canvas(Modifier.fillMaxSize()) { drawHub(size.minDimension / 2 * 0.11f) }
    }
}

@Composable
private fun FuelSubGauge(fraction: Float?, low: Boolean, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Box(
        modifier.drawWithCache {
            val rad = size.minDimension / 2
            val style = TextStyle(fontFamily = Inter, fontSize = (rad * 0.24f).toSp(), color = LabelColor)
            val marks = listOf(0f to "0", 0.5f to "1/2", 1f to "1").map { (f, s) -> f to measurer.measure(s, style) }
            onDrawBehind {
                drawCircle(FaceEdge, rad)
                drawCircle(Brush.linearGradient(Chrome), rad, style = Stroke(rad * 0.06f))
                val low0 = Cluster.SUB_START_DEG
                val lowSweep = Cluster.SUB_SWEEP_DEG * (DashModel.LOW_FUEL_LITERS / Cluster.FUEL_TANK_L)
                val inset = rad * 0.74f
                drawArc(
                    RedZone, low0, lowSweep, useCenter = false,
                    topLeft = Offset(center.x - inset, center.y - inset),
                    size = Size(inset * 2, inset * 2),
                    style = Stroke(rad * 0.09f),
                )
                for (i in 0..8) {
                    val a = Cluster.SUB_START_DEG + Cluster.SUB_SWEEP_DEG * i / 8f
                    val major = i % 4 == 0
                    drawLine(
                        if (major) TickMajor else TickMinor,
                        polar(a, rad * if (major) 0.62f else 0.70f), polar(a, rad * 0.84f),
                        strokeWidth = rad * if (major) 0.06f else 0.035f,
                    )
                }
                for ((f, layout) in marks) {
                    val p = polar(Cluster.SUB_START_DEG + Cluster.SUB_SWEEP_DEG * f, rad * 0.40f)
                    drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
                }
            }
        },
    ) {
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                rotationZ = Cluster.SUB_START_DEG + Cluster.SUB_SWEEP_DEG * (fraction ?: 0f)
                alpha = if (fraction != null) 1f else 0.35f
            },
        ) { drawNeedle(size.minDimension / 2, length = 0.80f, color = if (low) RedZone else Needle) }
        Canvas(Modifier.fillMaxSize()) { drawHub(size.minDimension / 2 * 0.16f) }
    }
}

/** Fundo escuro com leve brilho no meio, aro cromado e um fio azul discreto (a cor de destaque do app). */
private fun DrawScope.drawFace(rad: Float, accent: Color) {
    drawCircle(Brush.radialGradient(listOf(FaceCenter, FaceEdge), center, rad), rad)
    drawCircle(Brush.sweepGradient(Chrome, center), rad * 0.975f, style = Stroke(rad * 0.05f))
    drawCircle(accent.copy(alpha = 0.45f), rad * 0.935f, style = Stroke(rad * 0.008f))
}

/** Ponteiro apontando para a direita (0°); quem gira é a camada. */
private fun DrawScope.drawNeedle(rad: Float, length: Float, color: Color = Needle) {
    val c = center
    val tail = rad * 0.16f
    val base = rad * 0.034f
    val tip = rad * 0.008f
    val path = Path().apply {
        moveTo(c.x - tail, c.y - base)
        lineTo(c.x + rad * length, c.y - tip)
        lineTo(c.x + rad * length, c.y + tip)
        lineTo(c.x - tail, c.y + base)
        close()
    }
    // Brilho por baixo (ponteiro iluminado como no painel original).
    drawPath(path, color.copy(alpha = 0.28f), style = Stroke(rad * 0.04f))
    drawPath(path, color)
}

private fun DrawScope.drawHub(hubR: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFF2A2E34), Color(0xFF0C0D10)), center, hubR), hubR)
    drawCircle(Brush.sweepGradient(Chrome, center), hubR, style = Stroke(hubR * 0.22f))
}

private fun DrawScope.polar(deg: Float, radius: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    return Offset(center.x + radius * cos(a).toFloat(), center.y + radius * sin(a).toFloat())
}

private fun isMultiple(v: Float, step: Float): Boolean {
    val q = v / step
    return abs(q - q.roundToInt()) < 0.001f
}

private fun formatLabel(v: Float): String = v.roundToInt().toString()
