package com.rocha.jancarlauncher.ui.car3d

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.ui.dash.DashModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * O carro na tela do Carro como FOTO real do Fox do dono (escolha dele, 2026-11-13: o 3D gerado por código não
 * ficava realista; a câmera já era fixa). A foto é recortada do fundo e tem a placa em branco; por cima, o app
 * desenha só o que vem dos sinais reais: brilho nos faróis (fio de iluminação), setas piscando (CAN) e destaque
 * âmbar nas portas abertas que aparecem nesse ângulo (as do motorista ficam no texto de baixo).
 *
 * Sem o arquivo [ASSET] a tela avisa que a foto não foi encontrada.
 */
object CarPhoto {
    const val ASSET = "car/photo.png"

    /** Posição de um brilho: centro (x, y) e raio, em fração da LARGURA/ALTURA da foto (raio: da largura). */
    data class Glow(val x: Float, val y: Float, val r: Float)

    enum class Door { FrontLeft, FrontRight, RearLeft, RearRight, Trunk, Hood }

    /** O que se desenha por cima da foto, em coordenadas da foto (0..1). */
    data class Spec(
        val head: List<Glow>,
        val turnLeft: List<Glow>,
        val turnRight: List<Glow>,
        /** Contorno de cada porta visível na foto; porta fora da lista = não aparece nesse ângulo. */
        val doors: Map<Door, List<Offset>>,
        /** Foto de teste (ex.: faróis já acesos nela): a tela avisa "FOTO PROVISÓRIA". */
        val provisional: Boolean,
    )

    // Foto definitiva: docs/Fox-photos/IMG_20261008_210115332_HDR_PCT.jpg.jpeg (3/4 pela frente do lado do
    // MOTORISTA, faróis apagados), recortada em 1302×697. As medidas abaixo estão no "quadro de trabalho" do
    // recorte (a foto reduzida pela metade; o recorte começa em (24, 43) dele) — as mesmas do contorno usado.
    private const val W = 1302f
    private const val H = 697f
    private const val OX = 24
    private const val OY = 43
    private fun p(x: Int, y: Int) = Offset((x - OX) / W, (y - OY) / H)
    private fun g(x: Int, y: Int, r: Int) = Glow((x - OX) / W, (y - OY) / H, r / W)

    val SPEC = Spec(
        // As duas lâmpadas do farol de perto (esquerdo) e a quina do farol de lá, ao lado da grade.
        head = listOf(g(380, 450, 30), g(460, 445, 34), g(48, 425, 18)),
        // Lado do motorista é o de perto: seta âmbar embaixo do farol e repetidor embaixo do retrovisor.
        turnLeft = listOf(g(350, 494, 20), g(862, 272, 26)),
        // Lado do passageiro: só a quina da frente aparece.
        turnRight = listOf(g(40, 446, 14)),
        doors = mapOf(
            Door.FrontLeft to listOf(
                p(802, 600), p(793, 330), p(742, 300), p(800, 212), p(876, 106), p(1075, 92), p(1080, 522),
            ),
            Door.RearLeft to listOf(
                p(1080, 92), p(1210, 95), p(1240, 150), p(1250, 240), p(1226, 330), p(1196, 420), p(1190, 505),
                p(1080, 535),
            ),
            Door.Hood to listOf(p(330, 243), p(690, 232), p(620, 300), p(470, 360), p(300, 420), p(90, 375)),
        ),
        provisional = false,
    )

    /** Portas abertas segundo o CAN ([DashModel.Doors]); `null` (sem dado) conta como fechada. */
    fun openDoors(d: DashModel.Doors): Set<Door> = buildSet {
        if (d.frontLeft == true) add(Door.FrontLeft)
        if (d.frontRight == true) add(Door.FrontRight)
        if (d.rearLeft == true) add(Door.RearLeft)
        if (d.rearRight == true) add(Door.RearRight)
        if (d.trunk == true) add(Door.Trunk)
        if (d.hood == true) add(Door.Hood)
    }

    /** Onde a foto cabe em [box] sem distorcer (centralizada). */
    fun fit(imageW: Int, imageH: Int, box: Size): Pair<Offset, Size> {
        val s = min(box.width / imageW, box.height / imageH)
        val size = Size(imageW * s, imageH * s)
        return Offset((box.width - size.width) / 2f, (box.height - size.height) / 2f) to size
    }
}

