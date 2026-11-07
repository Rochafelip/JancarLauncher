package com.rocha.jancarlauncher.can

/** Valor que o `CarInfo` do servico devolve antes da MCU mandar a primeira leitura. */
private const val OUTSIDE_TEMP_UNKNOWN = 255

/**
 * Converte o valor cru de `ICar.getOutsideTempRawValue()` para graus Celsius.
 *
 * Mesma regra do SDK da Jancar (`IVICar.OutsideTemp.getTemp`, lido do ivi-services descompilado):
 * byte alto = temperatura + 100 (em °C) ou + 60 (em °F, quando o bit 0 do byte baixo e 1);
 * bits 6-7 do byte baixo == 2 somam 0,5 grau, para longe do zero.
 */
fun decodeOutsideTempC(raw: Int?): Float? {
    if (raw == null || raw == OUTSIDE_TEMP_UNKNOWN) return null
    val high = (raw shr 8) and 0xFF
    val low = raw and 0xFF
    val halfDegree = (low shr 6) and 0x3 == 2
    fun withHalf(t: Float) = if (halfDegree) t + (if (t < 0f) -0.5f else 0.5f) else t
    return if (low and 1 == 0) {
        withHalf(high - 100f)
    } else {
        (withHalf(high - 60f) - 32f) / 1.8f
    }
}
