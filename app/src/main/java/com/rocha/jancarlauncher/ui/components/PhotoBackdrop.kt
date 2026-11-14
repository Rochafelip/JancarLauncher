package com.rocha.jancarlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.rocha.jancarlauncher.R
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.LightColors
import com.rocha.jancarlauncher.ui.theme.LocalJancarColors

/**
 * Estilo dos temas nas outras telas (Painel, gaveta; pedido do dono, 2026-11-14: "outras telas acompanham o tema, só
 * o estilo"): a foto real do Fox ao fundo, bem escurecida ([dim] = opacidade do véu na cor de fundo), para os
 * números continuarem legíveis. No tema claro o véu é claro e um pouco mais forte (o texto é escuro).
 */
@Composable
fun PhotoBackdrop(dim: Float, modifier: Modifier = Modifier) {
    val c = JTheme.colors
    val light = c == LightColors
    Box(modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.home_wallpaper), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = if (light) (0.40f + dim * 0.55f).coerceAtMost(0.95f) else dim)))
    }
}

/** Botões e cartões levemente translúcidos sobre a foto (o carro aparece por trás), como na Home. */
@Composable
fun OnPhotoSurfaces(content: @Composable () -> Unit) {
    val c = JTheme.colors
    CompositionLocalProvider(
        LocalJancarColors provides c.copy(surface = c.surface.copy(alpha = 0.84f), surfaceHover = c.surfaceHover.copy(alpha = 0.92f)),
        content = content,
    )
}
