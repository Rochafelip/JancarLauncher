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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.theme.Inter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * O carro na tela do Carro como FOTO real do Fox do dono (escolha dele, 2026-11-13: o 3D gerado por código não
 * ficava realista; a câmera já era fixa). A foto é recortada do fundo e tem a placa em branco; por cima, o app
 * desenha só o que vem dos sinais reais: brilho nos faróis (fio de iluminação), setas piscando (CAN) e, em cada
 * porta aberta, um balão com o nome dela apontando para a porta (pedido do dono, 2026-11-15: "avisar qual porta";
 * o contorno âmbar de antes não agradou). As portas do passageiro, que não aparecem nesse ângulo, apontam para o
 * teto do lado de lá.
 *
 * Sem o arquivo [ASSET] a tela avisa que a foto não foi encontrada.
 */
object CarPhoto {
    const val ASSET = "car/photo.png"
    /** Máscara da lataria (mesmo tamanho da foto; `tools/car-photo/mascara.ps1`). */
    const val MASK_ASSET = "car/body_mask.png"

    /** Posição de um brilho: centro (x, y) e raio, em fração da LARGURA/ALTURA da foto (raio: da largura). */
    data class Glow(val x: Float, val y: Float, val r: Float)

    enum class Door(val label: String) {
        FrontLeft("MOTORISTA"), FrontRight("PASSAGEIRO"), RearLeft("TRASEIRA ESQ."), RearRight("TRASEIRA DIR."),
        Trunk("PORTA-MALAS"), Hood("CAPÔ"),
    }

    /** O que se desenha por cima da foto, em coordenadas da foto (0..1). */
    data class Spec(
        val head: List<Glow>,
        /** Farol de milha (lâmpada redonda no para-choque). */
        val fog: List<Glow>,
        val turnLeft: List<Glow>,
        val turnRight: List<Glow>,
        /** Para onde o balão de cada porta aberta aponta (todas as portas; as escondidas, no lado de lá do carro). */
        val doors: Map<Door, Offset>,
        /** Foto de teste (ex.: faróis já acesos nela): a tela avisa "FOTO PROVISÓRIA". */
        val provisional: Boolean,
    )

    /** Balão de porta aberta já posicionado, em px da caixa da foto: retângulo e o ponto da porta. */
    data class Tag(val door: Door, val left: Float, val top: Float, val width: Float, val height: Float, val anchor: Offset) {
        val right get() = left + width
        val bottom get() = top + height
    }

    /**
     * Posiciona os balões: cada um centrado acima do seu ponto ([lift] de folga para a haste), sem sair dos lados da
     * caixa ([boxWidth]); se encostar em outro, sobe até ficar livre ([gap] entre eles). Os de baixo são colocados
     * primeiro, então quem sobe é o de cima — a haste continua ligando o balão à porta.
     */
    fun placeTags(items: List<Pair<Door, Offset>>, sizes: Map<Door, Size>, boxWidth: Float, lift: Float, gap: Float): List<Tag> {
        val placed = mutableListOf<Tag>()
        for ((door, a) in items.sortedByDescending { it.second.y }) {
            val s = sizes[door] ?: continue
            val left = (a.x - s.width / 2f).coerceIn(0f, (boxWidth - s.width).coerceAtLeast(0f))
            var top = a.y - lift - s.height
            // Cada balão já colocado empurra no máximo uma vez (depois de subir, fica acima dele): sempre termina.
            while (true) {
                val hit = placed.firstOrNull { o ->
                    left < o.right + gap && left + s.width > o.left - gap && top < o.bottom + gap && top + s.height > o.top - gap
                } ?: break
                top = hit.top - gap - s.height
            }
            placed += Tag(door, left, top, s.width, s.height, a)
        }
        return placed
    }

    // Foto definitiva (2026-11-15, pedido do dono: sem os reflexos das árvores): docs/Fox-photos/
    // "Volkswagen Fox Prata Isolado em PNG.png" (3/4 pela frente do lado do MOTORISTA, faróis apagados, luz
    // uniforme, fundo já transparente). Recorte de 1535×863 no "quadro de trabalho" de tools/car-photo/foto.ps1
    // (a foto como veio, 1536×1024), começando em (1, 87); o arquivo do app é esse recorte reduzido a 80%.
    // As medidas abaixo estão no quadro de trabalho.
    private const val W = 1535f
    private const val H = 863f
    private const val OX = 1
    private const val OY = 87
    private fun p(x: Int, y: Int) = Offset((x - OX) / W, (y - OY) / H)
    private fun g(x: Int, y: Int, r: Int) = Glow((x - OX) / W, (y - OY) / H, r / W)

