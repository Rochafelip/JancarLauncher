package com.rocha.jancarlauncher.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Escala da interface inteira (botões, ícones e fontes juntos), para a Home e as outras telas servirem em qualquer
 * tela de multimídia (pedido do dono, 2026-11-18: botões pequenos numa tela touch de 7"; "adaptar a qualquer tamanho").
 *
 * Duas partes que se multiplicam:
 *  - **automática** ([auto]): o desenho foi medido para uma área de [REF_W]×[REF_H] dp; numa tela maior ou menor (outra
 *    resolução ou densidade) tudo cresce ou encolhe na mesma proporção, para o arranjo continuar cabendo;
 *  - **do usuário** ([Size], Ajustes › Tamanho da interface): Normal / Grande / Extra.
 *
 * Funções puras, testáveis sem Android.
 */
object UiScale {
    /** Área de referência, em dp, para a qual os arranjos foram desenhados (central 1024×600 menos a barra de cima). */
    const val REF_W = 1024f
    const val REF_H = 534f

    /** Limites da escala automática: abaixo disso nada cabe/ler; acima disso o conteúdo fica gigante. */
    const val AUTO_MIN = 0.7f
    const val AUTO_MAX = 1.8f

    /** Tamanho escolhido pelo usuário. [factor] multiplica a escala automática. */
    enum class Size(val label: String, val factor: Float) {
        Normal("Normal", 1.0f),
        Grande("Grande", 1.12f),
        Extra("Extra grande", 1.3f);

        companion object {
            /** Padrão: Grande (o dono achou o tamanho anterior pequeno). */
            val Default = Grande
            fun fromName(name: String?): Size = entries.firstOrNull { it.name == name } ?: Default
        }
    }

    /** Escala automática para uma área de [widthDp]×[heightDp] (dp da densidade original da tela). */
    fun auto(widthDp: Float, heightDp: Float): Float {
        if (widthDp <= 0f || heightDp <= 0f) return 1f
        // Em telas de 1024×600 o ajuste dá 1,0 (altura sobra, largura manda); só mexe quando a tela difere de verdade.
        val fit = minOf(widthDp / REF_W, heightDp / REF_H)
        // Arredonda a 0,05 para a escala não tremer por 1 px de diferença (barra de status que aparece/some).
        val snapped = Math.round(fit * 20f) / 20f
        return snapped.coerceIn(AUTO_MIN, AUTO_MAX)
    }

    /**
     * Escala final. O arranjo do usuário só vale enquanto o conteúdo ainda cabe: a área efetiva (dp / escala) não pode
     * ficar menor que [MIN_FIT_W]×[MIN_FIT_H], o mínimo em que os arranjos da Home ainda cabem com tamanho Extra.
     */
    fun total(widthDp: Float, heightDp: Float, size: Size): Float {
        val wanted = auto(widthDp, heightDp) * size.factor
        if (widthDp <= 0f || heightDp <= 0f) return wanted
        val maxFit = minOf(widthDp / MIN_FIT_W, heightDp / MIN_FIT_H)
        return minOf(wanted, maxOf(maxFit, auto(widthDp, heightDp)))
    }

    /** Menor área efetiva (dp) em que os arranjos ainda cabem. */
    const val MIN_FIT_W = 780f
    const val MIN_FIT_H = 410f
}

/**
 * Aplica a escala a tudo que está dentro: `dp` e `sp` crescem juntos. Mede a área disponível com a densidade original
 * e troca a [LocalDensity] (o toque e o desenho continuam coerentes, é só uma densidade maior).
 */
@Composable
fun UiScaleProvider(size: UiScale.Size, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    BoxWithConstraints(androidx.compose.ui.Modifier.fillMaxSize()) {
        val scale = UiScale.total(maxWidth.value, maxHeight.value, size)
        val scaled = Density(density = base.density * scale, fontScale = base.fontScale)
        CompositionLocalProvider(LocalDensity provides scaled) { content() }
    }
}
