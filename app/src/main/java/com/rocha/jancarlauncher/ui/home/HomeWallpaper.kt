package com.rocha.jancarlauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.rocha.jancarlauncher.R
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.LightColors

/**
 * Fundo da Home (pedido do dono, 2026-11-14: "em vez de preto, o papel de parede ... centralizando o carro" e depois
 * "a foto real sem esse azul com blur"): a **foto real** do Fox, sem tratamento, com o carro no meio e a placa em
 * branco (`home_wallpaper`, gerada por `tools/car-photo/papel-de-parede.ps1`). Centralizada: na central a barra da
 * Jancar come o topo e o corte fica igual em cima e embaixo.
 *
 * Por cima, um véu na cor de fundo do tema só para o relógio e os botões continuarem legíveis (a foto tem a parede
 * branca atrás): no escuro, escurece mais em cima (relógio) e embaixo (blocos); no claro, véu claro.
 */
@Composable
fun HomeWallpaper(modifier: Modifier = Modifier, layout: HomeLayout = HomeLayout.Atual) {
    val c = JTheme.colors
    val light = c == LightColors
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.home_wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
        // Véu do tema (cada arranjo escurece onde tem texto); no claro, sempre um pouco mais forte (texto escuro).
        val stops = layout.veil().map { (pos, a) -> pos to c.bg.copy(alpha = if (light) (0.40f + a * 0.55f) else a) }
        val veil = Brush.verticalGradient(*stops.toTypedArray())
        Box(Modifier.fillMaxSize().background(veil))
    }
}
