package com.rocha.jancarlauncher.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.R

private fun inter(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Inter (variável) — pesos usados pelo design: 200, 400, 500, 600. */
val Inter = FontFamily(inter(200), inter(400), inter(500), inter(600))

object JancarType {
    val clockLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W200, fontSize = 92.sp, letterSpacing = (-2).sp, lineHeight = 92.sp)
    val clockBoot = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W200, fontSize = 88.sp, letterSpacing = (-2).sp, lineHeight = 88.sp)
    val date = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W400, fontSize = 18.sp, letterSpacing = 0.3.sp)
    val dateBoot = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W400, fontSize = 16.sp)
    val caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 13.sp, letterSpacing = 2.5.sp)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W400, fontSize = 13.sp)
    val tileLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 15.sp, letterSpacing = 0.2.sp)
    val chip = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 13.sp)
    val appName = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 14.sp)
    val appTag = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 10.5.sp, letterSpacing = 0.6.sp)
    val h1 = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 26.sp, letterSpacing = (-0.3).sp)
    val panelTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 14.sp)
}
