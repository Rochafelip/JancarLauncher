package com.rocha.jancarlauncher.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ícones do preview (SVG 24×24) convertidos para ImageVector. Preto aqui; a cor real vem do tint em [JIcon].
 */
private class IconScope(private val b: ImageVector.Builder, private val stroke: Float) {
    private fun nodes(d: String) = PathParser().parsePathString(d).toNodes()

    fun line(d: String) {
        b.addPath(
            pathData = nodes(d),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    fun fill(d: String) {
        b.addPath(pathData = nodes(d), fill = SolidColor(Color.Black))
    }

    fun circle(cx: Float, cy: Float, r: Float) = line(circlePath(cx, cy, r))
    fun dot(cx: Float, cy: Float, r: Float) = fill(circlePath(cx, cy, r))
    fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) = line(rectPath(x, y, w, h, rx))
}

private fun circlePath(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun rectPath(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
        "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

private fun icon(name: String, stroke: Float = 1.6f, block: IconScope.() -> Unit): ImageVector {
    val b = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    IconScope(b, stroke).block()
    return b.build()
}

object JIcons {
    /** Alternar tema: metade sol (contorno + raios, à esquerda) e metade lua crescente (cheia, à direita). */
    val ThemeToggle = icon("ThemeToggle", 1.8f) {
        line("M12 6a6 6 0 0 0 0 12")
        line("M12 3.6V1.6M12 20.4v2M3.6 12h-2M6.1 6.1L4.7 4.7M6.1 17.9l-1.4 1.4")
        fill("M12 6a6 6 0 0 1 0 12a9 9 0 0 0 0-12z")
    }
    /** Brilho: sol inteiro com 8 raios (o de tema é meio sol / meio lua, não confundem). */
    val Brightness = icon("Brightness", 1.7f) {
        circle(12f, 12f, 4f)
        line("M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7")
    }
    val Grid = icon("Grid") {
        for (cy in listOf(6f, 12f, 18f)) for (cx in listOf(6f, 12f, 18f)) dot(cx, cy, 1.6f)
    }
    /** Botão "Painel": velocímetro (arco com marcas + ponteiro). */
    val Gauge = icon("Gauge", 1.7f) {
        line("M3.5 17a8.5 8.5 0 1 1 17 0")
        line("M6 9.8l1.3 1.1M12 5.5v1.7M18 9.8l-1.3 1.1")
        line("M12 17l3.5-5")
        dot(12f, 17f, 1.4f)
    }
    /** Botão "Apagar tela": símbolo de energia (arco aberto em cima + traço). */
    val Power = icon("Power", 1.7f) { line("M12 3.5v7.5"); line("M7.4 6.6a7.5 7.5 0 1 0 9.2 0") }
    val Back = icon("Back", 1.8f) { line("M15 6l-6 6 6 6") }
    val ChevronUp = icon("ChevronUp", 1.8f) { line("M6 15l6-6 6 6") }

    val Wifi = icon("Wifi", 1.7f) {
        line("M2 8.5a16 16 0 0 1 20 0"); line("M5.5 12.5a11 11 0 0 1 13 0"); line("M9 16.5a6 6 0 0 1 6 0")
        dot(12f, 20f, 1f)
    }
    val Bluetooth = icon("Bluetooth", 1.7f) { line("M7 7l10 10-5 4V3l5 4L7 17") }
    val Gps = icon("Gps", 1.7f) {
        line("M12 21s7-7.5 7-12a7 7 0 1 0-14 0c0 4.5 7 12 7 12z"); circle(12f, 9f, 2.3f)
    }
    val Settings = icon("Settings") {
        circle(12f, 12f, 3f)
        line("M19.4 13a7.97 7.97 0 0 0 0-2l2.1-1.6-2-3.4-2.5 1a8 8 0 0 0-1.7-1L14.9 3h-4l-.4 2.9a8 8 0 0 0-1.7 1l-2.5-1-2 3.4L6.6 11a7.97 7.97 0 0 0 0 2l-2.1 1.6 2 3.4 2.5-1a8 8 0 0 0 1.7 1l.4 2.9h4l.4-2.9a8 8 0 0 0 1.7-1l2.5 1 2-3.4L19.4 13z")
    }

    // Blocos principais
    val Auto = icon("Auto") { rect(2f, 4f, 20f, 13f, 2f); line("M8 21h8"); line("M12 17v4") }
    val Music = icon("Music") {
        circle(6.5f, 18f, 2.4f); circle(16.5f, 16f, 2.4f)
        line("M8.9 18V6.6L18.9 4.3v11.6"); line("M8.9 8.7l10-2.3")
    }
    val Radio = icon("Radio") {
        line("M9 7l3-3 3 3"); line("M12 10V4"); rect(3f, 10f, 18f, 10f, 2.5f)
        dot(8f, 15f, 1.3f); line("M13 15h5")
    }
    val Video = icon("Video") { rect(3f, 4f, 18f, 16f, 3f); fill("M10 9l5 3-5 3V9z") }

    // Gaveta
    val Vlc = icon("Vlc") { circle(12f, 12f, 9f); fill("M10 8.5l6 3.5-6 3.5v-7z") }
    val NewPipe = icon("NewPipe") { circle(11f, 11f, 7f); line("M16.5 16.5L21 21"); fill("M9.5 9l3 2-3 2V9z") }
    val FDroid = icon("FDroid") { line("M3 8l9-5 9 5-9 5-9-5z"); line("M3 8v8l9 5 9-5V8"); line("M12 13v8") }
    val Lightning = icon("Lightning") {
        circle(12f, 12f, 9f); line("M3 12h18"); line("M12 3a14 14 0 0 1 0 18 14 14 0 0 1 0-18z")
    }
    val Gallery = icon("Gallery") {
        rect(3f, 4f, 18f, 16f, 2.5f); dot(8.5f, 9.5f, 1.6f); line("M21 16l-5.5-5.5-4 4L7 11l-4 4")
    }
    val Folder = icon("Folder") {
        line("M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7z")
    }
    val BtMusic = icon("BtMusic") {
        line("M4 15V9a8 8 0 0 1 16 0v6"); line("M4 15a2 2 0 0 0 2 2h1v-6H6a2 2 0 0 0-2 2z"); line("M20 15a2 2 0 0 1-2 2h-1v-6h1a2 2 0 0 1 2 2z")
    }
    val Eq = icon("Eq") { line("M5 19V9"); line("M10 19V5"); line("M15 19v-7"); line("M20 19v-4") }
    val Vehicle = icon("Vehicle") {
        line("M4 16V11l2-5h12l2 5v5"); line("M4 16a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2"); line("M15 16a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2"); line("M4 11h16")
    }
    val Scrcpy = icon("Scrcpy") { rect(2f, 5f, 14f, 10f, 1.5f); line("M18 9l4-2v10l-4-2") }
    val Obd = icon("Obd") { line("M4 15a8 8 0 0 1 16 0"); line("M12 15l4-5"); dot(12f, 15f, 1.3f) }

    // Card "Tocando agora"
    val Play = icon("Play") { fill("M8 5l12 7-12 7V5z") }
    val Pause = icon("Pause") { fill("M7 5h4v14H7z"); fill("M13 5h4v14h-4z") }
    val Previous = icon("Previous") { fill("M6 5h2v14H6z"); fill("M18 5v14l-10-7 10-7z") }
    val Next = icon("Next") { fill("M16 5h2v14h-2z"); fill("M6 5v14l10-7-10-7z") }
    val Shuffle = icon("Shuffle") {
        line("M3 7h4l9 10h5"); line("M16 7h5v5"); line("M21 7l-5 5"); line("M3 17h4l3.5-4")
    }
    val NoteMusic = icon("NoteMusic") { circle(7f, 18f, 2.6f); line("M9.6 18V5.5L19.6 3v11.3"); circle(17f, 16f, 2.6f) }

    // Painel
    val Fuel = icon("Fuel", 1.7f) {
        rect(4f, 3f, 10f, 18f, 1.5f); line("M4 10h10")
        line("M14 8h2a2 2 0 0 1 2 2v6a1.5 1.5 0 0 0 3 0V9l-3-3")
    }
    val Thermometer = icon("Thermometer", 1.7f) {
        line("M14 14.76V4.5a2 2 0 0 0-4 0v10.26a4 4 0 1 0 4 0z"); line("M12 9v8")
    }
    val Road = icon("Road", 1.7f) { line("M5 21L9 3"); line("M19 21L15 3"); line("M12 5v2.5M12 11v2.5M12 17v2.5") }
    val Warning = icon("Warning", 1.8f) {
        line("M12 3.5L2.5 20h19L12 3.5z"); line("M12 9.5v5"); dot(12f, 17.2f, 1.1f)
    }
    val Handbrake = icon("Handbrake", 1.8f) {
        circle(12f, 12f, 6.5f); line("M12 8.5v4.2"); dot(12f, 15.4f, 1f)
        line("M4.6 6.6a9 9 0 0 0 0 10.8"); line("M19.4 6.6a9 9 0 0 1 0 10.8")
    }

    // Indicador de ADB sem fio
    val Usb = icon("Usb", 1.7f) {
        line("M12 2v9"); line("M9 6l3-3 3 3"); circle(12f, 16f, 3f); line("M9 16H7a2 2 0 0 1-2-2v-1")
        line("M15 16h2a2 2 0 0 0 2-2v-1")
    }
}
