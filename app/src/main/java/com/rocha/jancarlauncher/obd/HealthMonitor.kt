package com.rocha.jancarlauncher.obd

import android.os.SystemClock
import com.rocha.jancarlauncher.system.RingLog
import com.rocha.jancarlauncher.ui.dash.DashModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Bateria e água pelo OBD, avaliadas o tempo todo (vida do processo), para o Painel, a tela Fox e o cartão sobre o
 * AA mostrarem o mesmo aviso. Um tique por segundo faz as regras andarem mesmo quando a leitura não muda.
 */
class HealthMonitor(
    obd: StateFlow<ObdClient.Live>,
    scope: CoroutineScope,
    private val log: RingLog? = null,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val battery = BatteryRule()
    private val heat = OverheatRule()
    private var last = DashModel.Health()

    private val ticks = flow { while (true) { emit(Unit); delay(1_000) } }

    val health: StateFlow<DashModel.Health> = combine(obd, ticks) { l, _ ->
        val now = clock()
        val h = DashModel.Health(
            battery = battery.update(l.batteryV, l.rpm, now),
            overheat = heat.update(l.coolantC, now),
            coolantC = l.coolantC,
            batteryV = l.batteryV,
        )
        if (h.battery != last.battery || h.overheat != last.overheat) {
            log?.add("saude", "bateria=${h.battery ?: "ok"} (${h.batteryV} V), água quente=${h.overheat} (${h.coolantC} °C)")
        }
        last = h
        h
    }.stateIn(scope, SharingStarted.Eagerly, DashModel.Health())
}
