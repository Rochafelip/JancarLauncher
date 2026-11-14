package com.rocha.jancarlauncher.ui.home

/**
 * Temas da Home (pedido do dono, 2026-11-14: "ficar com esses 6 e mais 6, usar como tema; clica num botão e segura e
 * lá dá para escolher"). Cada tema é um **arranjo** da Home sobre a foto do carro, escolhido no Claude Design
 * (https://claude.ai/artifact/EoZN91ks7PscfjrNdFCiZA). As outras telas seguem só o estilo; a tela do Fox fica fiel ao
 * carro. Escolha salva em `SettingsStore.homeLayout`.
 *
 * [preview] = retângulos (fração 0..1 de x, y, largura, altura) da miniatura da galeria; `dark` = faixa escura
 * (coluna, trilho) em vez de bloco claro.
 */
enum class HomeLayout(val letter: String, val label: String, val preview: List<Block>) {
    Atual("A", "Atual", listOf(b(.06, .08, .30, .14), b(.60, .08, .34, .10), b(.06, .52, .50, .08), b(.06, .66, .88, .24))),
    Dock("B", "Dock", listOf(b(.06, .08, .26, .12), b(.64, .08, .30, .09), b(.04, .80, .92, .15))),
    Coluna("C", "Coluna", listOf(b(0.0, 0.0, .40, 1.0, dark = true), b(.06, .08, .28, .12), b(.06, .30, .28, .40), b(.62, .06, .32, .09))),
    Tocando("D", "Tocando", listOf(b(.06, .08, .26, .12), b(.06, .34, .56, .20), b(.06, .72, .88, .18))),
    Central("E", "Central", listOf(b(.35, .06, .30, .16), b(.18, .78, .64, .14))),
    Carro("F", "Carro", listOf(b(.06, .08, .28, .13), b(.58, .20, .36, .22), b(.06, .72, .88, .18))),
    Grade("G", "Grade", listOf(b(.04, .74, .34, .16), b(.60, .18, .36, .64))),
    Trilho("H", "Trilho", listOf(b(0.0, 0.0, .11, 1.0, dark = true), b(.02, .06, .07, .60), b(.70, .06, .26, .14))),
    Widgets("I", "Widgets", listOf(b(.04, .14, .92, .22), b(.04, .74, .92, .18))),
    Oito("J", "Oito", listOf(b(.06, .08, .28, .13), b(.06, .78, .88, .14))),
    Pilulas("K", "Pílulas", listOf(b(.04, .08, .30, .14), b(.66, .16, .30, .64))),
    Vitrine("L", "Vitrine", listOf(b(.04, .06, .24, .09), b(.26, .82, .48, .12)));

    data class Block(val x: Float, val y: Float, val w: Float, val h: Float, val dark: Boolean = false)

    companion object {
        /** Tema salvo; ausente ou desconhecido = o de sempre (A). */
        fun fromName(name: String?): HomeLayout = entries.firstOrNull { it.name == name } ?: Atual
    }
}

private fun b(x: Double, y: Double, w: Double, h: Double, dark: Boolean = false) =
    HomeLayout.Block(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), dark)
