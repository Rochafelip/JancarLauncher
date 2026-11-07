package com.rocha.jancarlauncher.ui.dash

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.Inter
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

// Cores das faixas de giro (só do Painel): normal no acento azul-aço da marca, depois âmbar e vermelho.
private val ZoneNormal = Color(0xFF6FA3D8)
private val ZoneWarn = Color(0xFFF2A93B)
private val ZoneRed = Color(0xFFEF4444)

private fun DashModel.Zone.color() = when (this) {
    DashModel.Zone.Normal -> ZoneNormal
    DashModel.Zone.Warn -> ZoneWarn
    DashModel.Zone.Red -> ZoneRed
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
 * ([JancarApp.carService]) de reserva; a origem de cada valor fica em [DashModel.reading]. Mantém as únicas
 * conexões enquanto está aberto. Toque longo abre a camada de diagnóstico (valores crus do ICar e do CAN bus).
 */
@Composable
fun DashRoute(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as JancarApp
    val client = app.carService
    DisposableEffect(client) {
        client.connect()
        onDispose { client.disconnect() }
    }
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

    DashScreen(state, connected, onBack, canConnected, canDash, canCabin, canLights)
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
) {
    var diagnostics by remember { mutableStateOf(false) }
    BackHandler(enabled = diagnostics) { diagnostics = false }
    val r = DashModel.reading(state, canDash)
    val alerts = DashModel.alerts(r)
    // Conectar leva um instante: só avisa a falta de dados depois de alguns segundos.
    var graceOver by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(4_000); graceOver = true }
    val noCarData = graceOver && !canConnected

    // Painel é sempre escuro, independente do tema da Home.
    JancarTheme(dark = true) {
        val c = JTheme.colors
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
                    if (noCarData) BasicText("Sem dados do carro", style = JancarType.appTag.copy(color = ZoneWarn))
                }

                Spacer(Modifier.height(14.dp))
                RpmBar(r.rpm)

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
                            DashModel.shiftLight(r.rpm) -> ZoneRed
                            r.rpm >= DashModel.RPM_WARN -> ZoneWarn
                            else -> c.fg
                        }
                        BasicText(DashModel.rpmText(r.rpm), style = digits(84, FontWeight.W500).copy(color = rpmColor))
                        BasicText("rpm", style = JancarType.tileLabel.copy(color = c.muted))
                    }
                }

                // Faixa de aviso: só existe quando algo acontece.
                if (alerts.isNotEmpty()) {
                    AlertBanner(alerts, r)
                    Spacer(Modifier.height(16.dp))
                }

                // Ícones no lugar de rótulos: bomba = combustível, termômetro = temperatura externa, estrada = odômetro.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InfoItem(
                        JIcons.Fuel, DashModel.valueText(r.fuelLiters), "L",
                        highlight = if (DashModel.Alert.LowFuel in alerts) ZoneWarn else null,
                    )
                    InfoItem(JIcons.Thermometer, DashModel.valueText(r.outsideTempC), "°C")
                    InfoItem(JIcons.Road, DashModel.odoText(r.odoKm), "km")
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
}

/** Barra de giro segmentada; acima de [DashModel.RPM_SHIFT] os segmentos acesos piscam (hora da troca). */
@Composable
private fun RpmBar(rpm: Float?) {
    val c = JTheme.colors
    val lit = DashModel.litSegments(rpm)
    val shift = DashModel.shiftLight(rpm)
    val blink by rememberInfiniteTransition(label = "shift").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse),
        label = "shiftBlink",
    )
    Column {
        Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until DashModel.SEGMENTS) {
                val on = i < lit
                val color = when {
                    !on -> c.surface
                    shift -> ZoneRed.copy(alpha = blink)
                    else -> DashModel.zone(i).color()
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (on) color else DashModel.zone(i).color().copy(alpha = 0.10f).compositeOver(color)),
                )
            }
        }
        // Escala x1000 rpm sob a barra.
        BoxWithConstraints(Modifier.fillMaxWidth().height(22.dp).padding(top = 6.dp)) {
            val steps = DashModel.RPM_MAX / 1000
            for (k in 0..steps) {
                val x = maxWidth * (k.toFloat() / steps)
                BasicText(
                    "$k",
                    style = JancarType.appTag.copy(color = if (k * 1000 >= DashModel.RPM_RED) ZoneRed else c.faint),
                    modifier = Modifier.offset(x = (x - 4.dp).coerceIn(0.dp, maxWidth - 10.dp)),
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
private fun InfoItem(icon: ImageVector, value: String, unit: String, highlight: Color? = null) {
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

/** Faixa de aviso (vermelha = grave, âmbar = atenção). Com mais de um aviso, alterna a cada 3 s. */
@Composable
private fun AlertBanner(alerts: List<DashModel.Alert>, r: DashModel.Reading) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(alerts.size) {
        while (alerts.size > 1) { delay(3_000); tick++ }
    }
    val alert = alerts[tick % alerts.size]
    val (icon, title, detail) = when (alert) {
        DashModel.Alert.HandbrakeWhileMoving -> Triple(JIcons.Handbrake, "FREIO DE MÃO PUXADO", "Carro em movimento")
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
