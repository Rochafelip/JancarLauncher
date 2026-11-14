package com.rocha.jancarlauncher.ui.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.obd.FuelModel
import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.obd.Trip
import com.rocha.jancarlauncher.ui.components.JIcon
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import kotlinx.coroutines.delay

/** Textos do bloco de consumo (puros, testáveis). "—" = sem dado: nada é simulado. */
object ConsumptionText {
    /** Média agora: km/L andando; parado, L/h (km/L não existe com o carro parado). */
    fun instant(live: ObdClient.Live): Pair<String, String> = when {
        live.status != ObdClient.Status.Connected -> DashModel.DASH to "km/L"
        live.kmPerLiter != null -> DashModel.valueText(live.kmPerLiter.coerceAtMost(99.9f)) to "km/L"
        live.litersPerHour != null -> DashModel.valueText(live.litersPerHour) to "L/h"
        else -> DashModel.DASH to "km/L"
    }

    fun trip(t: Trip): String = t.kmPerLiter?.let { DashModel.valueText(it.toFloat().coerceAtMost(99.9f)) } ?: DashModel.DASH

    fun tripKm(t: Trip): String = DashModel.valueText(t.km.toFloat())

    /** Linha de status: só aparece quando algo falta. */
    fun status(live: ObdClient.Live): String? = when (live.status) {
        ObdClient.Status.Connected -> null
        ObdClient.Status.Off -> "OBD desligado"
        ObdClient.Status.NoAdapter -> "Adaptador OBD não pareado"
        ObdClient.Status.Connecting -> "Conectando ao OBD…"
        ObdClient.Status.NoFuelData -> "O carro não informa o ar do motor"
    }

    /** O botão de combustível só aparece quando a conta depende dele: conectado e o carro não diz o % de etanol. */
    fun needsBlend(live: ObdClient.Live): Boolean = live.status == ObdClient.Status.Connected && live.ethanolPercent == null

    /** Rótulo da média agora (com o etanol informado pelo carro, quando ele informa). */
    fun instantCaption(live: ObdClient.Live): String = live.ethanolPercent?.let { "AGORA · E${it.toInt()}" } ?: "AGORA"
}

/**
 * Consumo pelo OBD-II (pedido do dono, 2026-11-13), na coluna do meio do Painel: média agora e média da viagem.
 * Estimativa pelo ar admitido ([FuelModel]). Sem OBD some a média agora; fica a viagem guardada (se houver) e
 * uma linha discreta dizendo o porquê.
 */
@Composable
fun ConsumptionBlock(live: ObdClient.Live, trip: Trip, onResetTrip: () -> Unit, modifier: Modifier = Modifier) {
    val c = JTheme.colors
    val digits = JancarType.clockLarge.copy(fontWeight = FontWeight.W500, letterSpacing = 0.sp, fontFeatureSettings = "tnum")
    val connected = live.status == ObdClient.Status.Connected
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (connected) {
            val (now, unit) = ConsumptionText.instant(live)
            Caption(ConsumptionText.instantCaption(live))
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText(now, style = digits.copy(fontSize = 48.sp, lineHeight = 50.sp, color = if (now == DashModel.DASH) c.faint else c.fg))
                Spacer(Modifier.width(6.dp))
                BasicText(unit, style = JancarType.tileLabel.copy(color = c.muted), modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(14.dp))
        }
        if (connected || trip.km > 0.0) {
            Caption("VIAGEM · ${ConsumptionText.tripKm(trip)} km")
            Row(verticalAlignment = Alignment.CenterVertically) {
                val tv = ConsumptionText.trip(trip)
                BasicText(tv, style = digits.copy(fontSize = 34.sp, lineHeight = 36.sp, color = if (tv == DashModel.DASH) c.faint else c.fg))
                Spacer(Modifier.width(6.dp))
                BasicText("km/L", style = JancarType.tileLabel.copy(color = c.muted))
                Spacer(Modifier.width(14.dp))
                ResetButton(onResetTrip)
            }
        }
        ConsumptionText.status(live)?.let {
            Spacer(Modifier.height(8.dp))
            BasicText(it, style = JancarType.appTag.copy(color = c.faint, textAlign = TextAlign.Center))
        }
    }
}

@Composable
private fun Caption(text: String) {
    BasicText(text, style = JancarType.caption.copy(color = JTheme.colors.faint, fontSize = 13.sp))
}

/** ZERAR com confirmação: o 1º toque arma ("ZERAR?" em âmbar por 3 s), o 2º zera — um toque sem querer não apaga a viagem. */
@Composable
private fun ResetButton(onReset: () -> Unit) {
    val c = JTheme.colors
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(3_000); armed = false } }
    val shape = RoundedCornerShape(Dimens.RadiusButton)
    BasicText(
        if (armed) "ZERAR?" else "ZERAR",
        style = JancarType.chip.copy(color = if (armed) dash.warn else c.fg, fontSize = 14.sp, letterSpacing = 1.sp),
        modifier = Modifier
            .clip(shape)
            .background(c.surface)
            .border(1.dp, if (armed) dash.warn else c.surfaceHover, shape)
            .clickable(onClickLabel = "Zerar média da viagem", role = Role.Button) {
                if (armed) { armed = false; onReset() } else armed = true
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
    )
}

/** Combustível do tanque, no topo do Painel ao lado do modo (é ajuste, não leitura). Mesmo formato do botão de modo. */
@Composable
fun FuelBlendButton(blend: FuelModel.Blend, onClick: () -> Unit) {
    val c = JTheme.colors
    Row(
        Modifier
            .height(Dimens.MinTouch)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(c.surface)
            .clickable(onClickLabel = "Trocar combustível do tanque", role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JIcon(JIcons.Fuel, tint = c.fg, size = 20.dp)
        Spacer(Modifier.width(10.dp))
        BasicText(blend.label, style = JancarType.chip.copy(color = c.fg, letterSpacing = 1.sp))
    }
}
