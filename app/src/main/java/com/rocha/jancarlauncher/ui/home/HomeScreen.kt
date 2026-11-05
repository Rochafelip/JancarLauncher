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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.launch.AppTargets
import com.rocha.jancarlauncher.launch.SettingsActions
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
                    BasicText("MULTIMÍDIA", style = JancarType.caption.copy(color = c.faint), modifier = Modifier.padding(top = 10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        JIconButton(if (isDark) JIcons.Sun else JIcons.Moon, "Alternar tema claro/escuro", onToggleTheme)
                        JIconButton(JIcons.Sliders, "Ajustar brilho", onToggleBrightness)
                        JIconButton(JIcons.Grid, "Ver todos os apps", onOpenDrawer)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(Modifier.padding(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickChip(JIcons.Wifi, "Wi-Fi", { actions.openSettings(SettingsActions.WIFI) })
                QuickChip(JIcons.Bluetooth, "Bluetooth", { actions.openSettings(SettingsActions.BLUETOOTH) })
                QuickChip(JIcons.Gps, "GPS", { actions.openSettings(SettingsActions.LOCATION) })
                QuickChip(JIcons.Camera, "Câmera de ré", { actions.open(AppTargets.BACK_CAMERA) })
                QuickChip(JIcons.Settings, "Ajustes", { actions.openSettings(SettingsActions.MAIN) })
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MainTile(JIcons.Auto, "Auto", { actions.openAuto() }, Modifier.weight(1f))
                MainTile(JIcons.Music, "Música", { actions.open(AppTargets.MUSIC) }, Modifier.weight(1f))
                MainTile(JIcons.Radio, "Rádio", { actions.open(AppTargets.RADIO) }, Modifier.weight(1f))
                MainTile(JIcons.Video, "Vídeo", { actions.open(AppTargets.VIDEO) }, Modifier.weight(1f))
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
