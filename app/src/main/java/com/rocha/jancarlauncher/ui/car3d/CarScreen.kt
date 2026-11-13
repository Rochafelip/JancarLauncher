package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.dash.DashModel
import androidx.compose.ui.graphics.vector.ImageVector
import com.rocha.jancarlauncher.ui.dash.Lamp
import com.rocha.jancarlauncher.ui.dash.TURN_BLINK_HALF_MS
import com.rocha.jancarlauncher.ui.dash.dash
import com.rocha.jancarlauncher.ui.dash.latched
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import com.rocha.jancarlauncher.ui.theme.LightColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Modelo do carro dentro do APK. Trocar o arquivo (mesmos nomes de nós) troca o carro, sem mexer no código. */
const val CAR_MODEL_ASSET = "car/fox.glb"

/**
 * Carro 3D (aberto pelo Painel): o Fox no meio, girando com o dedo, com portas, porta-malas e luzes seguindo o
 * carro de verdade. Mesmos dados e mesma conexão do Painel ([DashModel.reading]); nada aqui é simulado.
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
    CarScreen(DashModel.reading(car, canDash, canCabin, canLights), canConnected, onBack)
}

@Composable
fun CarScreen(reading: DashModel.Reading, canConnected: Boolean, onBack: () -> Unit) {
    val c = JTheme.colors
    val p = dash
    val load by rememberCarScene(CAR_MODEL_ASSET)
    val r = reading

    // Setas: mesmo pisca do Painel (o CAN manda a alavanca fixa em 1); ícone e lanterna 3D piscam juntos.
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

    val dark = c != LightColors
    val renderState = CarRenderState(
        doors = r.doors,
        headlights = r.headlightOn == true,
        turnLeftLit = left && lit,
        turnRightLit = right && lit,
        dark = dark,
        background = c.bg.toArgb(),
    )

    Box(Modifier.fillMaxSize().background(c.bg)) {
        // Mesma moldura do Painel (área útil da central ~534dp de altura).
        Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 20.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                JIconButton(JIcons.Back, "Voltar", onBack)
                Spacer(Modifier.width(16.dp))
                BasicText("FOX", style = JancarType.caption.copy(color = c.faint))
                Spacer(Modifier.weight(1f))
                if (noCarData) BasicText("Sem dados do carro", style = JancarType.appTag.copy(color = p.warn))
                // Honestidade: enquanto o modelo comprado não chega, avisa que o desenho é provisório.
                if ((load as? SceneLoad.Ready)?.scene?.placeholder == true) {
                    Spacer(Modifier.width(20.dp))
                    BasicText("MODELO PROVISÓRIO", style = JancarType.appTag.copy(color = c.faint))
                }
            }

            // Como o painel original do Fox: conta-giros | (visor → aqui o carro 3D) | velocímetro + combustível.
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Tachometer(r.rpm, accent = p.normal, diameter = GAUGE_SIZE)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    when (val l = load) {
                        is SceneLoad.Ready -> Car3D(l.scene, renderState, Modifier.fillMaxSize())
                        is SceneLoad.Failed -> BasicText("Modelo 3D não carregou", style = JancarType.body.copy(color = c.faint))
                        SceneLoad.Loading -> {}
                    }
                    GestureHint(Modifier.align(Alignment.BottomCenter))
                }
                Speedometer(
                    r.speed, r.fuelLiters,
                    lowFuel = DashModel.Alert.LowFuel in DashModel.alerts(r),
                    accent = p.normal, diameter = GAUGE_SIZE,
                )
            }

            // Embaixo: luzes-espia e portas, como no Painel.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lamp(JIcons.TurnLeft, left && lit)
                    Lamp(JIcons.Headlight, r.headlightOn)
                    Lamp(JIcons.TurnRight, right && lit)
                }
                Spacer(Modifier.width(40.dp))
                DoorsStatus(r.doors)
                Spacer(Modifier.weight(1f))
                // O que o visor do painel original mostra: temperatura externa e hodômetro.
                SmallStat(JIcons.Thermometer, DashModel.valueText(r.outsideTempC), "°C")
                Spacer(Modifier.width(36.dp))
                SmallStat(null, DashModel.odoText(r.odoKm), "km")
            }
        }
    }
}

/** Diâmetro dos mostradores: cabe na altura útil da central (~534dp) com o carro ainda grande no meio. */
private val GAUGE_SIZE = 240.dp

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
private fun DoorsStatus(d: DashModel.Doors) {
    val c = JTheme.colors
    val open = d.anyOpen
    val color = when {
        open -> dash.warn
        d.known -> c.muted
        else -> c.faint
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        JIcon(JIcons.DoorOpen, tint = color, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        val text = when {
            open -> "Aberto: ${DashModel.openDoorsText(d)}"
            d.known -> "Tudo fechado"
            else -> "Portas sem dado"
        }
        BasicText(text, style = JancarType.body.copy(color = color))
    }
}

/** Dica dos gestos: aparece ao abrir e some sozinha (a tela fica limpa). */
@Composable
private fun GestureHint(modifier: Modifier) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(6_000); visible = false }
    AnimatedVisibility(visible, modifier = modifier, exit = fadeOut(tween(600))) {
        BasicText(
            "Arraste para girar · pinça para aproximar · dois toques centraliza",
            style = JancarType.appTag.copy(color = JTheme.colors.faint),
        )
    }
}

@Composable
private fun Car3D(scene: GlbScene, state: CarRenderState, modifier: Modifier) {
    var view by remember { mutableStateOf<CarGlView?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { view?.onPause() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { view?.onResume() }
    AndroidView(
        factory = { ctx -> CarGlView(ctx, scene).also { view = it } },
        update = { it.update(state) },
        modifier = modifier,
    )
}

sealed interface SceneLoad {
    data object Loading : SceneLoad
    class Ready(val scene: GlbScene) : SceneLoad
    class Failed(val message: String?) : SceneLoad
}

/** Lê o .glb fora da thread da tela (o modelo comprado pode ter alguns MB). */
@Composable
private fun rememberCarScene(asset: String): State<SceneLoad> {
    val context = LocalContext.current.applicationContext
    return produceState<SceneLoad>(SceneLoad.Loading, asset) {
        value = withContext(Dispatchers.IO) {
            runCatching { Glb.parse(context.assets.open(asset).use { it.readBytes() }) }
                .fold({ SceneLoad.Ready(it) }, { SceneLoad.Failed(it.message) })
        }
    }
}
