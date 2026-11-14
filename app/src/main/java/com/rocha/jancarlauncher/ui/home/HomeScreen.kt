package com.rocha.jancarlauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.media.NowPlayingState
import com.rocha.jancarlauncher.system.AdbStatus
import com.rocha.jancarlauncher.system.WifiStatus
import com.rocha.jancarlauncher.ui.UiActions
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.LocalJancarColors

@Composable
fun HomeScreen(
    clock: ClockInfo,
    isDark: Boolean,
    brightnessPercent: Int,
    brightnessOpen: Boolean,
    actions: UiActions,
    onToggleTheme: () -> Unit,
    onToggleBrightness: () -> Unit,
    onCloseBrightness: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
    onOpenDrawer: () -> Unit,
    nowPlaying: NowPlayingState = NowPlayingState.EMPTY,
    onPlay: () -> Unit = {},
    onPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    adbStatus: AdbStatus = AdbStatus.Disabled,
    onOpenAdbSettings: () -> Unit = {},
    wifi: WifiStatus = WifiStatus.Disconnected,
    onScreenOff: () -> Unit = {},
    onOpenDash: () -> Unit = {},
    /** Tema (arranjo) da Home; trocado pela galeria (segurar o botão de tema, ou pelo painel de brilho). */
    layout: HomeLayout = HomeLayout.Atual,
    onLayoutChange: (HomeLayout) -> Unit = {},
) {
    val c = JTheme.colors
    var pickerOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = pickerOpen) { pickerOpen = false }
    val parts = HomeParts(
        clock, wifi, adbStatus, actions, nowPlaying, onPlay, onPause, onNext, onPrevious,
        onToggleTheme = onToggleTheme,
        onOpenThemes = { onCloseBrightness(); pickerOpen = true },
        onToggleBrightness = onToggleBrightness,
        onOpenDrawer = onOpenDrawer,
        onOpenDash = onOpenDash,
        onScreenOff = onScreenOff,
        onOpenAdbSettings = onOpenAdbSettings,
    )
    Box(Modifier.fillMaxSize().background(c.bg)) {
        // Fundo: a foto real do carro, com o véu do tema (HomeWallpaper).
        HomeWallpaper(layout = layout)
        // Sobre a foto, os botões ficam levemente translúcidos (o carro aparece por trás).
        CompositionLocalProvider(
            LocalJancarColors provides c.copy(surface = c.surface.copy(alpha = 0.84f), surfaceHover = c.surfaceHover.copy(alpha = 0.92f)),
        ) {
            HomeArrangement(layout, parts)
        }

        if (brightnessOpen) {
            // Toque fora do painel fecha.
            Box(
                Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCloseBrightness,
                ),
            )
            BrightnessPanel(
                percent = brightnessPercent,
                onChange = onBrightnessChange,
                onChangeFinished = onBrightnessChangeFinished,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 146.dp, end = 64.dp),
                onOpenThemes = parts.onOpenThemes,
                themeLabel = "${layout.letter} · ${layout.label}",
            )
        }

        if (pickerOpen) {
            ThemePicker(
                current = layout,
                ready = READY_LAYOUTS,
                onPick = { onLayoutChange(it); pickerOpen = false },
                onClose = { pickerOpen = false },
            )
        }
    }
}
