package com.rocha.jancarlauncher.ui.obd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.obd.DtcCatalog
import com.rocha.jancarlauncher.obd.ScanResult
import com.rocha.jancarlauncher.obd.Severity
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SevHigh = Color(0xFFEF4444)
private val SevMid = Color(0xFFF2A93B)

/** Aba Falhas: botão de leitura (só parado), resultado com explicação de cada código e as últimas leituras. */
@Composable
fun FaultsTab(
    canScan: Boolean,
    scanning: Boolean,
    result: ScanResult?,
    history: List<ScanResult>,
    catalog: DtcCatalog,
    onScan: () -> Unit,
) {
    val c = JTheme.colors
    var open by remember { mutableStateOf<String?>(null) }

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText("SCANNER DE FALHAS", style = JancarType.caption.copy(color = c.muted))
                BasicText(
                    when {
                        scanning -> "Lendo a central do motor…"
                        !canScan -> "Disponível com o carro parado e o OBD conectado"
                        else -> "Deixe a ignição ligada; o motor pode estar ligado ou desligado."
                    },
                    style = JancarType.body.copy(color = c.fg),
                )
            }
            ScanButton(enabled = canScan && !scanning, onScan)
        }
    }

    result?.let { r ->
        Card {
            r.mil?.let { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(if (m.milOn) SevMid else c.faint))
                    Spacer(Modifier.width(10.dp))
                    BasicText(
                        if (m.milOn) "Luz de injeção ACESA" else "Luz de injeção apagada",
                        style = JancarType.panelTitle.copy(color = c.fg),
                    )
                }
            }
            r.error?.let { BasicText(it, style = JancarType.body.copy(color = SevHigh)) }
            val all = r.confirmed.map { it to "CONFIRMADO" } + r.pending.map { it to "PENDENTE" } +
                r.permanent.orEmpty().map { it to "PERMANENTE" }
            if (all.isEmpty() && r.error == null) {
                BasicText("NENHUM CÓDIGO ENCONTRADO", style = JancarType.panelTitle.copy(color = c.fg, letterSpacing = 1.sp))
            }
            all.forEach { (code, kind) ->
                val info = catalog.describe(code)
                CodeRow(code, kind, info.title, info.severity) { open = if (open == code) null else code }
                if (open == code) {
                    Column(Modifier.padding(start = 16.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (kind == "PENDENTE") {
                            Small("Pendente: a central notou a condição, mas ainda não confirmou. Pode sumir sozinho se não se repetir.")
                        }
                        Small("Possíveis causas", strong = true)
                        info.causes.forEach { Small("• $it") }
                        Small("O que conferir", strong = true)
                        info.checks.forEach { Small("• $it") }
                    }
                }
            }
            Small("Um código indica a condição que a central detectou; não confirma que uma peça está com defeito.")
        }
    }

    if (history.isNotEmpty()) {
        Card {
            BasicText("ÚLTIMAS LEITURAS", style = JancarType.caption.copy(color = c.muted))
            val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
            history.forEach { h ->
                val codes = (h.confirmed + h.pending).distinct()
                Small(
                    "${fmt.format(Date(h.wallMs))} · " + when {
                        h.error != null && codes.isEmpty() -> h.error
                        codes.isEmpty() -> "nenhum código"
                        else -> codes.joinToString(", ")
                    } + (if (h.mil?.milOn == true) " · luz acesa" else ""),
                )
            }
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val c = JTheme.colors
    val shape = RoundedCornerShape(Dimens.RadiusCard)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.surfaceBorder, shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun ScanButton(enabled: Boolean, onClick: () -> Unit) {
    val c = JTheme.colors
    Box(
        Modifier
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(if (enabled) c.accent.copy(alpha = 0.28f) else c.surfaceHover)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Ler falhas", onClick = onClick)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("LER FALHAS", style = JancarType.chip.copy(color = if (enabled) c.fg else c.faint, letterSpacing = 1.sp))
    }
}

@Composable
private fun CodeRow(code: String, kind: String, title: String, severity: Severity, onClick: () -> Unit) {
    val c = JTheme.colors
    val tone = when (severity) { Severity.Alta -> SevHigh; Severity.Media -> SevMid; Severity.Baixa -> c.muted }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(c.surfaceHover)
            .clickable(role = Role.Button, onClickLabel = "Ver explicação", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).heightIn(min = 36.dp).clip(RoundedCornerShape(3.dp)).background(tone))
        Spacer(Modifier.width(12.dp))
        BasicText(code, style = JancarType.panelTitle.copy(color = c.fg, fontWeight = FontWeight.W600))
        Spacer(Modifier.width(14.dp))
        BasicText(title, style = JancarType.body.copy(color = c.fg, fontSize = 16.sp), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        BasicText(kind, style = JancarType.appTag.copy(color = tone))
    }
}

@Composable
private fun Small(text: String, strong: Boolean = false) {
    val c = JTheme.colors
    BasicText(
        text,
        style = JancarType.body.copy(
            color = if (strong) c.fg else c.muted, fontSize = 15.sp,
            fontWeight = if (strong) FontWeight.W600 else FontWeight.W400,
        ),
    )
}
