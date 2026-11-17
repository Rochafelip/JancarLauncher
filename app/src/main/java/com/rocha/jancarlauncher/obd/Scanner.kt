package com.rocha.jancarlauncher.obd

/**
 * Leitura de falhas pela porta do OBD ([ObdClient.exclusive]): luz de injeção (0101), confirmados (03), pendentes (07)
 * e permanentes (0A, que muitos carros não têm). Só leitura. Se o adaptador cair no meio, devolve o que já leu com
 * [ScanResult.error].
 */
object Scanner {
    const val ERROR = "Não foi possível concluir a leitura"
    private const val TIMEOUT_MS = 6_000L

    fun run(link: ElmLink, wallMs: Long): ScanResult {
        var mil: MilStatus? = null
        var confirmed = emptyList<String>()
        var pending = emptyList<String>()
        var permanent: List<String>? = null
        val error = try {
            mil = Dtc.milStatus(link.send("0101"))
            val c = Dtc.parse(link.send("03", TIMEOUT_MS), 3)
            confirmed = c.orEmpty()
            val p = Dtc.parse(link.send("07", TIMEOUT_MS), 7)
            pending = p.orEmpty()
            permanent = Dtc.parse(link.send("0A", TIMEOUT_MS), 0x0A)
            val m = mil
            when {
                c == null || p == null -> ERROR
                // PID 01 é obrigatório: sem resposta, a central do motor não está falando (ignição desligada?) e o
                // "NO DATA" dos modos 03/07 não quer dizer "sem códigos".
                m == null -> ERROR
                // A central diz que tem códigos confirmados, mas o 03 veio vazio: leitura incompleta.
                m.count > 0 && confirmed.isEmpty() -> ERROR
                else -> null
            }
        } catch (_: Exception) {
            ERROR
        }
        return ScanResult(wallMs, mil, confirmed, pending, permanent, error)
    }
}

/** Ler falhas só com o carro parado (velocidade lida pelo OBD e até 3 km/h); sem leitura de velocidade, não. */
object ScanGate {
    const val MAX_KMH = 3f
    fun allowed(obdSpeedKmh: Float?): Boolean = obdSpeedKmh != null && obdSpeedKmh <= MAX_KMH
}
