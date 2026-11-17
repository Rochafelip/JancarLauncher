package com.rocha.jancarlauncher.obd

import java.util.Locale

/**
 * Custo da viagem em R$. O preço do litro é linear na fração de etanol (gasolina E27 → etanol hidratado), então basta
 * guardar os litros e os "litros de etanol equivalentes" ([Trip.ethanolLiters]): o custo sai certo com troca de
 * mistura no meio e com o preço mudado depois.
 */
object TripCost {
    const val MIN_CENTS = 100
    const val MAX_CENTS = 2000

    /** 0 = gasolina comum (E27), 1 = etanol; frações abaixo de E27 contam como gasolina. */
    fun ethanolWeight(f: Float): Double = ((f - Trip.GASOLINE_ETHANOL) / (1f - Trip.GASOLINE_ETHANOL)).toDouble().coerceIn(0.0, 1.0)

    fun reais(liters: Double, ethanolLiters: Double, gasCents: Int?, ethCents: Int?): Double? {
        if (gasCents == null || ethCents == null) return null
        val pg = gasCents / 100.0
        val pe = ethCents / 100.0
        return pg * liters + (pe - pg) * ethanolLiters
    }

    fun text(reais: Double?): String =
        reais?.let { "R$ " + String.format(Locale("pt", "BR"), "%.2f", it) } ?: "R$ — definir preço"

    /** Botões −/+ do preço: fica entre R$ 1,00 e R$ 20,00. */
    fun step(cents: Int, delta: Int): Int = (cents + delta).coerceIn(MIN_CENTS, MAX_CENTS)
}
