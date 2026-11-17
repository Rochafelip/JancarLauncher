package com.rocha.jancarlauncher.ui.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.obd.TripCost
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/** Ponto de partida dos botões quando o preço nunca foi definido. */
private const val START_GAS_CENTS = 600
private const val START_ETH_CENTS = 400

/**
 * Preço do litro da gasolina e do etanol (custo da viagem). Botões de 56 dp (−0,10 / − / + / +0,10) e segurar
 * repete; sem teclado (dirigindo, o teclado é ruim). OK grava; tocar fora fecha sem gravar.
 */
@Composable
fun PricePanel(gasCents: Int?, ethCents: Int?, onSave: (gas: Int, eth: Int) -> Unit, onClose: () -> Unit) {
    val c = JTheme.colors
    var gas by remember { mutableIntStateOf(gasCents ?: START_GAS_CENTS) }
    var eth by remember { mutableIntStateOf(ethCents ?: START_ETH_CENTS) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(Dimens.RadiusPanel)
        Column(
            Modifier
                .width(620.dp)
                .clip(shape)
                .background(c.surface.copy(alpha = 1f))
                .border(1.dp, c.surfaceBorder, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BasicText("PREÇO DO LITRO", style = JancarType.caption.copy(color = c.muted))
            PriceRow("Gasolina", gas) { gas = TripCost.step(gas, it) }
            PriceRow("Etanol", eth) { eth = TripCost.step(eth, it) }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(Dimens.RadiusButton))
                        .background(c.accent.copy(alpha = 0.28f))
                        .clickable(role = Role.Button, onClickLabel = "Gravar preços") { onSave(gas, eth) }
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center,
                ) { BasicText("OK", style = JancarType.chip.copy(color = c.fg, letterSpacing = 1.sp)) }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, cents: Int, onStep: (Int) -> Unit) {
    val c = JTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText(label, style = JancarType.body.copy(color = c.fg), modifier = Modifier.width(110.dp))
        StepButton("−0,10", "Diminuir $label em 10 centavos") { onStep(-10) }
        StepButton("−", "Diminuir $label em 1 centavo") { onStep(-1) }
        BasicText(
            "R$ " + String.format(Locale("pt", "BR"), "%.2f", cents / 100.0),
            style = JancarType.panelTitle.copy(color = c.fg, fontWeight = FontWeight.W600, fontSize = 22.sp),
            modifier = Modifier.width(120.dp).padding(horizontal = 6.dp),
        )
        StepButton("+", "Aumentar $label em 1 centavo") { onStep(+1) }
        StepButton("+0,10", "Aumentar $label em 10 centavos") { onStep(+10) }
    }
}

/** Botão de 56 dp: um toque = um passo; segurando, repete a cada 150 ms (depois de 400 ms). */
@Composable
private fun StepButton(label: String, description: String, onStep: () -> Unit) {
    val c = JTheme.colors
    val step by rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(width = if (label.length > 1) 72.dp else 56.dp, height = 56.dp)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(c.surfaceHover)
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick { step(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    val job = scope.launch {
                        step()
                        delay(400)
                        while (true) { step(); delay(150) }
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                }
            },
        contentAlignment = Alignment.Center,
    ) { BasicText(label, style = JancarType.chip.copy(color = c.fg)) }
}
