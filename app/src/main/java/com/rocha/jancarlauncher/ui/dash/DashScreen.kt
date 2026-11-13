package com.rocha.jancarlauncher.ui.dash

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.can.CarState
import com.rocha.jancarlauncher.can.CarStateDetails
import com.rocha.jancarlauncher.can.Cabin
import com.rocha.jancarlauncher.can.DashBoard
import com.rocha.jancarlauncher.can.Lights
import com.rocha.jancarlauncher.can.VehicleInfoDetails
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.components.JIconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.Inter
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.DarkColors
import com.rocha.jancarlauncher.ui.theme.LightColors
import com.rocha.jancarlauncher.ui.theme.JancarType

// Faixas de aviso (vermelha/âmbar) e SHIFT: blocos de cor próprios, iguais nos dois temas.
private val ZoneWarn = Color(0xFFF2A93B)
private val ZoneRed = Color(0xFFEF4444)
// Faixa SHIFT do modo bravo: amarelo com letra vermelha (pedido do usuário).
private val ShiftYellow = Color(0xFFFFD21F)
private val ShiftRed = Color(0xFFE11D1D)

/**
 * Cores do Painel que dependem do tema. No claro são mais escuras, pra continuar legíveis no fundo
 * branco: âmbar e vermelho também pintam texto (número do RPM, escala, nome do modo).
 */
internal data class DashPalette(
    val normal: Color,
    val warn: Color,
    val red: Color,
    /** Luz-espia acesa (setas, farol): verde, como no painel do carro. */
    val lampOn: Color,
    /** Segmento, luz-espia ou desenho apagado / sem dado: quase invisível. */
    val off: Color,
)

private val DarkDash = DashPalette(
    normal = Color(0xFF6FA3D8), warn = ZoneWarn, red = ZoneRed,
    lampOn = Color(0xFF22C55E), off = DarkColors.surface,
)

private val LightDash = DashPalette(
    normal = Color(0xFF2F6FB0), warn = Color(0xFFB45309), red = Color(0xFFB91C1C),
    lampOn = Color(0xFF15803D), off = Color(0xFFDDE0E4),
)

internal val dash: DashPalette
    @Composable get() = if (JTheme.colors == LightColors) LightDash else DarkDash

private fun DashModel.Zone.color(p: DashPalette) = when (this) {
    DashModel.Zone.Normal -> p.normal
    DashModel.Zone.Warn -> p.warn
    DashModel.Zone.Red -> p.red
}

/** Algarismos de largura fixa: o número não "dança" quando muda de 1 para 8. */
private fun digits(size: Int, weight: FontWeight) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = size.sp,
    fontFeatureSettings = "tnum",
)

/**
 * Painel de velocidade/RPM, alimentado pelo CAN bus ([JancarApp.canBus]) com o ICar da Jancar
 * ([JancarApp.carService]) de reserva; a origem de cada valor fica em [DashModel.reading]. Abre a conexão
 * única do CAN enquanto está na tela (a do ICar é do AppRoot). Toque longo abre o diagnóstico.
 */
@Composable
fun DashRoute(onBack: () -> Unit, onOpenCar: () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as JancarApp
    // ICar: conexão aberta pelo AppRoot (o farol comanda o modo noturno); aqui só lê.
    val client = app.carService
    val state by client.state.collectAsState()
    val connected by client.connected.collectAsState()

    // CAN bus: fonte principal do Painel + valores crus do diagnóstico. Uma conexão só, aberta aqui.
    val can = app.canBus
    DisposableEffect(can) {
        can.connect()
        onDispose { can.disconnect() }
    }
    val canConnected by can.connected.collectAsState()
    val canDash by can.dashboard.collectAsState()
    val canCabin by can.cabin.collectAsState()
    val canLights by can.lights.collectAsState()

    // Modo do aviso de troca: o Painel lembra o último escolhido.
    val savedMode by app.settings.driveMode.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    DashScreen(
        state, connected, onBack, canConnected, canDash, canCabin, canLights,
        mode = DashModel.DriveMode.fromName(savedMode),
        onModeChange = { scope.launch { app.settings.setDriveMode(it.name) } },
        onOpenCar = onOpenCar,
    )
}

