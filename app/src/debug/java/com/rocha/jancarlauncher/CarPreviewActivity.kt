package com.rocha.jancarlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.obd.Trip
import com.rocha.jancarlauncher.ui.car3d.CarScreen
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.theme.JancarTheme

/**
 * Prévia da tela do Carro com estados falsos (só debug), para testar luzes, setas e portas sem o carro.
 * `adb shell am start -n com.rocha.jancarlauncher/.CarPreviewActivity --es open fl,rr,trunk --ez lights true --es turn left`
 * - open: portas abertas, separadas por vírgula (fl, fr, rl, rr, trunk, hood); sem o extra, tudo fechado.
 * - lights: farol aceso. turn: left | right | hazard. light: tema claro.
 * - rpm, speed, fuel (--ei): valores dos mostradores (padrão 873 rpm, 0 km/h, 49 L).
 * - obd (--ez): OBD falso conectado (consumo no relógio pequeno e no visor); sem o extra, sem OBD.
 * - mode (--es): Daily | Sport (modo do shift light; o botão do topo também troca).
 * - demo (--ez): roteiro animado de ~32 s em loop (FoxDemo) com tudo funcionando; etiqueta DEMO na tela.
 * Reabrir com outros extras (`-f 0x10008000` limpa a tarefa) para ver a porta abrindo/fechando.
 */
class CarPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val open = intent.getStringExtra("open").orEmpty().split(',').map { it.trim() }.toSet()
        val turn = intent.getStringExtra("turn")
        val reading = DashModel.Reading(
            speed = intent.getIntExtra("speed", 0).toFloat(),
            rpm = intent.getIntExtra("rpm", 873).toFloat(),
            fuelLiters = intent.getIntExtra("fuel", 49).toFloat(),
            outsideTempC = 34.5f,
            odoKm = 68039,
            doors = DashModel.Doors(
                frontLeft = "fl" in open, frontRight = "fr" in open,
                rearLeft = "rl" in open, rearRight = "rr" in open,
                trunk = "trunk" in open, hood = if ("hood" in open) true else null,
            ),
            headlightOn = intent.getBooleanExtra("lights", false),
            turnLeft = turn == "left" || turn == "hazard",
            turnRight = turn == "right" || turn == "hazard",
        )
        val light = intent.getBooleanExtra("light", false)
        val obd = intent.getBooleanExtra("obd", false)
        val demo = intent.getBooleanExtra("demo", false)
        setContent {
            JancarTheme(dark = !light) {
                if (demo) { DemoCar { finish() }; return@JancarTheme }
                var mode by remember { mutableStateOf(DashModel.DriveMode.fromName(intent.getStringExtra("mode"))) }
                CarScreen(
                    reading, canConnected = true, onBack = { finish() },
                    obdLive = if (obd) ObdClient.Live(
                        ObdClient.Status.Connected, speedKmh = reading.speed,
                        litersPerHour = 4.4f, kmPerLiter = if ((reading.speed ?: 0f) > 3f) 14.1f else null, coolantC = 90f,
                    ) else ObdClient.Live(),
                    trip = if (obd) Trip(liters = 10.3, km = 133.2) else Trip(),
                    mode = mode, onModeChange = { mode = it },
                )
            }
        }
    }
}

/** Modo DEMO: roda o roteiro do [FoxDemo] a ~20 quadros/s, somando o trip, com a etiqueta DEMO por cima. */
@Composable
private fun DemoCar(onBack: () -> Unit) {
    var seconds by remember { mutableStateOf(0.0) }
    var trip by remember { mutableStateOf(Trip(liters = 10.3, km = 133.2)) }
    var mode by remember { mutableStateOf(DashModel.DriveMode.Daily) }
    LaunchedEffect(Unit) {
        val start = System.nanoTime()
        var last = start
        while (true) {
            delay(50)
            val now = System.nanoTime()
            val dt = (now - last) / 1e9
            last = now
            seconds = (now - start) / 1e9
            val f = FoxDemo.at(seconds, mode)
            trip = trip.add(f.obd.litersPerHour ?: 0f, f.reading.speed ?: 0f, dt)
        }
    }
    val frame = FoxDemo.at(seconds, mode)
    Box(Modifier.fillMaxSize()) {
        CarScreen(
            frame.reading, canConnected = true, onBack = onBack, obdLive = frame.obd, trip = trip,
            mode = mode, onModeChange = { mode = it },
        )
        BasicText(
            "DEMO",
            style = TextStyle(color = Color(0xFF0A0B0F), fontSize = 13.sp, fontWeight = FontWeight.W600, letterSpacing = 2.sp),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 26.dp)
                .background(Color(0xFFFFB43A), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
