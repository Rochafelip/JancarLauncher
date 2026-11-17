package com.rocha.jancarlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.BuildConfig
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.components.OnPhotoSurfaces
import com.rocha.jancarlauncher.ui.components.PhotoBackdrop
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/**
 * Ajustes do launcher (aberta por "Ajustes do launcher" no painel de brilho): uma coluna de seções. Cada recurso
 * novo entra como uma seção (Relatório de erros, Atualizar, Retomar a música); a versão instalada fica no fim.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, sections: List<@Composable () -> Unit>) {
    val c = JTheme.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        PhotoBackdrop(dim = 0.86f)
        OnPhotoSurfaces {
            Column(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp, top = 36.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    JIconButton(JIcons.Back, "Voltar para a tela inicial", onBack, size = 52.dp, iconSize = 22.dp)
                    BasicText("Ajustes do launcher", style = JancarType.h1.copy(color = c.fg, fontSize = 26.sp))
                }
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    sections.forEach { it() }
                    SettingsCard("Versão instalada") {
                        BasicText(BuildConfig.VERSION_NAME, style = JancarType.body.copy(color = c.muted))
                    }
                }
            }
        }
    }
}

/** Cartão de uma seção dos Ajustes: título e conteúdo. */
@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = JTheme.colors
    val shape = RoundedCornerShape(Dimens.RadiusCard)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.surfaceBorder, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(title, style = JancarType.panelTitle.copy(color = c.fg))
        content()
    }
}

/** Botão de texto dos Ajustes (altura de toque 56 dp). [accent] = ação principal. */
@Composable
fun SettingsButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Boolean = false, enabled: Boolean = true) {
    val c = JTheme.colors
    val shape = RoundedCornerShape(Dimens.RadiusButton)
    Box(
        modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (accent && enabled) c.accent.copy(alpha = 0.22f) else c.surfaceHover)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = JancarType.chip.copy(color = if (enabled) c.fg else c.faint))
    }
}
