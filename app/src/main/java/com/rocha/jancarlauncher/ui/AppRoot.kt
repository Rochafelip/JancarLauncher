package com.rocha.jancarlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rocha.jancarlauncher.launch.PackageGateway
import com.rocha.jancarlauncher.launch.SettingsActions
import com.rocha.jancarlauncher.media.rememberNowPlaying
import com.rocha.jancarlauncher.settings.NightModeController
import com.rocha.jancarlauncher.settings.SettingsStore
import com.rocha.jancarlauncher.settings.WelcomePolicy
import com.rocha.jancarlauncher.system.rememberAdbStatus
import com.rocha.jancarlauncher.ui.drawer.DrawerScreen
import com.rocha.jancarlauncher.ui.home.BrightnessController
import com.rocha.jancarlauncher.ui.home.HomeScreen
import com.rocha.jancarlauncher.ui.home.rememberClock
import com.rocha.jancarlauncher.ui.nav.Screen
import com.rocha.jancarlauncher.ui.theme.JancarTheme
import com.rocha.jancarlauncher.ui.welcome.WelcomeScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppRoot(
    settings: SettingsStore,
    actions: UiActions,
    gateway: PackageGateway,
    brightness: BrightnessController,
    events: Flow<Screen>,
    bootCount: Int,
    forceWelcome: Boolean,
) {
    val dark by settings.dark.collectAsState(initial = null)
    var screen by rememberSaveable { mutableStateOf<Screen?>(null) }
    var brightnessOpen by rememberSaveable { mutableStateOf(false) }
    var brightnessPercent by remember { mutableIntStateOf(brightness.currentSystemPercent()) }
    val scope = rememberCoroutineScope()
    val clock by rememberClock()
    val nowPlaying = rememberNowPlaying()
    val adbStatus by rememberAdbStatus(tickKey = clock)

    // Tela inicial: Boas-vindas na 1ª abertura após reinício (ou quando a ativação por "acordar" pediu).
    LaunchedEffect(Unit) {
        if (screen == null) {
            val saved = settings.lastBootCount.first()
            screen = if (forceWelcome || WelcomePolicy.shouldShowOnLaunch(bootCount, saved)) Screen.Welcome else Screen.Home
        }
    }
    // Brilho: só reaplica o que o usuário escolheu; sem escolha, não mexe no brilho da central.
    LaunchedEffect(Unit) {
        settings.userBrightness.first()?.let {
            brightnessPercent = it
            brightness.apply(it)
        }
    }
    LaunchedEffect(events) { events.collect { screen = it } }

    // Modo noturno: reavaliado a cada tick de minuto do relógio (e uma vez na abertura, já cobrindo
    // o caso de abrir dentro da janela). Toggle manual do usuário não é revertido até a próxima transição.
    LaunchedEffect(clock) {
        val now = java.time.LocalTime.now()
        val wasApplied = settings.nightModeApplied.first()
        val currentDark = settings.dark.first()
        val currentBrightness = settings.userBrightness.first()
        val previousDark = settings.preNightDark.first()
        val previousBrightness = settings.preNightBrightness.first()
        when (
            val action = NightModeController.evaluate(
                now = now,
                wasApplied = wasApplied,
                currentDark = currentDark,
                currentBrightness = currentBrightness,
                previousDark = previousDark,
                previousBrightness = previousBrightness,
            )
        ) {
            is NightModeController.Action.ApplyNight -> {
                settings.applyNightMode(action.previousDark, action.previousBrightness)
                if (action.previousBrightness != null) {
                    brightnessPercent = com.rocha.jancarlauncher.settings.NightModePolicy.NIGHT_BRIGHTNESS
                    brightness.apply(brightnessPercent)
                }
            }
            is NightModeController.Action.RestoreDay -> {
                settings.restoreDayMode(action.dark, action.brightness)
                if (action.brightness != null) {
                    brightnessPercent = action.brightness
                    brightness.apply(brightnessPercent)
                }
            }
            NightModeController.Action.NoOp -> {}
        }
    }

    BackHandler(enabled = true) {
        when {
            brightnessOpen -> brightnessOpen = false
            screen == Screen.Drawer -> screen = Screen.Home
            // Home/Boas-vindas: a tela inicial não "sai" com o botão voltar.
        }
    }

    val isDark = dark
    val current = screen
    if (isDark == null || current == null) {
        // Até carregar tema e tela inicial, só o fundo escuro (igual ao windowBackground).
        Box(Modifier.fillMaxSize().background(Color(0xFF0A0B0F)))
        return
    }

    JancarTheme(isDark) {
        val toggleTheme: () -> Unit = { scope.launch { settings.setDark(!isDark) } }
        AnimatedContent(
            targetState = current,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "screen",
        ) { targetScreen ->
            when (targetScreen) {
            Screen.Welcome -> WelcomeScreen(clock) {
                screen = Screen.Home
                scope.launch { settings.setLastBootCount(bootCount) }
            }
            Screen.Home -> HomeScreen(
                clock = clock,
                isDark = isDark,
                brightnessPercent = brightnessPercent,
                brightnessOpen = brightnessOpen,
                actions = actions,
                onToggleTheme = toggleTheme,
                onToggleBrightness = { brightnessOpen = !brightnessOpen },
                onCloseBrightness = { brightnessOpen = false },
                onBrightnessChange = { brightnessPercent = it; brightness.apply(it) },
                onBrightnessChangeFinished = { scope.launch { settings.setUserBrightness(brightnessPercent) } },
                onOpenDrawer = { brightnessOpen = false; screen = Screen.Drawer },
                nowPlaying = nowPlaying.state.value,
                onPlay = nowPlaying::play,
                onPause = nowPlaying::pause,
                onNext = nowPlaying::next,
                onPrevious = nowPlaying::previous,
                adbStatus = adbStatus,
                onOpenAdbSettings = { actions.openSettings(SettingsActions.DEVELOPER_OPTIONS) },
            )
            Screen.Drawer -> DrawerScreen(
                isDark = isDark,
                gateway = gateway,
                actions = actions,
                onToggleTheme = toggleTheme,
                onBack = { screen = Screen.Home },
            )
            }
        }
    }
}
