package com.rocha.jancarlauncher.obd

/**
 * Consumo ESTIMADO a partir do ar que o motor admite (o OBD-II não manda o consumo pronto).
 * Combustível (g/s) = ar (g/s) ÷ relação estequiométrica; litros = gramas ÷ densidade. Os números do combustível
 * são valores típicos (aproximação): o Fox 2014 é flex e a mistura no tanque muda a conta.
 */
object FuelModel {
    /** Motor do Fox do dono: 1.0 8V Total Flex, 999 cm³ (informado por ele, 2026-11-13). */
    const val DISPLACEMENT_L = 0.999f

    /** Eficiência volumétrica típica de um 1.0 8V aspirado (só entra na conta pela pressão do coletor). */
    const val VOLUMETRIC_EFFICIENCY = 0.80f

    /** Mistura no tanque quando o carro não informa o % de etanol (PID 0x52): o dono escolhe no Painel. */
    enum class Blend(val label: String, val ethanol: Float) {
        Gasolina("GASOLINA", 0.27f), // gasolina comum do Brasil (E27)
        Mistura("MISTURA", 0.63f), // ~meio tanque de cada
        Etanol("ETANOL", 1f);

        fun next(): Blend = entries[(ordinal + 1) % entries.size]

        companion object {
            fun fromName(name: String?): Blend = entries.firstOrNull { it.name == name } ?: Gasolina
        }
    }

    // Relação ar/combustível estequiométrica e densidade (kg/L): gasolina pura e etanol hidratado.
    private const val AFR_GASOLINE = 14.7f
    private const val AFR_ETHANOL = 9.0f
    private const val DENSITY_GASOLINE = 0.745f
    private const val DENSITY_ETHANOL = 0.809f

    fun stoichAfr(ethanol: Float): Float = AFR_GASOLINE + (AFR_ETHANOL - AFR_GASOLINE) * ethanol.coerceIn(0f, 1f)
    fun densityKgPerL(ethanol: Float): Float = DENSITY_GASOLINE + (DENSITY_ETHANOL - DENSITY_GASOLINE) * ethanol.coerceIn(0f, 1f)

    /** Litros por hora a partir do ar (g/s) e da fração de etanol (0..1). */
    fun litersPerHour(airGramsPerSecond: Float, ethanol: Float): Float {
        val fuelGs = airGramsPerSecond / stoichAfr(ethanol)
        return fuelGs * 3600f / (densityKgPerL(ethanol) * 1000f)
    }

    /**
     * Ar (g/s) pela pressão do coletor quando o carro não tem MAF ("speed-density"):
     * massa = P·V·η / (R·T) por ciclo, um ciclo a cada 2 voltas.
     */
    fun airFromManifold(mapKpa: Float, intakeC: Float, rpm: Float): Float {
        val pa = mapKpa * 1000f
        val volumeM3 = DISPLACEMENT_L / 1000f
        val kelvin = intakeC + 273.15f
        val kgPerCycle = pa * volumeM3 * VOLUMETRIC_EFFICIENCY / (287.05f * kelvin)
        return kgPerCycle * (rpm / 120f) * 1000f
    }

    /** km/L agora; `null` parado ou quase (aí mostra L/h). */
    fun kmPerLiter(speedKmh: Float, litersPerHour: Float): Float? =
        if (speedKmh < 3f || litersPerHour < 0.05f) null else speedKmh / litersPerHour
}
