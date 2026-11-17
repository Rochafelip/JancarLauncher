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
import com.rocha.jancarlauncher.can.Cabin
import com.rocha.jancarlauncher.can.CarState
import com.rocha.jancarlauncher.can.DashBoard
import com.rocha.jancarlauncher.can.Lights
import com.rocha.jancarlauncher.obd.BatteryAlert
import com.rocha.jancarlauncher.obd.FuelModel
import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.obd.Trip
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.dash.DashScreen
import com.rocha.jancarlauncher.ui.theme.JancarTheme

/**
 * Prévia do Painel com dados falsos (só debug), pra conferir o layout sem o carro.
 * `adb shell am start -n com.rocha.jancarlauncher/.DashPreviewActivity --ez moving true`
 * (moving = andando a 62 km/h com porta aberta → aviso vermelho; sem o extra, parado em marcha lenta).
 * Aviso de troca: `--ez moving true --es mode Sport --ei rpm 3850` (mode = Daily | Sport).
 * Tema claro: `--ez light true` (sem o extra, escuro).
 * Consumo (OBD falso): `--ez obd true` (conectado, com média agora e da viagem); sem o extra, OBD desligado.
 * Modo DEMO: `--ez demo true [--es mode Sport]` roda em loop o roteiro do [FoxDemo] (o mesmo da tela Fox) no Painel.
 */
class DashPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val moving = intent.getBooleanExtra("moving", false)
        val rpm = intent.getIntExtra("rpm", if (moving) 3120 else 873)
        val mode = DashModel.DriveMode.fromName(intent.getStringExtra("mode"))
        val light = intent.getBooleanExtra("light", false)
        val obd = intent.getBooleanExtra("obd", false)
        // Avisos de saúde pelo OBD: --ez hot true (água 112 °C) / --es battery NotCharging|HighVoltage|Weak.
        val battery = intent.getStringExtra("battery")?.let { b -> BatteryAlert.entries.firstOrNull { it.name == b } }
        val health = DashModel.Health(
            battery = battery,
            overheat = intent.getBooleanExtra("hot", false),
            coolantC = 112f,
            batteryV = when (battery) { BatteryAlert.HighVoltage -> 15.2f; BatteryAlert.Weak -> 12.0f; else -> 12.4f },
        )
        if (intent.getBooleanExtra("demo", false)) {
            setContent { JancarTheme(dark = !light) { DemoDash(mode) { finish() } } }
            return
        }
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
            var blend by remember { mutableStateOf(FuelModel.Blend.Gasolina) }
            var trip by remember { mutableStateOf(if (obd) Trip(liters = 2.9, km = 37.4) else Trip()) }
            var prices by remember { mutableStateOf<Pair<Int, Int>?>(null) }
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
                    obdLive = if (!obd) ObdClient.Live() else ObdClient.Live(
                        ObdClient.Status.Connected, "OBDII", speedKmh = if (moving) 62f else 0f,
                        litersPerHour = if (moving) 4.4f else 0.7f, kmPerLiter = if (moving) 14.1f else null,
                    ),
                    obdTrip = trip,
                    fuelBlend = blend,
                    onResetTrip = { trip = Trip() },
                    onNextBlend = { blend = blend.next() },
                    health = health,
                    gasCents = prices?.first,
                    ethCents = prices?.second,
                    onPricesChange = { g, e -> prices = g to e },
                )
            }
        }
    }
}

/** Modo DEMO do Painel: o roteiro do [FoxDemo] convertido para os dados do CAN, com a etiqueta DEMO por cima. */
@Composable
private fun DemoDash(startMode: DashModel.DriveMode, onBack: () -> Unit) {
    var seconds by remember { mutableStateOf(0.0) }
    var trip by remember { mutableStateOf(Trip(liters = 10.3, km = 133.2)) }
    var mode by remember { mutableStateOf(startMode) }
    var blend by remember { mutableStateOf(FuelModel.Blend.Gasolina) }
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
    val r = frame.reading
    val no = Int.MIN_VALUE
    val nf = Int.MIN_VALUE.toFloat()
    fun Boolean?.can() = when (this) { true -> 1; false -> 0; null -> no }
    val dash = DashBoard(
        gearShiftPosition = 0, odo = r.odoKm ?: no, cruisingRange = no, rotationRate = r.rpm?.toInt() ?: no,
        speed = r.speed?.toInt() ?: no, runningTime = no, fuelTankage = no, fuel = r.fuelLiters ?: nf,
        fuelPercentage = nf, avgFuelCont = nf, instFuelCont = nf, coolantTemp = nf, engineOilTemp = nf, inletTemp = nf,
        // O CAN entrega a temperatura externa somada de 40 (DashModel tira de volta).
        ambientTemp = r.outsideTempC?.let { it + 40f } ?: nf, accPedal = no, brakePedal = no, throttlePos = no,
    )
    val cabin = Cabin(
        bonnetStatus = r.doors.hood.can(), flDoorStatus = r.doors.frontLeft.can(), frDoorStatus = r.doors.frontRight.can(),
        rlDoorStatus = r.doors.rearLeft.can(), rrDoorStatus = r.doors.rearRight.can(), trunkStatus = r.doors.trunk.can(),
        electricTrunk = no, electricTrunkDirection = no, leftSafetyBelt = 1, rightSafetyBelt = 1,
        sunroofStatus = no, sunroofSunshadeStatus = no, flWindowStatus = no, frWindowStatus = no,
        rlWindowStatus = no, rrWindowStatus = no, lockCarStatus = no, seatUpDownMoveStatus = no,
        seatFrontRearMoveStatus = no, seatHighLowMoveStatus = no, seatBackUpDownMoveStatus = no,
        seatBackFrontRearMoveStatus = no,
    )
    Box(Modifier.fillMaxSize()) {
        DashScreen(
            state = CarState(headlightOn = r.headlightOn),
            connected = true,
            onBack = onBack,
            canConnected = true,
            canDash = dash,
            canCabin = cabin,
            canLights = Lights(r.turnLeft.can(), r.turnRight.can(), no),
            mode = mode,
            onModeChange = { mode = it },
            obdLive = frame.obd,
            obdTrip = trip,
            fuelBlend = blend,
            onResetTrip = { trip = Trip() },
            onNextBlend = { blend = blend.next() },
        )
        BasicText(
            "DEMO",
            style = TextStyle(color = Color(0xFF0A0B0F), fontSize = 13.sp, fontWeight = FontWeight.W600, letterSpacing = 2.sp),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 26.dp)
                .background(Color(0xFFFFB43A), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
