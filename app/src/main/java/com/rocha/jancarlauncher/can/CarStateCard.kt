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
 * Fase de calibracao: valores lidos do [JancarCarClient] (instancia unica do app, ver
 * [JancarApp.carService] -- nunca cria uma propria). Comparar com a tela "Car Info" original.
 */
@Composable
fun CarStateCard(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as JancarApp
    val client = app.carService

    DisposableEffect(client) {
        client.connect()
        onDispose { client.disconnect() }
    }

    val connected by client.connected.collectAsState()
    val s by client.state.collectAsState()
    CarStateDetails(s, connected, modifier)
}

/**
 * Conteúdo do card sem gerenciar conexão: para quem já mantém o [JancarCarClient] conectado (ex.: o Painel).
 * Um segundo `connect()`/`disconnect()` no cliente compartilhado derrubaria a conexão de quem a abriu.
 */
@Composable
fun CarStateDetails(s: CarState, connected: Boolean, modifier: Modifier = Modifier) {
    val colors = JTheme.colors
    val style = JancarType.body.copy(color = colors.fg)

    fun f(v: Float?) = v?.let { "%.1f".format(it) } ?: "--"

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            "Serviço Jancar: ${if (connected) "conectado" else "desconectado"}",
            style = JancarType.panelTitle.copy(color = colors.fg),
        )
        BasicText("Velocidade: ${f(s.speed)}   RPM: ${f(s.rpm)}", style = style)
        BasicText(
            "Bateria: ${f(s.batteryVoltage)} V   Combustível: ${f(s.fuelLiters)} L   Autonomia: ${f(s.fuelRangeKm)}",
            style = style,
        )
        BasicText(
            "Freio de mão: ${s.handbrakeOn ?: "--"}   ACC: ${s.accOn ?: "--"}   Power: ${s.powerStatus ?: "--"}",
            style = style,
        )
        BasicText(
            "Portas FL/FR/RL/RR/Capô/Mala: " +
                listOf(0, 1, 2, 3, 4, 6).joinToString("/") { s.isDoorOpen(it)?.let { o -> if (o) "A" else "F" } ?: "-" } +
                "   (mask=${s.doorMask})",
            style = style,
        )
        BasicText(
            "Luzes mask=${s.lightMask}   Temp externa: ${f(s.outsideTempC)} °C (raw=${s.outsideTempRaw})",
            style = style,
        )
    }
}
