package com.rocha.jancarlauncher.system

/** Parser de `/proc/net/tcp` isolado das chamadas de I/O — recebe linhas já lidas, testável em JVM puro. */
object ProcNetTcpParser {
    private const val ESTABLISHED = "01"

    /** Porta 5555 em hex (formato do arquivo), maiúscula. */
    private const val DEFAULT_PORT_HEX = "15B3"

    /**
     * Procura uma conexão ESTABLISHED com porta local [portHex] e devolve o IP remoto (dotted-decimal),
     * ou null se não encontrar nenhuma conexão estabelecida nessa porta (porta fechada, ou arquivo
     * ilegível/vazio/malformado — nunca lança exceção, cada linha ruim é só ignorada).
     */
    fun findEstablishedRemoteIp(lines: List<String>, portHex: String = DEFAULT_PORT_HEX): String? {
        for (line in lines.drop(1)) { // primeira linha é o cabeçalho
            val cols = line.trim().split(Regex("\\s+"))
            if (cols.size < 4) continue
            val local = cols[1]
            val remote = cols[2]
            val state = cols[3]

            val localPort = local.substringAfter(':', missingDelimiterValue = "")
            if (!localPort.equals(portHex, ignoreCase = true)) continue
            if (!state.equals(ESTABLISHED, ignoreCase = true)) continue

            return decodeIp(remote.substringBefore(':'))
        }
        return null
    }

    /** "0100007F" (little-endian hex) -> "127.0.0.1". Entrada malformada devolve null. */
    private fun decodeIp(hex: String): String? {
        if (hex.length != 8) return null
        return try {
            val bytes = (0 until 4).map { hex.substring(it * 2, it * 2 + 2).toInt(16) }
            "${bytes[3]}.${bytes[2]}.${bytes[1]}.${bytes[0]}"
        } catch (e: NumberFormatException) {
            null
        }
    }
}
