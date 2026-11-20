package com.rocha.jancarlauncher.wheel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.rocha.jancarlauncher.system.RingLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Escuta os avisos de tecla do volante que a central manda por broadcast ([WheelKeys.ACTION]). Só leitura: não
 * mexe nas teclas nem as consome. Registrado no `Application`, então continua ouvindo com o Android Auto na frente.
 * Guarda os últimos [CAPACITY] avisos (mais novo primeiro) para a tela de teste em Ajustes › Teclas do volante.
 */
class WheelKeyMonitor(private val context: Context, private val log: RingLog) {
    private val _events = MutableStateFlow<List<WheelEvent>>(emptyList())
    val events: StateFlow<List<WheelEvent>> = _events.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val i = intent ?: return
            val id = i.getIntExtra(WheelKeys.EXTRA_ID, -1)
            if (id < 0) return
            record(WheelEvent(id, i.getIntExtra(WheelKeys.EXTRA_TYPE, -1), System.currentTimeMillis()))
        }
    }

    fun register() {
        val filter = IntentFilter(WheelKeys.ACTION)
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            else context.registerReceiver(receiver, filter)
        }.onFailure { log.add("volante", "não registrou o receiver: ${it.message}") }
    }

    internal fun record(e: WheelEvent) {
        _events.value = (listOf(e) + _events.value).take(CAPACITY)
        log.add("volante", "tecla ${e.id} (${e.name}) ${e.typeLabel}")
    }

    fun clear() { _events.value = emptyList() }

    private companion object { const val CAPACITY = 40 }
}