@Composable
fun DashScreen(
    state: CarState,
    connected: Boolean,
    onBack: () -> Unit,
    canConnected: Boolean = false,
    canDash: DashBoard? = null,
    canCabin: Cabin? = null,
    canLights: Lights? = null,
    mode: DashModel.DriveMode = DashModel.DriveMode.Daily,
    onModeChange: (DashModel.DriveMode) -> Unit = {},
    /** Abre o carro 3D (botão do topo ou toque no desenho das portas). */
    onOpenCar: () -> Unit = {},
) {
    var diagnostics by remember { mutableStateOf(false) }
    BackHandler(enabled = diagnostics) { diagnostics = false }
    val r = DashModel.reading(state, canDash, canCabin, canLights)
    val alerts = DashModel.alerts(r, mode)
    val stage = DashModel.shiftStage(r.rpm, mode)
    // Um pisca só no ponto de troca: barra, escala, número e faixa SHIFT piscam juntos (pedido do usuário).
    val blink = if (stage == DashModel.ShiftStage.Shift) shiftBlink() else null
    // Conectar leva um instante: só avisa a falta de dados depois de alguns segundos.
    var graceOver by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(4_000); graceOver = true }
    val noCarData = graceOver && !canConnected

    // Segue o tema da Home (claro/escuro, inclusive o modo noturno pelo farol).
    val c = JTheme.colors
    val p = dash
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { diagnostics = true }) },
    ) {
        // Na central a barra de status da Jancar ocupa ~66px do topo: área útil ~534dp, por isso tudo compacto.
        Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 20.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                JIconButton(JIcons.Back, "Voltar", onBack)
                Spacer(Modifier.width(16.dp))
                BasicText("PAINEL", style = JancarType.caption.copy(color = c.faint))
                Spacer(Modifier.weight(1f))
                // Tudo certo = nada escrito aqui.
                if (noCarData) BasicText("Sem dados do carro", style = JancarType.appTag.copy(color = p.warn))
                Spacer(Modifier.width(20.dp))
                JIconButton(JIcons.Vehicle, "Carro 3D", onOpenCar)
                Spacer(Modifier.width(12.dp))
                ModeButton(mode) { onModeChange(mode.next()) }
            }

            Spacer(Modifier.height(14.dp))
            RpmBar(r.rpm, mode, blink)

            Row(
                Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1.4f)) {
                    BasicText(
                        DashModel.speedText(r.speed),
                        style = digits(156, FontWeight.W600).copy(color = if (r.speed == null) c.faint else c.fg),
                    )
                    BasicText("km/h", style = JancarType.tileLabel.copy(color = c.muted))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    val rpmColor = when {
                        r.rpm == null -> c.faint
                        stage == DashModel.ShiftStage.Shift -> p.red
                        stage == DashModel.ShiftStage.Near -> p.warn
                        else -> c.fg
                    }
                    BasicText(
                        DashModel.rpmText(r.rpm),
                        style = digits(84, FontWeight.W500).copy(color = rpmColor),
                        modifier = Modifier.blinking(blink),
                    )
                    BasicText("rpm", style = JancarType.tileLabel.copy(color = c.muted))
                }
            }

            // Faixa de aviso: só existe quando algo acontece.
            if (alerts.isNotEmpty()) {
                AlertBanner(alerts, r, blink)
                Spacer(Modifier.height(16.dp))
            }

            // Ícones no lugar de rótulos: bomba = combustível, termômetro = temperatura externa, estrada = odômetro.
            // À direita, as luzes-espia (setas e farol) e o carro visto de cima com as portas abertas.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    InfoItem(
                        JIcons.Fuel, DashModel.valueText(r.fuelLiters), "L",
                        highlight = if (DashModel.Alert.LowFuel in alerts) p.warn else null,
                    )
                    InfoItem(JIcons.Thermometer, DashModel.valueText(r.outsideTempC), "°C")
                    InfoItem(JIcons.Road, DashModel.odoText(r.odoKm), "km")
                }
                Spacer(Modifier.weight(1f))
                StatusLights(r)
                Spacer(Modifier.width(28.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(Dimens.RadiusButton))
                        .clickable(onClickLabel = "Abrir carro 3D", role = Role.Button, onClick = onOpenCar)
                        .padding(6.dp),
                ) { DoorsDiagram(r.doors) }
            }
        }

        if (diagnostics) {
            DiagnosticsLayer(onClose = { diagnostics = false }) {
                CarStateDetails(state, connected, Modifier.weight(1f))
                VehicleInfoDetails(canConnected, canDash, canCabin, canLights, Modifier.weight(1f))
            }
        }
    }
}

