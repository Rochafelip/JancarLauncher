package com.rocha.jancarlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import com.rocha.jancarlauncher.ui.theme.UiScale
import kotlinx.coroutines.launch

/** Ajustes › Tamanho da interface: botões, ícones e fontes juntos (Normal / Grande / Extra grande). */
@Composable
fun UiSizeSection() {
    val app = LocalContext.current.applicationContext as JancarApp
    val c = JTheme.colors
    val name by app.settings.uiSize.collectAsState(initial = null)
    val current = UiScale.Size.fromName(name)
    val scope = rememberCoroutineScope()
    SettingsCard("Tamanho da interface") {
        BasicText(
            "Aumenta ou diminui botões, ícones e letras de todas as telas. A tela já se ajusta sozinha ao tamanho da central.",
            style = JancarType.body.copy(color = c.muted),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UiScale.Size.entries.forEach { size ->
                SettingsButton(
                    size.label, accent = size == current,
                    onClick = { scope.launch { app.settings.setUiSize(size.name) } },
                )
            }
        }
    }
}