sealed interface PhotoLoad {
    data object Loading : PhotoLoad
    /** Não há foto no APK: a tela avisa. */
    data object Missing : PhotoLoad
    class Ready(val image: ImageBitmap) : PhotoLoad
}

/** Lê a foto fora da thread da tela. */
@Composable
fun rememberCarPhoto(): State<PhotoLoad> {
    val context = LocalContext.current.applicationContext
    return produceState<PhotoLoad>(PhotoLoad.Loading) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(CarPhoto.ASSET).use { BitmapFactory.decodeStream(it) } }.getOrNull()
                ?.let { PhotoLoad.Ready(it.asImageBitmap()) } ?: PhotoLoad.Missing
        }
    }
}

private val HeadGlow = Color(0xFFDCE9FF)
private val TurnGlow = Color(0xFFFFA22E)
private val DoorAmber = Color(0xFFF2A93B)

/**
 * A foto do carro com as luzes e portas por cima. As luzes acendem/apagam com fade curto (como lâmpada) e as
 * setas seguem o pisca da tela ([turnLeftLit]/[turnRightLit] já vêm piscando).
 */
@Composable
fun CarPhotoView(
    photo: ImageBitmap,
    spec: CarPhoto.Spec,
    headlights: Boolean,
    turnLeftLit: Boolean,
    turnRightLit: Boolean,
    doors: DashModel.Doors,
    modifier: Modifier = Modifier,
) {
    val head by animateFloatAsState(if (headlights) 1f else 0f, tween(if (headlights) 90 else 220), label = "head")
    val left by animateFloatAsState(if (turnLeftLit) 1f else 0f, tween(if (turnLeftLit) 60 else 140), label = "turnL")
    val right by animateFloatAsState(if (turnRightLit) 1f else 0f, tween(if (turnRightLit) 60 else 140), label = "turnR")
    val open = CarPhoto.openDoors(doors)
    val doorLevels = CarPhoto.Door.entries.associateWith { d ->
        animateFloatAsState(if (d in open) 1f else 0f, tween(250), label = "door$d").value
    }
    Canvas(modifier) {
        val (origin, size) = CarPhoto.fit(photo.width, photo.height, this.size)
        // Sombra no chão, embaixo do carro (a foto recortada não traz o chão).
        val shadowC = Offset(origin.x + size.width * 0.52f, origin.y + size.height * 0.93f)
        drawOval(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent), shadowC, size.width * 0.5f),
            topLeft = Offset(shadowC.x - size.width * 0.5f, shadowC.y - size.height * 0.1f),
            size = Size(size.width, size.height * 0.2f),
        )
        drawImage(
            photo,
            dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High,
        )
        // Portas abertas: realce âmbar + contorno.
        for ((door, outline) in spec.doors) {
            val a = doorLevels[door] ?: 0f
            if (a <= 0f || outline.size < 3) continue
            val path = Path().apply {
                outline.forEachIndexed { i, q ->
                    val x = origin.x + q.x * size.width; val y = origin.y + q.y * size.height
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, DoorAmber.copy(alpha = 0.22f * a))
            drawPath(path, DoorAmber.copy(alpha = 0.9f * a), style = Stroke(width = 3.dp.toPx()))
        }
        glows(spec.head, head, HeadGlow, origin, size, 2.4f)
        glows(spec.turnLeft, left, TurnGlow, origin, size, 2.2f)
        glows(spec.turnRight, right, TurnGlow, origin, size, 2.2f)
    }
}

/** Brilho de lâmpada: miolo forte e halo suave, somado à foto (não cobre o desenho do farol). */
private fun DrawScope.glows(list: List<CarPhoto.Glow>, level: Float, color: Color, origin: Offset, size: Size, spread: Float) {
    if (level <= 0f) return
    for (gl in list) {
        val c = Offset(origin.x + gl.x * size.width, origin.y + gl.y * size.height)
        val r = gl.r * size.width
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.55f * level), color.copy(alpha = 0.18f * level), Color.Transparent), c, r * spread),
            radius = r * spread, center = c, blendMode = BlendMode.Plus,
        )
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f * level), Color.Transparent), c, r * 0.6f),
            radius = r * 0.6f, center = c, blendMode = BlendMode.Plus,
        )
    }
}
