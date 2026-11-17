package com.rocha.jancarlauncher.obd

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections

class ObdExclusiveTest {
    /** Adaptador falso: registra cada comando e demora um pouco, como o ELM real. */
    private class RecordingLink : ElmLink {
        val sent: MutableList<String> = Collections.synchronizedList(mutableListOf())
        override fun send(cmd: String, timeoutMs: Long): String {
            sent += cmd
            Thread.sleep(2)
            return "NO DATA"
        }
    }

    @Test fun blockRunsBetweenCycles() = runBlocking {
        val link = RecordingLink()
        val gate = ElmGate().apply { this.link = link }
        val cycles = launch(Dispatchers.Default) {
            while (isActive) {
                gate.use { l -> listOf("010D", "010B", "010C").forEach { l.send(it) } }
                delay(1)
            }
        }
        delay(20)
        val answer = withContext(Dispatchers.IO) { gate.use { l -> listOf("0101", "03", "07").map { l.send(it) } } }
        delay(20)
        cycles.cancel()

        assertEquals(3, answer?.size)
        val log = synchronized(link.sent) { link.sent.toList() }
        val i = log.indexOf("0101")
        assertEquals(listOf("0101", "03", "07"), log.subList(i, i + 3))
        // Nenhum ciclo foi cortado ao meio: antes do bloco, o último comando é o fim de um ciclo.
        assertEquals("010C", log[i - 1])
        assertEquals("010D", log[i + 3])
    }

    @Test fun noLink_returnsNull() = runBlocking {
        assertEquals(null, ElmGate().use { it.send("03") })
    }
}
