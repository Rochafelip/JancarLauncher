package com.rocha.jancarlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rocha.jancarlauncher.ui.car3d.CarScreen
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.theme.JancarTheme

/**
 * Prévia do carro 3D com estados falsos (só debug), para testar as animações sem o carro.
 * `adb shell am start -n com.rocha.jancarlauncher/.CarPreviewActivity --es open fl,rr,trunk --ez lights true --es turn left`
 * - open: portas abertas, separadas por vírgula (fl, fr, rl, rr, trunk, hood); sem o extra, tudo fechado.
 * - lights: farol aceso. turn: left | right | hazard. light: tema claro.
 * - rpm, speed, fuel (--ei): valores dos mostradores (padrão 873 rpm, 0 km/h, 49 L).
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
        setContent {
            JancarTheme(dark = !light) {
                CarScreen(reading, canConnected = true, onBack = { finish() })
            }
        }
    }
}
