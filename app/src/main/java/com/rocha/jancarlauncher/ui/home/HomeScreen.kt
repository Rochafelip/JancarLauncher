package com.rocha.jancarlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.can.VehicleInfoCard
import com.rocha.jancarlauncher.launch.AppTargets
import com.rocha.jancarlauncher.launch.SettingsActions
import com.rocha.jancarlauncher.media.NowPlayingState
import com.rocha.jancarlauncher.system.AdbStatus
import com.rocha.jancarlauncher.ui.UiActions
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.components.MainTile
import com.rocha.jancarlauncher.ui.components.QuickChip
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

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
) {
    val c = JTheme.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp, top = 52.dp, bottom = 48.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText(clock.time, style = JancarType.clockLarge.copy(color = c.fg))
                    BasicText(clock.date, style = JancarType.date.copy(color = c.muted))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (adbStatus != AdbStatus.Disabled) {
                            Row(
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onOpenAdbSettings,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                com.rocha.jancarlauncher.ui.components.JIcon(com.rocha.jancarlauncher.ui.icons.JIcons.Usb, c.muted, 13.dp)
                                BasicText(
                                    if (adbStatus is AdbStatus.EnabledConnected) "ADB conectado" else "ADB ativo",
                                    style = JancarType.appTag.copy(color = c.muted),
                                )
                            }
                        }
                        BasicText("MULTIMÍDIA", style = JancarType.caption.copy(color = c.faint))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        JIconButton(if (isDark) JIcons.Sun else JIcons.Moon, "Alternar tema claro/escuro", onToggleTheme)
                        JIconButton(JIcons.Sliders, "Ajustar brilho", onToggleBrightness)
                        JIconButton(JIcons.Grid, "Ver todos os apps", onOpenDrawer)
                    }
                }
            }

            NowPlayingCard(
                nowPlaying = nowPlaying,
                onPlay = onPlay,
                onPause = onPause,
                onNext = onNext,
                onPrevious = onPrevious,
                modifier = Modifier.padding(top = 24.dp),
            )

            // TEMP (calibração do CAN bus): valores crus pra conferir contra o carro real.
            // Remover (ou esconder atrás de um gesto de debug) depois de validar can/CanBusClient.kt.
            VehicleInfoCard()

            Spacer(Modifier.weight(1f))

            Row(Modifier.padding(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickChip(JIcons.Wifi, "Wi-Fi", { actions.openSettings(SettingsActions.WIFI) })
                QuickChip(JIcons.Bluetooth, "Bluetooth", { actions.openSettings(SettingsActions.BLUETOOTH) })
                QuickChip(JIcons.Gps, "GPS", { actions.openSettings(SettingsActions.LOCATION) })
                QuickChip(JIcons.Camera, "Câmera de ré", { actions.open(AppTargets.BACK_CAMERA) })
                QuickChip(JIcons.Settings, "Ajustes", { actions.openSettings(SettingsActions.MAIN) })
            }

            var openingTile by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(openingTile) {
                if (openingTile != null) {
                    kotlinx.coroutines.delay(800)
                    openingTile = null
                }
            }
            fun openTile(key: String, launch: () -> Unit) {
                openingTile = key
                launch()
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MainTile(
                    JIcons.Auto, "Auto", { openTile("auto") { actions.openAuto() } },
                    Modifier.weight(1f).alpha(if (openingTile == "auto") 0.6f else 1f),
                    onLongClick = { /* TODO(issue): atalho por bloco */ },
                )
                MainTile(
                    JIcons.Music, "Música", { openTile("music") { actions.open(AppTargets.MUSIC) } },
                    Modifier.weight(1f).alpha(if (openingTile == "music") 0.6f else 1f),
                    onLongClick = { /* TODO(issue): atalho por bloco */ },
                )
                MainTile(
                    JIcons.Radio, "Rádio", { openTile("radio") { actions.open(AppTargets.RADIO) } },
                    Modifier.weight(1f).alpha(if (openingTile == "radio") 0.6f else 1f),
                    onLongClick = { /* TODO(issue): atalho por bloco */ },
                )
                MainTile(
                    JIcons.Video, "Vídeo", { openTile("video") { actions.open(AppTargets.VIDEO) } },
                    Modifier.weight(1f).alpha(if (openingTile == "video") 0.6f else 1f),
                    onLongClick = { /* TODO(issue): atalho por bloco */ },
                )
            }
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
            )
        }
    }
}
