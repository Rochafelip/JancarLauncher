package com.rocha.jancarlauncher.overlay

import com.rocha.jancarlauncher.ui.home.HomeLayout

/**
 * Visual do painel sobre o Android Auto (pedido do dono, 2026-11-18: "overlay do Auto em todos os temas"). Os 12 temas
 * da Home se agrupam em 3 desenhos, para o painel combinar com o tema escolhido sem 12 telas desenhadas à mão.
 */
enum class OverlayStyle {
    /** Cartão vertical: velocidade grande, barrinha de giro, consumo, player embaixo (o de sempre). */
    Cartao,
    /** Faixa horizontal baixa: velocidade, giro, consumo, faixa tocando e ⏮ ⏯ ⏭ numa linha só. */
    Barra,
    /** Cartão pequeno: velocidade, giro em número e ⏮ ⏯ ⏭; pouco espaço tirado do Android Auto. */
    Compacto;

    companion object {
        fun of(layout: HomeLayout): OverlayStyle = when (layout) {
            HomeLayout.Atual, HomeLayout.Coluna, HomeLayout.Tocando, HomeLayout.Carro -> Cartao
            HomeLayout.Dock, HomeLayout.Widgets, HomeLayout.Pilulas, HomeLayout.Vitrine -> Barra
            HomeLayout.Central, HomeLayout.Grade, HomeLayout.Trilho, HomeLayout.Oito -> Compacto
        }

        fun fromName(name: String?): OverlayStyle? = entries.firstOrNull { it.name == name }
    }
}
