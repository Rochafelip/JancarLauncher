package com.rocha.jancarlauncher.ui.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import kotlinx.coroutines.launch

/** Ajustes › Retomar a música ao ligar (desligado por padrão, até o teste no carro). */
@Composable
fun MusicResumeSection() {
    val app = LocalContext.current.applicationContext as JancarApp
    val c = JTheme.colors
    val on by app.settings.resumeMusic.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    SettingsCard("Retomar a música ao ligar") {
        BasicText(
            "Ao ligar a central, volta a tocar o que estava tocando (Música da central ou o último app). " +
                "Com o Android Auto ou o CarPlay na frente, quem decide é o celular.",
            style = JancarType.body.copy(color = c.muted),
        )
        SettingsButton(if (on) "Ligado · toque para desligar" else "Desligado · toque para ligar", accent = on, onClick = {
            scope.launch { app.settings.setResumeMusic(!on) }
        })
    }
}
