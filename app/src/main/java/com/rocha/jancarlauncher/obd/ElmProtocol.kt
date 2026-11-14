package com.rocha.jancarlauncher.obd

/**
 * Conversa com o adaptador ELM327 (OBD-II) só com comandos padrão: inicialização AT do ELM e PIDs do modo 01 da
 * SAE J1979. Nenhum PID de fabricante. Funções puras (testáveis); o socket fica em [ObdClient].
 */
object ElmProtocol {
    /** Reinicia o ELM, sem eco, sem quebra de linha extra, sem espaços, sem cabeçalho, protocolo automático. */
    val INIT = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")

    // PIDs do modo 01 usados (SAE J1979).
    const val PIDS_01_20 = 0x00
    const val PIDS_21_40 = 0x20
    const val PIDS_41_60 = 0x40
    const val COOLANT = 0x05 // temperatura do líquido de arrefecimento (água do motor), °C
    const val MAP = 0x0B // pressão absoluta do coletor, kPa
    const val RPM = 0x0C
    const val SPEED = 0x0D // km/h
    const val IAT = 0x0F // temperatura do ar admitido, °C
    const val MAF = 0x10 // fluxo de massa de ar, g/s
    const val ETHANOL = 0x52 // % de etanol no combustível

    fun request(pid: Int): String = "01%02X".format(pid)

    /**
     * Bytes de dados da resposta do modo 01 para [pid] ("41 0C 1A F8" → [0x1A, 0xF8]). `null` = sem resposta
     * útil ("NO DATA", "?", "UNABLE TO CONNECT", resposta curta).
     */
    fun parse(raw: String, pid: Int, bytes: Int): IntArray? {
        val hex = raw.uppercase().replace("SEARCHING...", "").filter { it in '0'..'9' || it in 'A'..'F' }
        val tag = "41%02X".format(pid)
        val i = hex.indexOf(tag)
        if (i < 0) return null
        val start = i + tag.length
        if (hex.length < start + 2 * bytes) return null
        return try {
            IntArray(bytes) { k -> hex.substring(start + 2 * k, start + 2 * k + 2).toInt(16) }
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** PIDs suportados a partir da resposta de 0100/0120/0140 (4 bytes, bit mais alto = [base] + 1). */
    fun supported(bitmap: IntArray, base: Int): Set<Int> = buildSet {
        for (i in 0 until 32) {
            val byte = bitmap.getOrNull(i / 8) ?: continue
            if (byte and (0x80 shr (i % 8)) != 0) add(base + i + 1)
        }
    }

    fun rpm(d: IntArray) = (256 * d[0] + d[1]) / 4f
    fun speed(d: IntArray) = d[0].toFloat()
    fun intakeTemp(d: IntArray) = d[0] - 40f
    fun coolantTemp(d: IntArray) = d[0] - 40f
    fun manifoldPressure(d: IntArray) = d[0].toFloat()
    fun mafGramsPerSecond(d: IntArray) = (256 * d[0] + d[1]) / 100f
    fun ethanolPercent(d: IntArray) = d[0] * 100f / 255f
}
