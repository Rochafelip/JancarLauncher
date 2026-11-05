package com.rocha.jancarlauncher.boot

import android.content.Context
import androidx.core.content.edit

/** Histórico de reinícios por crash. Precisa ficar em disco: o processo que crasha morre junto com a memória. */
interface RestartLog {
    fun read(): List<Long>
    fun write(times: List<Long>)
}

/**
 * Limita reinícios automáticos após crash a [max] por [windowMs], para não entrar em laço de crash.
 * Tentativas negadas não entram no histórico (o bloqueio termina junto com a janela original).
 * Registros "no futuro" (relógio da central mudou) são descartados em vez de bloquear para sempre.
 */
class RestartPolicy(
    private val log: RestartLog,
    private val max: Int = 3,
    private val windowMs: Long = 60_000,
) {
    fun allow(nowMs: Long): Boolean {
        val recent = log.read().filter { nowMs - it in 0 until windowMs }
        if (recent.size >= max) {
            log.write(recent)
            return false
        }
        log.write(recent + nowMs)
        return true
    }
}

/** Guarda o histórico em SharedPreferences com commit() síncrono (o processo está morrendo). */
class SharedPrefsRestartLog(context: Context) : RestartLog {
    private val prefs = context.applicationContext.getSharedPreferences("crash_restarts", Context.MODE_PRIVATE)

    override fun read(): List<Long> =
        prefs.getString(KEY, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }

    // commit síncrono de propósito: quem chama está no handler de crash e o processo vai morrer em seguida.
    override fun write(times: List<Long>) {
        prefs.edit(commit = true) { putString(KEY, times.joinToString(",")) }
    }

    private companion object { const val KEY = "times" }
}
