package com.rocha.jancarlauncher.system

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Escopo do app (vida do processo) para os monitores (alarme de velocidade, saúde do motor). O launcher é a HOME do
 * carro: uma exceção num monitor não pode derrubar o app (e virar laço de crash); ela vai para o registro de eventos.
 */
fun appScope(log: RingLog): CoroutineScope = CoroutineScope(
    SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
        runCatching { Log.e("JancarApp", "monitor caiu", e) }
        log.add("app", "monitor caiu: ${e.javaClass.simpleName}: ${e.message}")
    },
)
