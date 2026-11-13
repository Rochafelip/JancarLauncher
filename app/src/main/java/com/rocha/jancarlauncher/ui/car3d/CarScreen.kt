package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.dash.DashModel
import androidx.compose.ui.graphics.vector.ImageVector
import com.rocha.jancarlauncher.ui.dash.Lamp
import com.rocha.jancarlauncher.ui.dash.TURN_BLINK_HALF_MS
import com.rocha.jancarlauncher.ui.dash.dash
import com.rocha.jancarlauncher.ui.dash.latched
import com.rocha.jancarlauncher.ui.dash.blinking
import com.rocha.jancarlauncher.ui.dash.shiftBlink
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import kotlinx.coroutines.delay

/**
 * Tela do Carro (aberta pelo Painel): a FOTO real do Fox do dono no meio ([CarPhoto]), com faróis, setas e portas
 * abertas desenhados por cima a partir dos sinais reais, e os números do layout "C" em volta. Mesmos dados e mesma
 * conexão do Painel ([DashModel.reading]); nada aqui é simulado.
 */
@Composable
fun CarRoute(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as JancarApp
    val car by app.carService.state.collectAsState()
    // CAN: a conexão é contada (Painel e Carro podem estar na tela ao mesmo tempo durante a troca).
    val can = app.canBus
    DisposableEffect(can) {
        can.connect()
        onDispose { can.disconnect() }
    }
    val canConnected by can.connected.collectAsState()
    val canDash by can.dashboard.collectAsState()
    val canCabin by can.cabin.collectAsState()
    val canLights by can.lights.collectAsState()
    // Mesmo modo de troca do Painel (DIA A DIA / BRAVO): trocado lá, vale aqui.
    val savedMode by app.settings.driveMode.collectAsState(initial = null)
    CarScreen(
        DashModel.reading(car, canDash, canCabin, canLights), canConnected,
        DashModel.DriveMode.fromName(savedMode), onBack,
    )
}

