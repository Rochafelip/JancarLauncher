package com.rocha.jancarlauncher.obd

/** Média da viagem: litros e km somados desde o último "zerar" (guardados entre ligações do carro). */
data class Trip(val liters: Double = 0.0, val km: Double = 0.0) {
    /**
     * Soma um intervalo de [dtSeconds] rodando a [speedKmh] e gastando [litersPerHour]. Intervalos estranhos
     * (≤ 0 ou mais de [MAX_GAP_S], ex.: o carro desligou no meio) não entram: não dá para saber o que houve.
     */
    fun add(litersPerHour: Float, speedKmh: Float, dtSeconds: Double): Trip {
        if (dtSeconds <= 0.0 || dtSeconds > MAX_GAP_S) return this
        return Trip(liters + litersPerHour * dtSeconds / 3600.0, km + speedKmh * dtSeconds / 3600.0)
    }

    /** km/L da viagem; `null` enquanto não há o bastante para uma média honesta. */
    val kmPerLiter: Double?
        get() = if (liters >= MIN_LITERS && km >= MIN_KM) km / liters else null

    companion object {
        const val MAX_GAP_S = 5.0
        const val MIN_LITERS = 0.05
        const val MIN_KM = 0.3
    }
}
