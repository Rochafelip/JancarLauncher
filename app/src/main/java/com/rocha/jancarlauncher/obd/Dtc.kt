package com.rocha.jancarlauncher.obd

/** Luz de injeção (MIL) e quantos códigos confirmados a central diz ter (PID 01 do modo 01). */
data class MilStatus(val milOn: Boolean, val count: Int)

/**
 * Códigos de falha (DTC) pelos modos 03 (confirmados), 07 (pendentes) e 0A (permanentes) da SAE J1979. Só leitura:
 * nunca o modo 04 (apagar). O ELM está com `ATS0`/`ATH0` (sem espaços nem cabeçalho). Respostas:
 * - KWP/ISO 9141: `43` + 6 bytes por linha (3 códigos, completados com `0000`), sem contagem;
 * - CAN (ISO 15765): `43` + byte de contagem + 2 bytes por código; com mais de 2 códigos vem em multiquadro
 *   (`00A` = tamanho, depois `0:...`, `1:...`).
 */
object Dtc {
    private val ERRORS = listOf("?", "ERROR", "UNABLE", "STOPPED", "BUFFERFULL", "CANERROR", "BUSBUSY", "FBERROR")

    /** Códigos na ordem em que vieram, sem repetir; lista vazia = sem códigos; `null` = o adaptador não respondeu. */
    fun parse(raw: String, mode: Int): List<String>? {
        val lines = raw.uppercase().split('\r', '\n')
            .map { it.replace(" ", "").replace(">", "").replace("SEARCHING...", "") }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null
        if (lines.any { l -> ERRORS.any { l.contains(it) } }) return null
        if (lines.any { it.contains("NODATA") }) return emptyList()

        val frames = lines.mapNotNull { FRAME.matchEntire(it) }
        val responses: List<Pair<String, Boolean>> = if (frames.isNotEmpty()) {
            listOf(frames.sortedBy { it.groupValues[1].toInt(16) }.joinToString("") { it.groupValues[2] } to true)
        } else {
            lines.map { it to false }
        }

        val tag = "%02X".format(0x40 + mode)
        val out = LinkedHashSet<String>()
        for ((resp, multiFrame) in responses) {
            if (!resp.startsWith(tag)) continue
            val bytes = resp.substring(tag.length).chunked(2).filter { it.length == 2 }.map { it.toIntOrNull(16) ?: return null }
            val pairs = if (multiFrame || bytes.size % 2 == 1) {
                val n = bytes.firstOrNull() ?: 0
                bytes.drop(1).take(2 * n)
            } else {
                bytes
            }
            pairs.chunked(2).filter { it.size == 2 && (it[0] != 0 || it[1] != 0) }.forEach { out += code(it[0], it[1]) }
        }
        return out.toList()
    }

    /** Dois bytes → "P0301": letra pelos 2 bits altos (P, C, B, U), depois 1 dígito de 0–3 e 3 em hex. */
    fun code(a: Int, b: Int): String =
        "PCBU"[a shr 6] + ((a shr 4) and 3).toString() + "%X".format(a and 0xF) + "%02X".format(b)

    /** Resposta do 0101: bit 7 do 1º byte = luz acesa, os outros 7 = quantidade de códigos confirmados. */
    fun milStatus(raw0101: String): MilStatus? =
        ElmProtocol.parse(raw0101, 0x01, 4)?.let { MilStatus(it[0] and 0x80 != 0, it[0] and 0x7F) }

    private val FRAME = Regex("^([0-9A-F]):([0-9A-F]+)$")
}
