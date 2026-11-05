package com.rocha.jancarlauncher

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.launch.AppTarget
import com.rocha.jancarlauncher.launch.AppTargets
import com.rocha.jancarlauncher.launch.Kind
import com.rocha.jancarlauncher.launch.LaunchResult
import com.rocha.jancarlauncher.launch.userMessage
import com.rocha.jancarlauncher.ui.components.AppTile
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.components.MainTile
import com.rocha.jancarlauncher.ui.components.QuickChip
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/** Galeria de depuração (só debug): todos os componentes nos dois temas lado a lado. */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = (application as JancarApp).launchController
        // Teste de lançamento (debug): mostra o resultado em toast, nunca deve derrubar o app.
        val tryLaunch: (AppTarget, () -> LaunchResult) -> Unit = { target, run ->
            val result = run()
            Toast.makeText(this, result.userMessage(target) ?: "${target.displayName}: aberto", Toast.LENGTH_SHORT).show()
        }
        val auto = AppTarget.unconfigured("auto", "Auto", Kind.System)
        val settingsApp = AppTarget("settings", "Ajustes (teste)", Kind.System, "com.android.settings", null)
        val launchTests = listOf<Pair<String, () -> Unit>>(
            "Auto" to { tryLaunch(auto) { controller.openAuto() } },
            "Rádio" to { tryLaunch(AppTargets.RADIO) { controller.open(AppTargets.RADIO) } },
            "Música" to { tryLaunch(AppTargets.MUSIC) { controller.open(AppTargets.MUSIC) } },
            "Vídeo" to { tryLaunch(AppTargets.VIDEO) { controller.open(AppTargets.VIDEO) } },
            "Câmera de ré" to { tryLaunch(AppTargets.BACK_CAMERA) { controller.open(AppTargets.BACK_CAMERA) } },
            "Ajustes (existe)" to { tryLaunch(settingsApp) { controller.open(settingsApp) } },
            // Só debug: testa o reinício automático após crash (Fase 6).
            "Forçar crash" to { throw IllegalStateException("crash de teste (galeria debug)") },
        )
        setContent {
            Row(Modifier.fillMaxSize()) {
                Panel(dark = true, launchTests = launchTests, modifier = Modifier.weight(1f))
                Panel(dark = false, launchTests = emptyList(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Panel(dark: Boolean, launchTests: List<Pair<String, () -> Unit>>, modifier: Modifier) {
    JancarTheme(dark) {
        val c = JTheme.colors
        Column(
            modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(if (dark) "Escuro" else "Claro", style = JancarType.caption.copy(color = c.faint))
            if (launchTests.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    launchTests.take(3).forEach { (label, run) -> QuickChip(JIcons.Auto, label, run) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    launchTests.drop(3).take(3).forEach { (label, run) -> QuickChip(JIcons.Auto, label, run) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    launchTests.drop(6).forEach { (label, run) -> QuickChip(JIcons.Auto, label, run) }
                }
            }
            BasicText("18:37", style = JancarType.clockLarge.copy(color = c.fg))
            BasicText("Quinta-feira, 5 de novembro", style = JancarType.date.copy(color = c.muted))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JIconButton(if (dark) JIcons.Sun else JIcons.Moon, "tema", {})
                JIconButton(JIcons.Sliders, "brilho", {})
                JIconButton(JIcons.Grid, "gaveta", {})
                JIconButton(JIcons.Back, "voltar", {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickChip(JIcons.Wifi, "Wi-Fi", {})
                QuickChip(JIcons.Bluetooth, "Bluetooth", {})
                QuickChip(JIcons.Gps, "GPS", {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickChip(JIcons.Camera, "Câmera de ré", {})
                QuickChip(JIcons.Settings, "Ajustes", {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MainTile(JIcons.Auto, "Auto", {}, Modifier.weight(1f))
                MainTile(JIcons.Music, "Música", {}, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MainTile(JIcons.Radio, "Rádio", {}, Modifier.weight(1f))
                MainTile(JIcons.Video, "Vídeo", {}, Modifier.weight(1f))
            }
            val apps = listOf(
                Triple(JIcons.Vlc, "VLC", "App"), Triple(JIcons.NewPipe, "NewPipe", "App"),
                Triple(JIcons.FDroid, "F-Droid", "App"), Triple(JIcons.Lightning, "Lightning", "App"),
                Triple(JIcons.Gallery, "Galeria", "Sistema"), Triple(JIcons.Folder, "Arquivos", "Sistema"),
                Triple(JIcons.Bluetooth, "Bluetooth", "Sistema"), Triple(JIcons.BtMusic, "Música BT", "Sistema"),
                Triple(JIcons.Eq, "EQ", "Sistema"), Triple(JIcons.Vehicle, "Veículo", "Sistema"),
                Triple(JIcons.Scrcpy, "Scrcpy", "App"), Triple(JIcons.Obd, "OBD-II", "App"),
            )
            apps.chunked(3).forEachIndexed { row, items ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.forEachIndexed { i, (icon, name, tag) ->
                        // O último da galeria aparece desabilitado, para ver o estado "app ausente".
                        val enabled = !(row == 3 && i == 2)
                        AppTile(icon, name, tag, enabled, {}, Modifier.weight(1f))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(bottom = 24.dp))
        }
    }
}
