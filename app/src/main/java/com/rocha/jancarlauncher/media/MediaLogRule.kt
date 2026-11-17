package com.rocha.jancarlauncher.media

/**
 * Quando registrar uma mudança do serviço de mídia no `RingLog` (anel de 200 linhas que vai junto no relatório de
 * crash): sempre que o tipo muda (música ↔ rádio ↔ Bluetooth…) e, no mesmo tipo, no máximo uma amostra a cada 30 s.
 * Sem isso, a busca de estação do rádio (um `setMediaInfo` por passo de frequência) apagaria o registro inteiro.
 */
class MediaLogRule(private val sameTypeEveryMs: Long = 30_000L) {
    private var lastType: Int? = null
    private var lastAt = 0L

    fun shouldLog(type: Int, nowMs: Long): Boolean {
        val log = type != lastType || nowMs - lastAt >= sameTypeEveryMs
        if (log) { lastType = type; lastAt = nowMs }
        return log
    }
}
