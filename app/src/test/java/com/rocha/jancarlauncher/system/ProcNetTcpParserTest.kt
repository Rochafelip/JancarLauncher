package com.rocha.jancarlauncher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProcNetTcpParserTest {
    private val header = "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode"

    @Test
    fun `arquivo vazio devolve null`() {
        assertNull(ProcNetTcpParser.findEstablishedRemoteIp(emptyList()))
    }

    @Test
    fun `so o cabecalho devolve null`() {
        assertNull(ProcNetTcpParser.findEstablishedRemoteIp(listOf(header)))
    }

    @Test
    fun `conexao estabelecida na porta 5555 devolve o ip remoto`() {
        // local 0.0.0.0:5555, remoto 192.168.3.10:51000, estado 01 = ESTABLISHED.
        val line = "   1: 00000000:15B3 0A03A8C0:C738 01 00000000:00000000 00:00000000 00000000     0        0 12345 1 0000000000000000 20 4 30 10 -1"
        assertEquals("192.168.3.10", ProcNetTcpParser.findEstablishedRemoteIp(listOf(header, line)))
    }

    @Test
    fun `porta 5555 em LISTEN sem conexao devolve null`() {
        val line = "   1: 00000000:15B3 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0        0 12345 1 0000000000000000 20 4 30 10 -1"
        assertNull(ProcNetTcpParser.findEstablishedRemoteIp(listOf(header, line)))
    }

    @Test
    fun `conexao estabelecida em outra porta e ignorada`() {
        val line = "   1: 00000000:0050 0A03A8C0:C738 01 00000000:00000000 00:00000000 00000000     0        0 12345 1 0000000000000000 20 4 30 10 -1"
        assertNull(ProcNetTcpParser.findEstablishedRemoteIp(listOf(header, line)))
    }

    @Test
    fun `linha malformada e ignorada sem lancar excecao`() {
        val malformed = "lixo qualquer"
        val good = "   1: 00000000:15B3 0A03A8C0:C738 01 00000000:00000000 00:00000000 00000000     0        0 12345 1 0000000000000000 20 4 30 10 -1"
        assertEquals("192.168.3.10", ProcNetTcpParser.findEstablishedRemoteIp(listOf(header, malformed, good)))
    }
}
