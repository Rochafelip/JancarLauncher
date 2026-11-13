package com.rocha.jancarlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rocha.jancarlauncher.can.Cabin
import com.rocha.jancarlauncher.can.CarState
import com.rocha.jancarlauncher.can.DashBoard
import com.rocha.jancarlauncher.can.Lights
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.dash.DashScreen
import com.rocha.jancarlauncher.ui.theme.JancarTheme

/**
 * Prévia do Painel com dados falsos (só debug), pra conferir o layout sem o carro.
 * `adb shell am start -n com.rocha.jancarlauncher/.DashPreviewActivity --ez moving true`
 * (moving = andando a 62 km/h com porta aberta → aviso vermelho; sem o extra, parado em marcha lenta).
 * Aviso de troca: `--ez moving true --es mode Sport --ei rpm 5000` (mode = Daily | Sport).
 * Tema claro: `--ez light true` (sem o extra, escuro).
 */
class DashPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val moving = intent.getBooleanExtra("moving", false)
        val rpm = intent.getIntExtra("rpm", if (moving) 3120 else 873)
        val mode = DashModel.DriveMode.fromName(intent.getStringExtra("mode"))
        val light = intent.getBooleanExtra("light", false)
        // Sem porta aberta quando o teste é do aviso de troca (o SHIFT esconderia o resto de qualquer jeito).
        val door = if (intent.hasExtra("rpm")) 0 else 1
        val no = Int.MIN_VALUE
        val nf = Int.MIN_VALUE.toFloat()
        val dash = DashBoard(
            gearShiftPosition = 0, odo = 68039, cruisingRange = no, rotationRate = rpm,
            speed = if (moving) 62 else no, runningTime = no, fuelTankage = no, fuel = 49f, fuelPercentage = nf,
            avgFuelCont = nf, instFuelCont = nf, coolantTemp = nf, engineOilTemp = nf, inletTemp = nf,
            ambientTemp = 74.5f, accPedal = no, brakePedal = no, throttlePos = no,
        )
        val cabin = Cabin(
            bonnetStatus = no, flDoorStatus = door, frDoorStatus = 0, rlDoorStatus = 0, rrDoorStatus = 0,
            trunkStatus = door, electricTrunk = no, electricTrunkDirection = no, leftSafetyBelt = 1, rightSafetyBelt = 1,
            sunroofStatus = no, sunroofSunshadeStatus = no, flWindowStatus = no, frWindowStatus = no,
            rlWindowStatus = no, rrWindowStatus = no, lockCarStatus = no, seatUpDownMoveStatus = no,
            seatFrontRearMoveStatus = no, seatHighLowMoveStatus = no, seatBackUpDownMoveStatus = no,
            seatBackFrontRearMoveStatus = no,
        )
        setContent {
            var current by remember { mutableStateOf(mode) }
            JancarTheme(dark = !light) {
                DashScreen(
                    state = CarState(headlightOn = true),
                    connected = true,
                    onBack = { finish() },
                    canConnected = true,
                    canDash = dash,
                    canCabin = cabin,
                    canLights = Lights(leftDirectionIndicator = 1, rightDirectionIndicator = 0, emergencyFlasher = 0),
                    mode = current,
                    onModeChange = { current = it },
                )
            }
        }
    }
}
