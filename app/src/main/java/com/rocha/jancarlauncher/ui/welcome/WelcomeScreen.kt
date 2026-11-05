package com.rocha.jancarlauncher.ui.welcome

import androidx.compose.foundation.Image
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

// A foto já vem tratada (desfoque + duotone), então as cores daqui são fixas nos dois temas.
private val Fg = Color(0xFFEEF0F2)

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
            painter = painterResource(R.drawable.boot_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(10, 11, 15, 77),
                    0.68f to Color(10, 11, 15, 153),
                    1f to Color(8, 9, 12, 230),
                ),
            ),
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            BasicText(
                "MULTIMÍDIA",
                style = JancarType.caption.copy(color = Fg.copy(alpha = 0.55f), letterSpacing = 3.sp),
            )
            BasicText(clock.time, style = JancarType.clockBoot.copy(color = Fg))
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
}
