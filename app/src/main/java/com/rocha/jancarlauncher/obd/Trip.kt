package com.rocha.jancarlauncher.obd

/**
 * Média da viagem: litros e km somados desde o último "zerar" (guardados entre ligações do carro).
 * [ethanolLiters] = litros "equivalentes de etanol" ([TripCost.ethanolWeight] de cada intervalo), para o custo em
 * R$ ficar certo mesmo trocando a mistura no meio da viagem.
 */
data class Trip(val liters: Double = 0.0, val km: Double = 0.0, val ethanolLiters: Double = 0.0) {
    /**
     * Soma um intervalo de [dtSeconds] rodando a [speedKmh] e gastando [litersPerHour] com a fração de etanol
     * [ethanol] (0..1). Intervalos estranhos (≤ 0 ou mais de [MAX_GAP_S], ex.: o carro desligou no meio) não entram.
     */
    fun add(litersPerHour: Float, speedKmh: Float, dtSeconds: Double, ethanol: Float = GASOLINE_ETHANOL): Trip {
        if (dtSeconds <= 0.0 || dtSeconds > MAX_GAP_S) return this
        val l = litersPerHour * dtSeconds / 3600.0
        return Trip(liters + l, km + speedKmh * dtSeconds / 3600.0, ethanolLiters + l * TripCost.ethanolWeight(ethanol))
    }

    /** km/L da viagem; `null` enquanto não há o bastante para uma média honesta. */
    val kmPerLiter: Double?
        get() = if (liters >= MIN_LITERS && km >= MIN_KM) km / liters else null

    companion object {
        const val MAX_GAP_S = 5.0
        const val MIN_LITERS = 0.05
        const val MIN_KM = 0.3
        /** Gasolina comum do Brasil (E27). */
        const val GASOLINE_ETHANOL = 0.27f
    }
}
