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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.rocha.jancarlauncher.ui.components.JIconButton
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
 * Painel de velocidade/RPM, alimentado pelo serviço ICar da Jancar ([JancarApp.carService]). Mantém a única
 * conexão enquanto está aberto. Toque longo abre a camada de diagnóstico (valores crus do ICar e do CAN bus,
 * para a calibração na central).
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

    // CAN bus: odômetro total (o ICar não tem) + valores crus do diagnóstico. Uma conexão só, aberta aqui.
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
                    BasicText(
                        if (connected) "Serviço do carro conectado" else "Serviço do carro desconectado",
                        style = JancarType.appTag.copy(color = if (connected) c.muted else ZoneWarn),
                    )
                }

                Spacer(Modifier.height(14.dp))
                RpmBar(state.rpm)

                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1.4f)) {
                        BasicText(
                            DashModel.speedText(state.speed),
                            style = digits(156, FontWeight.W600).copy(color = if (state.speed == null) c.faint else c.fg),
                        )
                        BasicText("km/h", style = JancarType.tileLabel.copy(color = c.muted))
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        val rpmColor = when {
                            state.rpm == null -> c.faint
                            DashModel.shiftLight(state.rpm) -> ZoneRed
                            state.rpm >= DashModel.RPM_WARN -> ZoneWarn
                            else -> c.fg
                        }
                        BasicText(DashModel.rpmText(state.rpm), style = digits(84, FontWeight.W500).copy(color = rpmColor))
                        BasicText("rpm", style = JancarType.tileLabel.copy(color = c.muted))
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.Bottom) {
                    InfoItem("BATERIA", DashModel.valueText(state.batteryVoltage), "V")
                    InfoItem("COMBUSTÍVEL", DashModel.valueText(state.fuelLiters), "L")
                    InfoItem("AUTONOMIA", DashModel.valueText(state.fuelRangeKm), "km")
                    InfoItem("ODÔMETRO", DashModel.odoText(canDash?.odo), "km")
                    Spacer(Modifier.weight(1f))
                    HandbrakeBadge(state.handbrakeOn)
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

@Composable
private fun InfoItem(label: String, value: String, unit: String) {
    val c = JTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = JancarType.appTag.copy(color = c.faint))
        Row(verticalAlignment = Alignment.Bottom) {
            BasicText(value, style = digits(30, FontWeight.W500).copy(color = if (value == DashModel.DASH) c.faint else c.fg))
            Spacer(Modifier.width(6.dp))
            BasicText(unit, style = JancarType.body.copy(color = c.muted), modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

/** Freio de mão: pílula vermelha quando puxado; discreta quando solto ou desconhecido. */
@Composable
private fun HandbrakeBadge(on: Boolean?) {
    val c = JTheme.colors
    val (bg, fg, text) = when (on) {
        true -> Triple(ZoneRed, Color.White, "FREIO DE MÃO")
        false -> Triple(c.surface, c.faint, "FREIO SOLTO")
        null -> Triple(c.surface, c.faint, "FREIO --")
    }
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = JancarType.caption.copy(color = fg, letterSpacing = 1.5.sp))
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
