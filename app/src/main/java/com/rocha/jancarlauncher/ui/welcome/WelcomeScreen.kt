package com.rocha.jancarlauncher.ui.welcome

import androidx.compose.foundation.Image
import com.rocha.jancarlauncher.ui.theme.ScreenStyles
import com.rocha.jancarlauncher.ui.theme.screenStyles
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.R
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.home.ClockInfo
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.JancarType

// Foto real do carro (a mesma da Home, estilo dos temas; antes era a tratada em azul com desfoque), com degradê
// escuro: as cores daqui são fixas nos dois temas.
private val Fg = Color(0xFFEEF0F2)

/** Sombra leve no relógio: a foto real tem a parede branca atrás do carro. */
private val TextShadow = androidx.compose.ui.graphics.Shadow(Color(0xB3000000), blurRadius = 14f)

@Composable
fun WelcomeScreen(clock: ClockInfo, onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Toque para continuar" }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onContinue,
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.home_wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Versão escolhida na galeria (aba Boas-vindas); a tela inteira continua sendo o "toque para continuar".
        when (screenStyles().welcome) {
            ScreenStyles.Welcome.Centro -> WelcomeCentro(clock)
            ScreenStyles.Welcome.Topo -> WelcomeTopo(clock)
            ScreenStyles.Welcome.Canto -> WelcomeCanto(clock)
            ScreenStyles.Welcome.Faixa -> WelcomeFaixa(clock)
        }
    }
}

private fun veil(vararg stops: Pair<Float, Int>) = Brush.verticalGradient(*stops.map { (p, a) -> p to Color(10, 11, 15, a) }.toTypedArray())

/** Atual: relógio no centro, sobre o carro. */
@Composable
private fun BoxScope.WelcomeCentro(clock: ClockInfo) {
    Box(Modifier.fillMaxSize().background(veil(0f to 110, 0.55f to 150, 1f to 230)))
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        BasicText("MULTIMÍDIA", style = JancarType.caption.copy(color = Fg.copy(alpha = 0.55f), letterSpacing = 3.sp))
        BasicText(clock.time, style = JancarType.clockBoot.copy(color = Fg, shadow = TextShadow))
        BasicText(clock.date, style = JancarType.dateBoot.copy(color = Fg.copy(alpha = 0.65f)), modifier = Modifier.padding(top = 4.dp))
    }
    Column(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        JIcon(JIcons.ChevronUp, Fg.copy(alpha = 0.6f), 20.dp)
        BasicText("Toque para continuar", style = JancarType.body.copy(color = Fg.copy(alpha = 0.6f), letterSpacing = 0.5.sp))
    }
}

/** Relógio no topo e "Toque para continuar" numa pílula embaixo: o carro fica livre no meio. */
@Composable
private fun BoxScope.WelcomeTopo(clock: ClockInfo) {
    Box(Modifier.fillMaxSize().background(veil(0f to 225, 0.22f to 140, 0.40f to 0, 0.78f to 0, 1f to 205)))
    Column(Modifier.align(Alignment.TopCenter).padding(top = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText("MULTIMÍDIA", style = JancarType.caption.copy(color = Fg.copy(alpha = 0.6f), letterSpacing = 3.sp))
        BasicText(clock.time, style = JancarType.clockBoot.copy(color = Fg, fontSize = 84.sp, lineHeight = 88.sp, shadow = TextShadow))
        BasicText(clock.date, style = JancarType.dateBoot.copy(color = Fg.copy(alpha = 0.75f)))
    }
    ContinuePill(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), light = false)
}

/** Relógio grande embaixo à esquerda e um botão claro à direita. */
@Composable
private fun BoxScope.WelcomeCanto(clock: ClockInfo) {
    Box(Modifier.fillMaxSize().background(veil(0f to 100, 0.22f to 0, 0.52f to 0, 1f to 235)))
    BasicText(
        "MULTIMÍDIA", style = JancarType.caption.copy(color = Fg.copy(alpha = 0.75f), letterSpacing = 3.sp),
        modifier = Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 26.dp),
    )
    Column(Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(clock.time, style = JancarType.clockBoot.copy(color = Fg, fontSize = 112.sp, lineHeight = 112.sp, shadow = TextShadow))
        BasicText(clock.date, style = JancarType.dateBoot.copy(color = Fg.copy(alpha = 0.78f)))
    }
    ContinuePill(Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 40.dp), light = true)
}

/** O carro ocupa a tela; hora, data e o botão numa faixa escura no rodapé. */
@Composable
private fun BoxScope.WelcomeFaixa(clock: ClockInfo) {
    Row(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(104.dp).background(Color(0xDB08090C)).padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BasicText(clock.time, style = JancarType.clockBoot.copy(color = Fg, fontSize = 60.sp, lineHeight = 60.sp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            BasicText(clock.date, style = JancarType.dateBoot.copy(color = Fg.copy(alpha = 0.78f), fontSize = 17.sp))
            BasicText("MULTIMÍDIA", style = JancarType.caption.copy(color = Fg.copy(alpha = 0.45f), letterSpacing = 3.sp, fontSize = 12.sp))
        }
        ContinuePill(Modifier, light = false)
    }
}

/** "Toque para continuar": a tela toda já é o botão; a pílula só mostra onde. */
@Composable
private fun ContinuePill(modifier: Modifier, light: Boolean) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .heightIn(min = 52.dp)
            .background(if (light) Fg else Color(0x9E0A0B0F), shape)
            .then(if (light) Modifier else Modifier.border(1.dp, Fg.copy(alpha = 0.14f), shape))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val color = if (light) Color(0xFF0A0B0F) else Fg
        BasicText("Toque para continuar", style = JancarType.body.copy(color = color, fontSize = 16.sp))
        JIcon(JIcons.ChevronUp, color, 18.dp)
    }
}
