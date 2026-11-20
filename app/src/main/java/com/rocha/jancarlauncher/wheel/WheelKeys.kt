package com.rocha.jancarlauncher.wheel

/**
 * Teclas do volante como a central as anuncia: a cada botão o serviço `com.jancar.services` manda o broadcast
 * [ACTION] com o número da tecla ([EXTRA_ID]) e o tipo ([EXTRA_TYPE]). Números e tipos conferidos no `IVIKey`
 * descompilado do `ivi-services`; o broadcast é só leitura, a central continua tratando a tecla por conta própria.
 */
object WheelKeys {
    const val ACTION = "com.jancar.services.action.key.event"
    const val EXTRA_ID = "key_event_id"
    const val EXTRA_TYPE = "key_event_type"

    /** Tipos do `IVIKey.Key.Type`. */
    const val TYPE_UP = 0
    const val TYPE_DOWN = 1
    const val TYPE_ONCE = 2

    private val names = mapOf(
        1 to "Volume +", 2 to "Volume −", 6 to "Modo", 7 to "Atender", 8 to "Home", 10 to "Rádio",
        11 to "Play/Pause", 14 to "Navegação", 24 to "Câmera", 25 to "OK", 29 to "Climatização",
        32 to "Próxima", 33 to "Anterior", 34 to "Desligar", 35 to "Mudo + desligar",
        42 to "Rádio: buscar ↑", 43 to "Rádio: buscar ↓", 51 to "Próxima/avançar", 52 to "Anterior/voltar",
        58 to "Cima", 59 to "Baixo", 60 to "Esquerda", 61 to "Direita", 64 to "Avançar", 65 to "Voltar (rebobinar)",
        66 to "Voltar (back)", 72 to "Power", 81 to "Ajustes do carro", 87 to "Pause", 105 to "Mudo",
        106 to "Voz", 159 to "Multifunção", 1001 to "Anterior/atender", 1002 to "Próxima/desligar",
        1005 to "Atender/desligar", 1007 to "Atender ou desligar", 1011 to "Voz Bluetooth", 1015 to "Siri/Google voz",
    )

    /** Nome legível da tecla; número fora da tabela vira "tecla N" (para anotar e acrescentar). */
    fun name(id: Int): String = names[id] ?: "tecla $id"

    fun typeLabel(type: Int): String = when (type) {
        TYPE_UP -> "soltou"
        TYPE_DOWN -> "apertou"
        TYPE_ONCE -> "clique"
        else -> "tipo $type"
    }
}

/** Um aviso de tecla do volante. [atMs] = relógio de parede. */
data class WheelEvent(val id: Int, val type: Int, val atMs: Long) {
    val name: String get() = WheelKeys.name(id)
    val typeLabel: String get() = WheelKeys.typeLabel(type)
}