    val SPEC = Spec(
        // As duas lâmpadas do farol de perto (esquerdo) e o farol de lá, na quina ao lado da grade.
        head = listOf(g(523, 588, 24), g(638, 560, 30), g(46, 590, 16)),
        // Farol de milha do lado de perto (o de lá fica escondido atrás da quina do para-choque).
        fog = listOf(g(596, 763, 20)),
        // Lado do motorista é o de perto: seta na faixa de baixo do farol e repetidor no retrovisor.
        turnLeft = listOf(g(470, 632, 20), g(1068, 353, 18)),
        // Lado do passageiro: só a quina da frente aparece (parte de baixo do farol de lá).
        turnRight = listOf(g(42, 618, 12)),
        // Ponto de cada porta: meio da porta visível; as do passageiro (do lado de lá) no teto, sobre o lado delas.
        doors = mapOf(
            Door.FrontLeft to p(1120, 520),
            Door.RearLeft to p(1340, 430),
            // Porta-malas: alto da tampa traseira (na lanterna, o balão cobria o da traseira esquerda).
            Door.Trunk to p(1440, 200),
            Door.Hood to p(430, 450),
            Door.FrontRight to p(700, 160),
            Door.RearRight to p(1230, 115),
        ),
        provisional = false,
    )

    /**
     * Tamanho e intensidade de um grupo de luzes (pedido do dono, 2026-11-15: "editar o tamanho da intensidade da
     * luz normais e de milha"). [size] multiplica o raio do halo e do miolo; [power] multiplica o brilho (acima de 1
     * desenha o halo em mais de uma camada somada, até 3×). 1 = como era antes.
     */
    data class Intensity(val size: Float, val power: Float)

    /** Ajuste das luzes da foto. Para mudar: troque os números aqui (testar com a prévia, ver CarPreviewActivity). */
    data class Tuning(val head: Intensity, val fog: Intensity)

    val TUNING = Tuning(
        head = Intensity(size = 1.3f, power = 1.5f),
        fog = Intensity(size = 1.4f, power = 1.6f),
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
    /** [body] = só a lataria (foto × máscara), para trocar a cor; `null` = sem máscara no APK (sem troca de cor). */
    class Ready(val image: ImageBitmap, val body: ImageBitmap? = null) : PhotoLoad
}

/** Lê a foto (e monta a camada da lataria, uma vez) fora da thread da tela. */
@Composable
fun rememberCarPhoto(): State<PhotoLoad> {
    val context = LocalContext.current.applicationContext
    return produceState<PhotoLoad>(PhotoLoad.Loading) {
        value = withContext(Dispatchers.IO) {
            val photo = runCatching { context.assets.open(CarPhoto.ASSET).use { BitmapFactory.decodeStream(it) } }.getOrNull()
                ?: return@withContext PhotoLoad.Missing
            val mask = runCatching { context.assets.open(CarPhoto.MASK_ASSET).use { BitmapFactory.decodeStream(it) } }.getOrNull()
            PhotoLoad.Ready(photo.asImageBitmap(), mask?.let { bodyLayer(photo, it) }?.asImageBitmap())
        }
    }
}

/** Foto × máscara (DST_IN): só os pixels da lataria, com a borda suave da máscara. */
private fun bodyLayer(photo: android.graphics.Bitmap, mask: android.graphics.Bitmap): android.graphics.Bitmap? = runCatching {
    val out = photo.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
    val c = android.graphics.Canvas(out)
    val p = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
    }
    c.drawBitmap(mask, null, android.graphics.Rect(0, 0, out.width, out.height), p)
    mask.recycle()
    out
}.getOrNull()

private val HeadGlow = Color(0xFFDCE9FF)
/** Lâmpada halógena do farol de milha: branco mais quente que o farol. */
private val FogGlow = Color(0xFFFFEBB8)
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
    fogLights: Boolean = false,
    tuning: CarPhoto.Tuning = CarPhoto.TUNING,
    /** Só a lataria ([PhotoLoad.Ready.body]); com [paint] diferente de Original, é desenhada tingida por cima. */
    body: ImageBitmap? = null,
    paint: CarPaint = CarPaint.Original,
) {
    val paintFilter = remember(paint) { paint.matrix()?.let { ColorFilter.colorMatrix(ColorMatrix(it)) } }
    val head by animateFloatAsState(if (headlights) 1f else 0f, tween(if (headlights) 90 else 220), label = "head")
    val fog by animateFloatAsState(if (fogLights) 1f else 0f, tween(if (fogLights) 90 else 220), label = "fog")
    val left by animateFloatAsState(if (turnLeftLit) 1f else 0f, tween(if (turnLeftLit) 60 else 140), label = "turnL")
    val right by animateFloatAsState(if (turnRightLit) 1f else 0f, tween(if (turnRightLit) 60 else 140), label = "turnR")
    val open = CarPhoto.openDoors(doors)
    val doorLevels = CarPhoto.Door.entries.associateWith { d ->
        animateFloatAsState(if (d in open) 1f else 0f, tween(250), label = "door$d").value
    }
    // Nomes das portas medidos uma vez (texto fixo).
    val measurer = rememberTextMeasurer()
    val labels = remember(measurer) { CarPhoto.Door.entries.associateWith { measurer.measure(it.label, TagText) } }
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
        // Cor nova só na lataria: a camada recortada, tingida pela luminância (reflexos e sombras continuam).
        if (body != null && paintFilter != null) {
            drawImage(
                body,
                dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.High,
                colorFilter = paintFilter,
            )
        }
        glows(spec.head, head, HeadGlow, origin, size, 2.4f, tuning.head)
        glows(spec.fog, fog, FogGlow, origin, size, 2.4f, tuning.fog)
        glows(spec.turnLeft, left, TurnGlow, origin, size, 2.2f)
        glows(spec.turnRight, right, TurnGlow, origin, size, 2.2f)
        doorTags(spec, doorLevels, labels, origin, size)
    }
}

