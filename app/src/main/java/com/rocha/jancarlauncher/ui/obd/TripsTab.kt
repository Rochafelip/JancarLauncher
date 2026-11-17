package com.rocha.jancarlauncher.ui.obd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.obd.TripRecord
import com.rocha.jancarlauncher.obd.TripText
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/** Aba Viagens: a viagem de agora (ao vivo) e as anteriores, uma por ligada do carro. Sem gráfico. */
@Composable
fun TripsTab(current: TripRecord?, trips: List<TripRecord>, gasCents: Int?, ethCents: Int?, nowWallMs: Long) {
    val c = JTheme.colors
    current?.let { TripCard("VIAGEM ATUAL", it, gasCents, ethCents, nowWallMs, highlight = true) }
    if (trips.isEmpty()) {
        if (current == null) {
            BasicText(
                "Nenhuma viagem ainda. Cada vez que o carro liga e anda com o OBD conectado, começa uma viagem nova.",
                style = JancarType.body.copy(color = c.muted),
            )
        }
        return
    }
    BasicText("ANTERIORES", style = JancarType.caption.copy(color = c.muted), modifier = Modifier.padding(top = 4.dp))
    trips.forEach { TripCard(null, it, gasCents, ethCents, nowWallMs) }
}

@Composable
private fun TripCard(label: String?, r: TripRecord, gasCents: Int?, ethCents: Int?, now: Long, highlight: Boolean = false) {
    val c = JTheme.colors
    val shape = RoundedCornerShape(Dimens.RadiusCard)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, if (highlight) c.accent.copy(alpha = 0.6f) else c.surfaceBorder, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(
            listOfNotNull(label, TripText.whenText(r, now)).joinToString(" · "),
            style = JancarType.appTag.copy(color = if (highlight) c.accent else c.muted),
        )
        BasicText(TripText.main(r, gasCents, ethCents), style = JancarType.panelTitle.copy(color = c.fg, fontWeight = FontWeight.W500, fontSize = 20.sp))
        BasicText(TripText.speeds(r), style = JancarType.body.copy(color = c.muted, fontSize = 15.sp))
    }
}