/**
 * Pisca de troca (1 → 0,25 a cada 160 ms). Criado só no ponto de troca e lido na fase de desenho
 * ([blinking]): o Painel não recompõe a cada quadro e, fora do ponto, nada fica animando.
 */
@Composable
private fun shiftBlink(): State<Float> =
    rememberInfiniteTransition(label = "shift").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse),
        label = "shiftBlink",
    )

/** Aplica o pisca de troca, se houver ([blink] `null` = fora do ponto de troca, não pisca). */
private fun Modifier.blinking(blink: State<Float>?): Modifier =
    if (blink == null) this else graphicsLayer { alpha = blink.value }

/**
 * Barra de giro segmentada. Os segmentos acesos ficam âmbar perto do ponto de troca do [mode] e
 * vermelhos a partir dele, piscando com [blink]; as faixas apagadas e a escala mostram onde fica o ponto.
 */
@Composable
private fun RpmBar(rpm: Float?, mode: DashModel.DriveMode, blink: State<Float>?) {
    val c = JTheme.colors
    val p = dash
    val lit = DashModel.litSegments(rpm)
    val stage = DashModel.shiftStage(rpm, mode)
    Column {
        Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until DashModel.SEGMENTS) {
                val on = i < lit
                val color = when {
                    !on -> p.off
                    stage == DashModel.ShiftStage.Shift -> p.red
                    stage == DashModel.ShiftStage.Near -> p.warn
                    else -> DashModel.zone(i, mode).color(p)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .then(if (on) Modifier.blinking(blink) else Modifier)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (on) color else DashModel.zone(i, mode).color(p).copy(alpha = 0.10f).compositeOver(color)),
                )
            }
        }
        // Escala x1000 rpm sob a barra; os números da faixa vermelha piscam junto no ponto de troca.
        BoxWithConstraints(Modifier.fillMaxWidth().height(22.dp).padding(top = 6.dp)) {
            val steps = DashModel.RPM_MAX / 1000
            for (k in 0..steps) {
                val x = maxWidth * (k.toFloat() / steps)
                val red = k * 1000 >= mode.shiftRpm
                BasicText(
                    "$k",
                    style = JancarType.appTag.copy(color = if (red) p.red else c.faint),
                    modifier = Modifier
                        .offset(x = (x - 4.dp).coerceIn(0.dp, maxWidth - 10.dp))
                        .then(if (red) Modifier.blinking(blink) else Modifier),
                )
            }
        }
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f,
    )
}