private val TagText = TextStyle(
    fontFamily = Inter, fontWeight = FontWeight.W700, fontSize = 11.sp, letterSpacing = 0.6.sp, color = Color(0xFF17130B),
)

/**
 * Balões das portas abertas: âmbar com o nome da porta, haste fina até um ponto na porta. Aparecem/somem com o
 * mesmo fade de antes ([levels]); a posição é recalculada a cada quadro só com os balões visíveis.
 */
private fun DrawScope.doorTags(
    spec: CarPhoto.Spec, levels: Map<CarPhoto.Door, Float>, labels: Map<CarPhoto.Door, TextLayoutResult>,
    origin: Offset, size: Size,
) {
    val visible = spec.doors.filterKeys { (levels[it] ?: 0f) > 0f }
    if (visible.isEmpty()) return
    val padX = 8.dp.toPx(); val padY = 4.dp.toPx()
    val sizes = visible.keys.associateWith { d ->
        val t = labels.getValue(d).size
        Size(t.width + 2 * padX, t.height + 2 * padY)
    }
    val anchors = visible.map { (d, q) -> d to Offset(origin.x + q.x * size.width, origin.y + q.y * size.height) }
    val tags = CarPhoto.placeTags(anchors, sizes, this.size.width, lift = 9.dp.toPx(), gap = 4.dp.toPx())
    // Primeiro todas as hastes (da base do balão, na vertical do ponto se couber, até o ponto na porta), depois os
    // balões: assim nenhum ponto fica desenhado por cima do nome de outra porta.
    for (t in tags) {
        val a = levels[t.door] ?: 0f
        val stemX = t.anchor.x.coerceIn(t.left + 6.dp.toPx(), t.right - 6.dp.toPx())
        drawLine(Color.Black.copy(alpha = 0.55f * a), Offset(stemX, t.bottom), t.anchor, 3.5.dp.toPx(), StrokeCap.Round)
        drawLine(DoorAmber.copy(alpha = a), Offset(stemX, t.bottom), t.anchor, 1.6.dp.toPx(), StrokeCap.Round)
        drawCircle(Color.Black.copy(alpha = 0.6f * a), 5.dp.toPx(), t.anchor)
        drawCircle(DoorAmber.copy(alpha = a), 3.5.dp.toPx(), t.anchor)
    }
    for (t in tags) {
        val a = levels[t.door] ?: 0f
        // Sombra leve + balão.
        val r = CornerRadius(t.height / 2f)
        drawRoundRect(Color.Black.copy(alpha = 0.45f * a), Offset(t.left, t.top + 1.5.dp.toPx()), Size(t.width, t.height), r)
        drawRoundRect(DoorAmber.copy(alpha = a), Offset(t.left, t.top), Size(t.width, t.height), r)
        drawText(labels.getValue(t.door), topLeft = Offset(t.left + padX, t.top + padY), alpha = a)
    }
}

private val NORMAL = CarPhoto.Intensity(1f, 1f)

/**
 * Brilho de lâmpada: miolo forte e halo suave, somado à foto (não cobre o desenho do farol). [k] aumenta o raio
 * ([CarPhoto.Intensity.size]) e o brilho ([CarPhoto.Intensity.power]: camadas somadas, cada uma no máximo opaca).
 */
private fun DrawScope.glows(
    list: List<CarPhoto.Glow>, level: Float, color: Color, origin: Offset, size: Size, spread: Float,
    k: CarPhoto.Intensity = NORMAL,
) {
    if (level <= 0f) return
    val power = k.power.coerceIn(0f, 3f)
    val layers = kotlin.math.ceil(power).toInt().coerceAtLeast(1)
    val per = power / layers
    for (gl in list) {
        val c = Offset(origin.x + gl.x * size.width, origin.y + gl.y * size.height)
        val r = gl.r * size.width * k.size.coerceIn(0.2f, 4f)
        repeat(layers) {
            drawCircle(
                Brush.radialGradient(
                    listOf(color.copy(alpha = (0.55f * per * level).coerceAtMost(1f)),
                        color.copy(alpha = (0.18f * per * level).coerceAtMost(1f)), Color.Transparent),
                    c, r * spread,
                ),
                radius = r * spread, center = c, blendMode = BlendMode.Plus,
            )
        }
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = (0.85f * power * level).coerceAtMost(1f)), Color.Transparent), c, r * 0.6f),
            radius = r * 0.6f, center = c, blendMode = BlendMode.Plus,
        )
    }
}
