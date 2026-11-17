package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.obd.Trip
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.dash.GearModel
import com.rocha.jancarlauncher.ui.dash.ModeButton
import com.rocha.jancarlauncher.ui.dash.shiftBlink
import com.rocha.jancarlauncher.ui.dash.rememberGear
import com.rocha.jancarlauncher.ui.dash.TURN_BLINK_HALF_MS
import com.rocha.jancarlauncher.ui.dash.dash
import com.rocha.jancarlauncher.ui.dash.latched
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tela do Fox (aberta pelo Painel). Arranjo "A" escolhido no Claude Design (2026-11-14): o **painel de instrumentos
 * original** do carro redesenhado da foto do dono ([FoxCluster]) — conta-giros à esquerda, velocímetro à direita — e,
 * no meio, a **foto real** do Fox ([CarPhoto]) com faróis/setas/portas por cima, setas do painel e o visor âmbar.
 * Antes repetia o Painel (faixa de RPM e números); agora cada tela tem seu papel. Mesmos dados do Painel
 * ([DashModel.reading]) + consumo/viagem do OBD; nada aqui é simulado.
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
    val obdLive by app.obd.live.collectAsState()
    val obdTrip by app.obd.trip.collectAsState()
    val savedMode by app.settings.driveMode.collectAsState(initial = null)
    val health by app.health.health.collectAsState()
    val scope = rememberCoroutineScope()
    CarScreen(
        DashModel.reading(car, canDash, canCabin, canLights), canConnected, onBack, obdLive, obdTrip,
        DashModel.DriveMode.fromName(savedMode),
        onModeChange = { scope.launch { app.settings.setDriveMode(it.name) } },
        health = health,
    )
}

@Composable
fun CarScreen(
    reading: DashModel.Reading,
    canConnected: Boolean,
    onBack: () -> Unit,
    obdLive: ObdClient.Live = ObdClient.Live(),
    trip: Trip = Trip(),
    mode: DashModel.DriveMode = DashModel.DriveMode.Daily,
    onModeChange: (DashModel.DriveMode) -> Unit = {},
    /** Tamanho/intensidade das luzes da foto (a prévia de debug troca para testar; o app usa [CarPhoto.TUNING]). */
    lightTuning: CarPhoto.Tuning = CarPhoto.TUNING,
    /** Avisos de bateria/água pelo OBD: acende a luz da bateria no conta-giros. */
    health: DashModel.Health = DashModel.Health(),
) {
    // Painel de carro é escuro de verdade: esta tela fica sempre no tema escuro.
    JancarTheme(dark = true) {
        CarScreenContent(reading, canConnected, onBack, obdLive, trip, mode, onModeChange, lightTuning, health)
    }
}

@Composable
private fun CarScreenContent(
    reading: DashModel.Reading,
    canConnected: Boolean,
    onBack: () -> Unit,
    obdLive: ObdClient.Live,
    trip: Trip,
    mode: DashModel.DriveMode,
    onModeChange: (DashModel.DriveMode) -> Unit,
    lightTuning: CarPhoto.Tuning,
    health: DashModel.Health,
) {
    val c = JTheme.colors
    val photo by rememberCarPhoto()
    val r = reading

    // Setas: mesmo pisca do Painel (o CAN manda a alavanca fixa em 1); seta do painel e da foto piscam juntas.
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
    // Marcha e sugestão de troca (mesmo modo DIA A DIA / BRAVO do Painel), no visor como nos VW mais novos.
    val gear = rememberGear(r.rpm, r.speed)
    val gearHint = GearModel.hintText(GearModel.suggest(gear, r.rpm, r.speed, mode))

    Box(Modifier.fillMaxSize().background(ClusterBg)) {
        // Área útil da central ~534dp de altura (a barra da Jancar ocupa o topo).
        Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                JIconButton(JIcons.Back, "Voltar", onBack)
                Spacer(Modifier.width(16.dp))
                BasicText("FOX", style = JancarType.caption.copy(color = c.faint))
                Spacer(Modifier.weight(1f))
                if (noCarData) BasicText("Sem dados do carro", style = JancarType.appTag.copy(color = dash.warn))
                Spacer(Modifier.width(20.dp))
                // Modo do shift light (o mesmo do Painel: trocado aqui, vale lá).
                ModeButton(mode) { onModeChange(mode.next()) }
            }

            Box(Modifier.fillMaxWidth().weight(1f)) {
                val atShift = DashModel.shiftStage(r.rpm, mode) == DashModel.ShiftStage.Shift
                AnalogGauge(
                    GaugeKind.Tach, r.rpm, FoxCluster.coolantFraction(obdLive), lamp = r.headlightOn ?: false,
                    size = GAUGE, modifier = Modifier.align(Alignment.CenterStart),
                    shiftMode = mode,
                    shiftBlink = if (atShift) shiftBlink() else null,
                    battery = health.batteryV?.let { health.battery != null },
                )
                AnalogGauge(
                    GaugeKind.Speed, r.speed, FoxCluster.fuelFraction(r.fuelLiters), lamp = null,
                    size = GAUGE, modifier = Modifier.align(Alignment.CenterEnd),
                )
                // Foto maior (pedido do dono, 2026-11-15: "aumentar o tamanho do carro"): a coluna sobe para o topo,
                // onde os relógios redondos se afastam do meio, e a foto passa por cima do espaço entre eles.
                Column(
                    Modifier.align(Alignment.TopCenter).padding(top = PHOTO_TOP).width(PHOTO_W),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(width = PHOTO_W, height = PHOTO_H), contentAlignment = Alignment.Center) {
                        when (val ph = photo) {
                            is PhotoLoad.Ready -> CarPhotoView(
                                ph.image, CarPhoto.SPEC,
                                headlights = r.headlightOn == true,
                                turnLeftLit = left && lit,
                                turnRightLit = right && lit,
                                doors = r.doors,
                                modifier = Modifier.fillMaxSize(),
                                fogLights = r.fogOn == true,
                                tuning = lightTuning,
                            )
                            PhotoLoad.Missing -> BasicText("Foto do carro não encontrada", style = JancarType.body.copy(color = c.faint))
                            PhotoLoad.Loading -> {}
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClusterArrow(left = true, on = left && lit)
                        ClusterLcd(r.outsideTempC, r.odoKm, trip, gear = gear, hint = gearHint)
                        ClusterArrow(left = false, on = right && lit)
                    }
                    // Portas abertas: balão com o nome em cima de cada uma, na própria foto (CarPhotoView).
                }
            }
        }
    }
}

private val GAUGE = 330.dp
/** Caixa da foto do carro (a foto tem proporção ~1,78:1). Antes 290×156; maior que isso encosta nos relógios. */
private val PHOTO_W = 372.dp
private val PHOTO_H = 210.dp
private val PHOTO_TOP = 4.dp
private val ClusterBg = Color(0xFF060709)
