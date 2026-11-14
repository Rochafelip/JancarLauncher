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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rocha.jancarlauncher.launch.PackageGateway
import com.rocha.jancarlauncher.launch.SettingsActions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rocha.jancarlauncher.media.JancarNowPlaying
import com.rocha.jancarlauncher.media.NowPlayingSource
import com.rocha.jancarlauncher.media.rememberNowPlaying
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.rocha.jancarlauncher.power.ScreenOffPolicy
import com.rocha.jancarlauncher.ui.car3d.CarRoute
import com.rocha.jancarlauncher.ui.dash.DashRoute
import com.rocha.jancarlauncher.ui.screenoff.ScreenOffScreen
import com.rocha.jancarlauncher.settings.NightModeController
import com.rocha.jancarlauncher.settings.SettingsStore
import com.rocha.jancarlauncher.settings.WelcomePolicy
import com.rocha.jancarlauncher.system.rememberAdbStatus
import com.rocha.jancarlauncher.system.rememberWifiStatus
import com.rocha.jancarlauncher.ui.drawer.DrawerScreen
import com.rocha.jancarlauncher.ui.home.BrightnessController
import com.rocha.jancarlauncher.ui.home.HomeScreen
import com.rocha.jancarlauncher.ui.home.rememberClock
import com.rocha.jancarlauncher.ui.nav.Screen
import com.rocha.jancarlauncher.ui.nav.ScreenRestore
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
    // Ao recriar a activity, nunca volta direto no Painel/Gaveta/tela apagada (ver ScreenRestore).
    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen?>(null) }
    var brightnessOpen by rememberSaveable { mutableStateOf(false) }
    var brightnessPercent by remember { mutableIntStateOf(brightness.currentSystemPercent()) }
    val scope = rememberCoroutineScope()
    val clock by rememberClock()
    val nowPlaying = rememberNowPlaying()
    // Música da Jancar (IMedia): conexão única enquanto o launcher está aberto.
    val jancarMedia = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.rocha.jancarlauncher.JancarApp).media
    androidx.compose.runtime.DisposableEffect(jancarMedia) {
        jancarMedia.connect()
        onDispose { jancarMedia.disconnect() }
    }
    // Serviço do carro (ICar): conexão única enquanto o launcher está aberto; o farol comanda o modo noturno.
    val carService = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.rocha.jancarlauncher.JancarApp).carService
    androidx.compose.runtime.DisposableEffect(carService) {
        carService.connect()
        onDispose { carService.disconnect() }
    }
    // OBD-II sempre conectado enquanto o launcher roda: a média da viagem conta tudo até zerar (escolha do dono).
    val obd = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.rocha.jancarlauncher.JancarApp).obd
    androidx.compose.runtime.DisposableEffect(obd) {
        obd.start()
        onDispose { obd.stop() }
    }
    val headlightOn by remember(carService) { carService.state.map { it.headlightOn }.distinctUntilChanged() }
        .collectAsState(initial = null)
    val jancarInfo by jancarMedia.info.collectAsState()
    val jancarArt by jancarMedia.artPng.collectAsState()
    // O serviço manda o estado a cada segundo (posição avançando): só tocando/parado interessa ao card,
    // então zera posição/duração para não recompor a tela inteira a cada segundo na central.
    val jancarPlayFlow = remember(jancarMedia) {
        jancarMedia.playState.map { it?.copy(c = 0, d = 0) }.distinctUntilChanged()
    }
    val jancarPlay by jancarPlayFlow.collectAsState(initial = null)
    val jancarNow = JancarNowPlaying.map(jancarInfo, jancarPlay, jancarArt)
    val sessionNow = nowPlaying.state.value
    val nowSource = JancarNowPlaying.pick(sessionNow, jancarNow)
    val shownNow = if (nowSource == NowPlayingSource.Jancar) jancarNow else sessionNow
    val adbStatus by rememberAdbStatus(tickKey = clock)
    val wifiStatus by rememberWifiStatus(tickKey = clock)

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

    // "Apagar tela": ao entrar em Off a janela vai ao brilho mínimo; ao sair, volta o brilho do usuário
    // (inclusive uma mudança do modo noturno que ficou adiada enquanto a tela estava apagada).
    var wasOff by remember { mutableStateOf(false) }
    LaunchedEffect(screen) {
        val now = screen ?: return@LaunchedEffect
        val isOff = now == Screen.Off
        if (isOff || wasOff) {
            val user = settings.userBrightness.first()
            if (!isOff && user != null) {
                brightnessPercent = user
                brightness.apply(user)
            } else {
                brightness.applyWindow(ScreenOffPolicy.windowBrightness(now, user))
            }
        }
        wasOff = isOff
    }
    // Outro app passou à frente (ex.: câmera de ré) com a tela apagada: ao voltar, Boas-vindas, não a tela preta.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        screen = screen?.let(ScreenOffPolicy::afterLeavingForeground)
    }

    // Modo noturno: farol aceso = noite (sem leitura do farol, vale a janela de horário). Reavaliado a cada
    // tick de minuto e quando o farol muda. A espera de 3 s evita trocar o tema num liga-desliga rápido e dá
    // tempo do ICar conectar na abertura. Toggle manual do usuário não é revertido até a próxima transição.
    LaunchedEffect(clock, headlightOn) {
        kotlinx.coroutines.delay(3_000)
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
                headlightOn = headlightOn,
            )
        ) {
            is NightModeController.Action.ApplyNight -> {
                settings.applyNightMode(action.previousDark, action.previousBrightness)
                if (action.previousBrightness != null) {
                    brightnessPercent = com.rocha.jancarlauncher.settings.NightModePolicy.NIGHT_BRIGHTNESS
                    // Com a tela apagada só grava; o brilho novo é aplicado ao acordar.
                    if (ScreenOffPolicy.shouldApplyBrightness(screen ?: Screen.Home)) brightness.apply(brightnessPercent)
                }
            }
            is NightModeController.Action.RestoreDay -> {
                settings.restoreDayMode(action.dark, action.brightness)
                if (action.brightness != null) {
                    brightnessPercent = action.brightness
                    if (ScreenOffPolicy.shouldApplyBrightness(screen ?: Screen.Home)) brightness.apply(brightnessPercent)
                }
            }
            NightModeController.Action.NoOp -> {}
        }
    }

    BackHandler(enabled = true) {
        when {
            brightnessOpen -> brightnessOpen = false
            screen == Screen.Off -> screen = ScreenOffPolicy.afterWake(Screen.Off)
            screen == Screen.Drawer || screen == Screen.Dash -> screen = Screen.Home
            screen == Screen.Car -> screen = Screen.Dash
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
                nowPlaying = shownNow,
                // Botões vão para a fonte que o card está mostrando.
                onPlay = { if (nowSource == NowPlayingSource.Jancar) jancarMedia.play() else nowPlaying.play() },
                onPause = { if (nowSource == NowPlayingSource.Jancar) jancarMedia.pause() else nowPlaying.pause() },
                onNext = { if (nowSource == NowPlayingSource.Jancar) jancarMedia.next() else nowPlaying.next() },
                onPrevious = { if (nowSource == NowPlayingSource.Jancar) jancarMedia.previous() else nowPlaying.previous() },
                adbStatus = adbStatus,
                onOpenAdbSettings = { actions.openSettings(SettingsActions.DEVELOPER_OPTIONS) },
                wifi = wifiStatus,
                onScreenOff = { brightnessOpen = false; screen = Screen.Off },
                onOpenDash = { brightnessOpen = false; screen = Screen.Dash },
            )
            Screen.Dash -> DashRoute(onBack = { screen = Screen.Home }, onOpenCar = { screen = Screen.Car })
            Screen.Car -> CarRoute(onBack = { screen = Screen.Dash })
            Screen.Off -> ScreenOffScreen(onWake = { screen = ScreenOffPolicy.afterWake(Screen.Off) })
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

private val ScreenSaver = Saver<Screen?, String>(
    save = { ScreenRestore.save(it) },
    restore = { ScreenRestore.restore(it) },
)
