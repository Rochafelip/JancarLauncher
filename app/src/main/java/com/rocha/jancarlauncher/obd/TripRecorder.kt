package com.rocha.jancarlauncher.obd

/** Uma viagem fechada (ou a atual): do 1º movimento ao último dado recebido. */
data class TripRecord(
    val startWallMs: Long,
    val endWallMs: Long,
    /** Tempo andando (velocidade > 0), s. */
    val movingS: Double,
    val km: Double,
    val liters: Double,
    /** Litros de etanol equivalentes ([TripCost.ethanolWeight]), para o custo em R$. */
    val ethanolLiters: Double,
    val maxKmh: Float,
) {
    val avgKmh: Double? get() = if (movingS >= 60.0) km / (movingS / 3600.0) else null
    val kmPerLiter: Double? get() = if (liters >= Trip.MIN_LITERS && km >= Trip.MIN_KM) km / liters else null
}

/**
 * Viagens separadas por ligada do carro (escolha do dono, 2026-11-17): começa quando o OBD conecta e o carro anda;
 * termina quando a sessão OBD cai (ignição desligada) e não volta em [graceMs] (parada rápida, ex.: posto, não
 * divide). O fim é o último dado recebido, nunca a hora em que a central acordou. Viagens com < 0,3 km são
 * descartadas. Tempo em `elapsedRealtime` (conta durante a suspensão); horário em parede só para mostrar.
 */
class TripRecorder(private val graceMs: Long = 180_000) {
    private var cur: TripRecord? = null
    /** Último dado de verdade (velocidade lida): a espera de [graceMs] conta daqui, nunca de quando a queda foi notada. */
    private var lastElapsed = 0L

    val current: TripRecord? @Synchronized get() = cur

    @Synchronized
    fun sample(elapsedMs: Long, wallMs: Long, speedKmh: Float?, litersPerHour: Float?, ethanol: Float, dtS: Double): TripRecord? {
        val finished = closeIfExpired(elapsedMs)
        // Sem velocidade (ignição em ACC, ECU desligada) não é dado: não abre nem estende a viagem.
        if (speedKmh == null) return finished
        val moving = speedKmh > 0f
        var t = cur
        if (t == null) {
            if (!moving) return finished
            t = TripRecord(wallMs, wallMs, 0.0, 0.0, 0.0, 0.0, 0f)
        }
        if (dtS > 0.0 && dtS <= Trip.MAX_GAP_S) {
            val l = (litersPerHour ?: 0f) * dtS / 3600.0
            t = t.copy(
                movingS = t.movingS + if (moving) dtS else 0.0,
                km = t.km + speedKmh * dtS / 3600.0,
                liters = t.liters + l,
                ethanolLiters = t.ethanolLiters + l * TripCost.ethanolWeight(ethanol),
            )
        }
        cur = t.copy(endWallMs = wallMs, maxKmh = maxOf(t.maxKmh, speedKmh))
        lastElapsed = elapsedMs
        return finished
    }

    /**
     * A sessão OBD caiu. Não muda nada: a espera já conta do último dado. (A queda pode ser notada só ao acordar da
     * suspensão, horas depois; contar dali juntaria a viagem de ontem com a de hoje.)
     */
    @Suppress("UNUSED_PARAMETER")
    fun disconnected(elapsedMs: Long, wallMs: Long) {}

    /** Chamado de tempos em tempos: fecha a viagem se a espera passou. */
    @Synchronized
    fun tick(elapsedMs: Long): TripRecord? = closeIfExpired(elapsedMs)

    private fun closeIfExpired(now: Long): TripRecord? {
        val t = cur ?: return null
        if (now - lastElapsed <= graceMs) return null
        cur = null
        return t.takeIf { it.km >= Trip.MIN_KM }
    }
}