@Composable
fun CarScreen(
    reading: DashModel.Reading,
    canConnected: Boolean,
    mode: DashModel.DriveMode = DashModel.DriveMode.Daily,
    onBack: () -> Unit,
) {
    val c = JTheme.colors
    val p = dash
    // Foto real do carro (escolha do dono, 2026-11-13: o 3D gerado por código não ficava realista).
    val photo by rememberCarPhoto()
    val r = reading

    // Setas: mesmo pisca do Painel (o CAN manda a alavanca fixa em 1); ícone e seta na foto piscam juntos.
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
    var graceOver by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(4_000); graceOver = true }
    val noCarData = graceOver && !canConnected

    Box(Modifier.fillMaxSize().background(c.bg)) {
        // Mesma moldura do Painel (área útil da central ~534dp de altura).
        Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 20.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                JIconButton(JIcons.Back, "Voltar", onBack)
                Spacer(Modifier.width(16.dp))
                BasicText("FOX", style = JancarType.caption.copy(color = c.faint))
                Spacer(Modifier.weight(1f))
                if (noCarData) BasicText("Sem dados do carro", style = JancarType.appTag.copy(color = p.warn))
                // Honestidade: foto de teste (ex.: faróis acesos nela) é avisada.
                if (photo is PhotoLoad.Ready && CarPhoto.SPEC.provisional) {
                    Spacer(Modifier.width(20.dp))
                    BasicText("FOTO PROVISÓRIA", style = JancarType.appTag.copy(color = c.faint))
                }
                Spacer(Modifier.width(20.dp))
                BasicText(
                    "${mode.label} · TROCA ${DashModel.rpmText(mode.shiftRpm.toFloat())}",
                    style = JancarType.appTag.copy(color = c.muted),
                )
            }

            // Layout "C" (escolhido no Claude Design): faixa de RPM no topo, velocidade grande à esquerda, giro à
            // direita e a foto do carro no meio.
            val stage = DashModel.shiftStage(r.rpm, mode)
            val blink = if (stage == DashModel.ShiftStage.Shift) shiftBlink() else null
            Spacer(Modifier.height(12.dp))
            RpmStrip(r.rpm, mode, blink)
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(SIDE_W)) {
                    BigReadout(
                        DashModel.speedText(r.speed), "KM/H", 92,
                        if (r.speed == null) c.faint else c.fg, alignEnd = false,
                    )
                    Spacer(Modifier.height(18.dp))
                    FuelBar(r.fuelLiters, low = DashModel.Alert.LowFuel in DashModel.alerts(r))
                }
                Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    when (val ph = photo) {
                        is PhotoLoad.Ready -> CarPhotoView(
                            ph.image, CarPhoto.SPEC,
                            headlights = r.headlightOn == true,
                            turnLeftLit = left && lit,
                            turnRightLit = right && lit,
                            doors = r.doors,
                            modifier = Modifier.fillMaxSize(),
                        )
                        PhotoLoad.Missing -> BasicText("Foto do carro não encontrada", style = JancarType.body.copy(color = c.faint))
                        PhotoLoad.Loading -> {}
                    }
                }
                Column(Modifier.width(SIDE_W), horizontalAlignment = Alignment.End) {
                    BasicText(
                        if (stage == DashModel.ShiftStage.Shift) "TROQUE" else " ",
                        style = JancarType.appTag.copy(color = p.red, letterSpacing = 3.sp),
                        modifier = Modifier.blinking(blink),
                    )
                    val rpmColor = when {
                        r.rpm == null -> c.faint
                        stage == DashModel.ShiftStage.Shift -> p.red
                        stage == DashModel.ShiftStage.Near -> p.warn
                        else -> c.fg
                    }
                    BigReadout(
                        DashModel.rpmText(r.rpm), "RPM", 60, rpmColor, alignEnd = true,
                        modifier = Modifier.blinking(blink),
                    )
                }
            }

            // Embaixo: luzes-espia e portas, como no Painel.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lamp(JIcons.TurnLeft, left && lit)
                    Lamp(JIcons.Headlight, r.headlightOn)
                    Lamp(JIcons.TurnRight, right && lit)
                }
                Spacer(Modifier.width(40.dp))
                DoorsStatus(r.doors, Modifier.weight(1f))
                Spacer(Modifier.width(24.dp))
                // O que o visor do painel original mostra: temperatura externa e hodômetro.
                SmallStat(JIcons.Thermometer, DashModel.valueText(r.outsideTempC), "°C")
                Spacer(Modifier.width(36.dp))
                SmallStat(null, DashModel.odoText(r.odoKm), "km")
            }
        }
    }
}

/** Largura das colunas dos números (velocidade / giro) dos lados do carro. */
private val SIDE_W = 200.dp

/** Valor pequeno da linha de baixo (temperatura externa, hodômetro). */
@Composable
private fun SmallStat(icon: ImageVector?, value: String, unit: String) {
    val c = JTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            JIcon(icon, tint = c.muted, size = 28.dp)
            Spacer(Modifier.width(10.dp))
        }
        val unknown = value == DashModel.DASH
        BasicText(value, style = JancarType.tileLabel.copy(color = if (unknown) c.faint else c.fg, fontFeatureSettings = "tnum"))
        Spacer(Modifier.width(5.dp))
        BasicText(unit, style = JancarType.body.copy(color = c.muted))
    }
}

/** Situação das portas por extenso: só ganha cor quando algo está aberto. */
@Composable
private fun DoorsStatus(d: DashModel.Doors, modifier: Modifier = Modifier) {
    val c = JTheme.colors
    val open = d.anyOpen
    val color = when {
        open -> dash.warn
        d.known -> c.muted
        else -> c.faint
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        JIcon(JIcons.DoorOpen, tint = color, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        val text = when {
            open -> "Aberto: ${DashModel.openDoorsText(d)}"
            d.known -> "Tudo fechado"
            else -> "Portas sem dado"
        }
        // Tudo aberto não cabe ao lado da temperatura e do hodômetro: corta com "…" em vez de espremer o resto.
        BasicText(text, style = JancarType.body.copy(color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

