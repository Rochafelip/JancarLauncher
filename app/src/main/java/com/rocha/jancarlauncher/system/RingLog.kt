package com.rocha.jancarlauncher.system

import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Registro curto, em memória, dos eventos importantes (OBD/CAN conectou ou caiu, atualização, avisos). Vai junto
 * no relatório de crash e aparece em Ajustes › Relatório de erros ("Registro agora"): o ADB sem fio cai muito, então
 * dá para ver o que aconteceu direto na central.
 */
class RingLog(private val capacity: Int = 200) {
    private val lines = ArrayDeque<String>(capacity)
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun add(tag: String, msg: String, wallMs: Long = System.currentTimeMillis()) {
        val line = "${synchronized(fmt) { fmt.format(Date(wallMs)) }} $tag: $msg"
        synchronized(lines) {
            if (lines.size == capacity) lines.removeFirst()
            lines.addLast(line)
        }
    }

    fun lines(): List<String> = synchronized(lines) { lines.toList() }
}
