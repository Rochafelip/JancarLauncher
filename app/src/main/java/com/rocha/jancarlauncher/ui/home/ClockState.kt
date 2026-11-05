package com.rocha.jancarlauncher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime

/** Relógio atualizado pelo tick de minuto do sistema (sem loops); o receiver é removido ao sair da tela. */
@Composable
fun rememberClock(): State<ClockInfo> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(ClockFormatter.format(LocalDateTime.now())) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                state.value = ClockFormatter.format(LocalDateTime.now())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        state.value = ClockFormatter.format(LocalDateTime.now()) // pode ter passado minutos desde a composição
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
