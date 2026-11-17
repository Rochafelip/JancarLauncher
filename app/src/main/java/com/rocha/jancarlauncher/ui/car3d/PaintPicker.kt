package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/** Fileira de cores do Fox (toque na foto abre; some sozinha). "Original" = o prata da foto. */
@Composable
fun PaintPicker(current: CarPaint, onPick: (CarPaint) -> Unit, modifier: Modifier = Modifier) {
    val c = JTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE6101114))
            .border(1.dp, c.surfaceBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(current.label.uppercase(), style = JancarType.appTag.copy(color = c.muted))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CarPaint.entries.forEach { p ->
                val selected = p == current
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton, onClickLabel = p.label) { onPick(p) },
                    contentAlignment = Alignment.Center,
                ) {
                    val fill = p.swatch?.let { Brush.linearGradient(listOf(Color(it), Color(it).copy(alpha = 0.75f))) }
                        ?: Brush.linearGradient(listOf(Color(0xFFE6E8EB), Color(0xFF9EA3AA)))
                    Box(
                        Modifier
                            .size(if (selected) 40.dp else 34.dp)
                            .clip(CircleShape)
                            .background(fill)
                            .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color(0x40FFFFFF), CircleShape),
                    )
                }
            }
        }
    }
}
