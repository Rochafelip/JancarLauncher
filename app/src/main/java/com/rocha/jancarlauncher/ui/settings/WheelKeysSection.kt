package com.rocha.jancarlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ajustes › Teclas do volante: mostra cada botão apertado no volante (número, nome e tipo), para descobrir quais teclas
 * a central manda e de que lado do volante cada uma está. Só leitura; as teclas continuam funcionando como sempre.
 */
@Composable
fun WheelKeysSection() {
    val app = LocalContext.current.applicationContext as JancarApp
    val c = JTheme.colors
    val events by app.wheelKeys.events.collectAsState()
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    SettingsCard("Teclas do volante") {
        BasicText(
            "Aperte os botões do volante, um de cada vez (primeiro um lado, depois o outro), e anote o que aparece aqui.",
            style = JancarType.body.copy(color = c.muted),
        )
        if (events.isEmpty()) {
            BasicText("Nenhuma tecla ainda.", style = JancarType.body.copy(color = c.faint))
        } else {
            events.take(12).forEach { e ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BasicText(fmt.format(Date(e.atMs)), style = JancarType.body.copy(color = c.faint))
                    BasicText("${e.id}", style = JancarType.body.copy(color = c.accent))
                    BasicText(e.name, style = JancarType.body.copy(color = c.fg))
                    BasicText(e.typeLabel, style = JancarType.body.copy(color = c.muted))
                }
            }
        }
        SettingsButton("Limpar", onClick = { app.wheelKeys.clear() }, enabled = events.isNotEmpty())
    }
}
