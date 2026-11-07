package com.rocha.jancarlauncher.can

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/**
 * Fase de calibração: mostra valores crus lidos do [CanBusClient] (instância única do app, ver
 * [JancarApp.canBus] -- nunca cria uma própria, pra não abrir uma conexão por tela). Use pra
 * conferir a ordem dos campos do Parcel contra o carro real (abrir/fechar portas, ligar setas,
 * comparar velocidade com o painel) antes de usar esses dados em qualquer UI definitiva.
 */
@Composable
fun VehicleInfoCard(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as JancarApp
    val client = app.canBus

    DisposableEffect(client) {
        client.connect()
        onDispose { client.disconnect() }
    }

    val connected by client.connected.collectAsState()
    val dash by client.dashboard.collectAsState()
    val cabin by client.cabin.collectAsState()
    val lights by client.lights.collectAsState()
    VehicleInfoDetails(connected, dash, cabin, lights, modifier)
}

/**
 * Conteúdo do card sem gerenciar conexão: para quem já mantém o [CanBusClient] conectado (ex.: o Painel).
 * Um segundo `connect()`/`disconnect()` no cliente compartilhado derrubaria a conexão de quem a abriu.
 */
@Composable
fun VehicleInfoDetails(
    connected: Boolean,
    dash: DashBoard?,
    cabin: Cabin?,
    lights: Lights?,
    modifier: Modifier = Modifier,
) {
    val colors = JTheme.colors
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            "CAN: ${if (connected) "conectado" else "desconectado"}",
            style = JancarType.panelTitle.copy(color = colors.fg),
        )
        dash?.let {
            BasicText("Velocidade: ${it.speed}   RPM: ${it.rotationRate}", style = JancarType.body.copy(color = colors.fg))
            BasicText(
                "Combustível: ${it.fuelPercentage}%  (${it.fuel})  Autonomia: ${it.cruisingRange}",
                style = JancarType.body.copy(color = colors.fg),
            )
            BasicText("Marcha: ${it.gearShiftPosition}   Odô: ${it.odo}", style = JancarType.body.copy(color = colors.fg))
            BasicText(
                "Temp motor: ${it.coolantTemp}   Externa: ${it.ambientTemp}",
                style = JancarType.body.copy(color = colors.fg),
            )
        }
        cabin?.let {
            BasicText(
                "Portas FL/FR/RL/RR: ${it.flDoorStatus}/${it.frDoorStatus}/${it.rlDoorStatus}/${it.rrDoorStatus}",
                style = JancarType.body.copy(color = colors.fg),
            )
            BasicText("Porta-malas: ${it.trunkStatus}   Capô: ${it.bonnetStatus}", style = JancarType.body.copy(color = colors.fg))
            BasicText(
                "Cintos E/D: ${it.leftSafetyBelt}/${it.rightSafetyBelt}   Trava: ${it.lockCarStatus}",
                style = JancarType.body.copy(color = colors.fg),
            )
        }
        lights?.let {
            BasicText(
                "Setas E/D: ${it.leftDirectionIndicator}/${it.rightDirectionIndicator}  Pisca: ${it.emergencyFlasher}",
                style = JancarType.body.copy(color = colors.fg),
            )
        }
    }
}
