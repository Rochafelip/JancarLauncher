package com.rocha.jancarlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.system.CrashEntry
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Relatório de erros (o ADB sem fio cai toda hora): lista dos crashes gravados no aparelho, o texto completo de cada
 * um e o registro de eventos de agora. Letra de 14 sp para dar para fotografar a tela e mandar.
 */
@Composable
fun CrashReportSection() {
    val app = LocalContext.current.applicationContext as JancarApp
    val c = JTheme.colors
    var version by remember { mutableIntStateOf(0) }
    val crashes = remember(version) { app.crashes.list() }
    var open by remember { mutableStateOf<String?>(null) }
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(3_000); armed = false } }

    SettingsCard("Relatório de erros") {
        if (crashes.isEmpty()) {
            BasicText("Nenhum erro gravado.", style = JancarType.body.copy(color = c.muted))
        } else {
            crashes.forEach { e -> CrashRow(e) { open = app.crashes.read(e) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsButton("Registro agora", { open = app.log.lines().joinToString("\n").ifEmpty { "Registro vazio." } })
            if (crashes.isNotEmpty()) {
                SettingsButton(if (armed) "APAGAR?" else "Apagar tudo", {
                    if (armed) { app.crashes.clear(); armed = false; open = null; version++ } else armed = true
                })
            }
            if (open != null) SettingsButton("Fechar", { open = null })
        }
        open?.let { text ->
            BasicText(
                text,
                style = JancarType.body.copy(color = c.fg, fontSize = 14.sp, fontFamily = FontFamily.Monospace, lineHeight = 19.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusButton))
                    .background(c.bg.copy(alpha = 0.6f))
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp),
            )
        }
    }
}

@Composable
private fun CrashRow(e: CrashEntry, onClick: () -> Unit) {
    val c = JTheme.colors
    val whenText = remember(e.wallMs) { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(e.wallMs)) }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouch)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(c.surfaceHover)
            .clickable(role = Role.Button, onClickLabel = "Ver erro completo", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(whenText, style = JancarType.appTag.copy(color = c.muted))
        BasicText(e.firstLine, style = JancarType.body.copy(color = c.fg, fontSize = 15.sp), maxLines = 1)
    }
}