/** Valor com ícone grande à esquerda; [highlight] pinta ícone e número (ex.: reserva em âmbar). */
@Composable
internal fun InfoItem(icon: ImageVector, value: String, unit: String, highlight: Color? = null) {
    val c = JTheme.colors
    val unknown = value == DashModel.DASH
    Row(verticalAlignment = Alignment.CenterVertically) {
        JIcon(icon, tint = highlight ?: c.muted, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        BasicText(value, style = digits(36, FontWeight.W500).copy(color = highlight ?: if (unknown) c.faint else c.fg))
        Spacer(Modifier.width(6.dp))
        BasicText(unit, style = JancarType.body.copy(color = c.muted), modifier = Modifier.padding(top = 8.dp))
    }
}

/** Pisca das setas no Painel: meio ciclo aceso, meio apagado (1 piscada/s, pedido do usuário: "mais lento"). */
internal const val TURN_BLINK_HALF_MS = 500L

/**
 * O CAN do Fox manda a posição da alavanca (fica em 1 enquanto ligada), então quem pisca é o Painel.
 * Se a caixa um dia mandar a lâmpada acendendo e apagando, segura o "ligado" por este tempo pra não
 * piscar no ritmo dela.
 */
private const val TURN_HOLD_MS = 800L

/**
 * Luzes-espia: seta esquerda, farol e seta direita. Acesas em verde (como no painel do carro); apagadas ou
 * sem dado ficam quase invisíveis, pra não poluir. As setas piscam juntas (pisca-alerta = as duas).
 */
@Composable
private fun StatusLights(r: DashModel.Reading) {
    val left = latched(r.turnLeft == true)
    val right = latched(r.turnRight == true)
    val blinking = left || right
    var lit by remember { mutableStateOf(true) }
    LaunchedEffect(blinking) {
        lit = true
        while (blinking) {
            delay(TURN_BLINK_HALF_MS)
            lit = !lit
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Lamp(JIcons.TurnLeft, left && lit)
        Lamp(JIcons.Headlight, r.headlightOn)
        Lamp(JIcons.TurnRight, right && lit)
    }
}

/** `true` assim que [on] liga; só volta a `false` depois de [TURN_HOLD_MS] desligado. */
@Composable
internal fun latched(on: Boolean): Boolean {
    var held by remember { mutableStateOf(on) }
    LaunchedEffect(on) {
        if (on) held = true else { delay(TURN_HOLD_MS); held = false }
    }
    return held
}

@Composable
internal fun Lamp(icon: ImageVector, on: Boolean?) {
    val p = dash
    JIcon(icon, tint = if (on == true) p.lampOn else p.off, size = 36.dp)
}

/**
 * Carro visto de cima (frente pra cima): porta aberta aparece "aberta" em âmbar; porta-malas/capô abertos
 * pintam a traseira/dianteira. Sem dado de portas, o desenho fica apagado.
 */
@Composable
private fun DoorsDiagram(d: DashModel.Doors) {
    val c = JTheme.colors
    val p = dash
    val open = p.warn
    val body = if (d.known) c.muted else p.off
    Canvas(Modifier.width(48.dp).height(66.dp)) {
        // Desenhado numa grade de 52×72 e escalado.
        val k = size.height / 72f
        fun o(x: Float, y: Float) = Offset(x * k, y * k)
        val w = 2.2f * k
        drawRoundRect(body, topLeft = o(12f, 6f), size = Size(28f * k, 60f * k), cornerRadius = CornerRadius(9f * k), style = Stroke(w))
        drawLine(body, o(16f, 21f), o(36f, 21f), strokeWidth = w, cap = StrokeCap.Round) // para-brisa
        drawLine(body, o(17f, 56f), o(35f, 56f), strokeWidth = w, cap = StrokeCap.Round) // vidro traseiro
        val thick = 3.6f * k
        // Porta aberta: borda pintada + folha girada pra fora a partir da dobradiça (lado da frente).
        fun door(isOpen: Boolean?, x: Float, y: Float, out: Float) {
            if (isOpen != true) return
            drawLine(open, o(x, y), o(x, y + 14f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(open, o(x, y), o(x + out * 11f, y + 9f), strokeWidth = thick, cap = StrokeCap.Round)
        }
        door(d.frontLeft, 12f, 23f, -1f)
        door(d.rearLeft, 12f, 39f, -1f)
        door(d.frontRight, 40f, 23f, 1f)
        door(d.rearRight, 40f, 39f, 1f)
        if (d.hood == true) drawLine(open, o(17f, 6f), o(35f, 6f), strokeWidth = thick * 1.4f, cap = StrokeCap.Round)
        if (d.trunk == true) {
            drawLine(open, o(17f, 66f), o(35f, 66f), strokeWidth = thick * 1.4f, cap = StrokeCap.Round)
            drawLine(open, o(18f, 71f), o(34f, 71f), strokeWidth = thick, cap = StrokeCap.Round)
        }
    }
}

/** Faixa de aviso (vermelha = grave, âmbar = atenção). Com mais de um aviso, alterna a cada 3 s. */
@Composable
private fun AlertBanner(alerts: List<DashModel.Alert>, r: DashModel.Reading, blink: State<Float>?) {
    // Hora de trocar (modo bravo): enquanto durar, só a faixa SHIFT aparece.
    if (DashModel.Alert.Shift in alerts) {
        ShiftBanner(blink)
        return
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(alerts.size) {
        while (alerts.size > 1) { delay(3_000); tick++ }
    }
    val alert = alerts[tick % alerts.size]
    val (icon, title, detail) = when (alert) {
        DashModel.Alert.Shift -> return // tratado acima
        DashModel.Alert.DoorOpen -> Triple(JIcons.DoorOpen, "PORTA ABERTA", DashModel.openDoorsText(r.doors))
        DashModel.Alert.LowFuel -> Triple(JIcons.Fuel, "COMBUSTÍVEL NA RESERVA", "${DashModel.valueText(r.fuelLiters)} L no tanque")
    }
    val bg = if (alert.red) ZoneRed else ZoneWarn
    val fg = if (alert.red) Color.White else Color(0xFF0A0B0F)
    Row(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(16.dp)).background(bg).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JIcon(icon, tint = fg, size = 44.dp)
        Spacer(Modifier.width(18.dp))
        BasicText(title, style = JancarType.panelTitle.copy(color = fg, fontWeight = FontWeight.W600, letterSpacing = 1.sp))
        Spacer(Modifier.weight(1f))
        BasicText(detail, style = JancarType.body.copy(color = fg))
    }
}

/** Faixa amarela com "SHIFT" em vermelho, piscando junto com barra, escala e número (modo bravo, no ponto de troca). */
@Composable
private fun ShiftBanner(blink: State<Float>?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .blinking(blink)
            .clip(RoundedCornerShape(16.dp))
            .background(ShiftYellow),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            "SHIFT",
            style = TextStyle(
                fontFamily = Inter, fontWeight = FontWeight.W800, fontSize = 48.sp, lineHeight = 48.sp,
                letterSpacing = 12.sp, color = ShiftRed,
            ),
        )
    }
}

/** Botão do modo de condução (alterna dia a dia ↔ bravo); o nome do modo atual fica escrito nele. */
@Composable
private fun ModeButton(mode: DashModel.DriveMode, onClick: () -> Unit) {
    val c = JTheme.colors
    val accent = if (mode == DashModel.DriveMode.Sport) dash.warn else c.fg
    Row(
        Modifier
            .height(Dimens.MinTouch)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(c.surface)
            .clickable(onClickLabel = "Trocar modo de condução", role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JIcon(JIcons.Gauge, tint = accent, size = 20.dp)
        Spacer(Modifier.width(10.dp))
        BasicText(mode.label, style = JancarType.chip.copy(color = accent, letterSpacing = 1.sp))
    }
}

/** Valores crus para calibrar na central: ICar (estado do Painel) e CAN bus. Toque fora / voltar fecha. */
@Composable
private fun DiagnosticsLayer(onClose: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val c = JTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg.copy(alpha = 0.94f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClose() }) },
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JIconButton(JIcons.Back, "Fechar diagnóstico", onClose)
                Spacer(Modifier.width(16.dp))
                BasicText("DIAGNÓSTICO (calibração)", style = JancarType.caption.copy(color = c.faint))
            }
            Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                content()
            }
        }
    }
}
