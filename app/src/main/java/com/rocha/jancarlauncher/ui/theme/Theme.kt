package com.rocha.jancarlauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

object JTheme {
    val colors: JancarColors
        @Composable get() = LocalJancarColors.current
}

object Dimens {
    /** Toque mínimo (spec: ≥ 44 dp; usamos 48). */
    val MinTouch = 48.dp
    /** Altura dos chips de atalho da Home: alvo de toque para uso dirigindo (56–64 dp). */
    val ChipHeight = 60.dp
    val RadiusButton = 14.dp
    val RadiusTile = 22.dp
    val RadiusAppTile = 18.dp
    val RadiusCard = 20.dp
    val RadiusPanel = 16.dp
}

@Composable
fun JancarTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalJancarColors provides if (dark) DarkColors else LightColors,
        content = content,
    )
}
