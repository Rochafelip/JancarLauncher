package com.rocha.jancarlauncher.obd

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Um comando ao ELM327 e a resposta até o prompt '>' (ou o tempo acabar). */
interface ElmLink {
    fun send(cmd: String, timeoutMs: Long = 1_500): String
}

/**
 * Porta única do socket do ELM327: o ciclo de leitura do [ObdClient] e os pedidos avulsos (leitura de falhas)
 * passam por aqui, um de cada vez, para as respostas nunca se misturarem. Sem conexão ([link] nulo) = `null`.
 */
class ElmGate {
    private val mutex = Mutex()
    @Volatile var link: ElmLink? = null

    suspend fun <T> use(block: (ElmLink) -> T): T? = mutex.withLock { link?.let(block) }
}
