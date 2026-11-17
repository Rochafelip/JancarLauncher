package com.rocha.jancarlauncher

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Tela da sonda da câmera de ré (só debug; ver [ReverseProbe]).
 * `adb shell am start -n com.rocha.jancarlauncher/.ReverseProbeActivity [--ez overlay true]`
 * A sonda continua rodando com esta tela atrás da câmera; "Encerrar teste" desliga tudo.
 */
class ReverseProbeActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var logView: TextView
    private lateinit var overlayBtn: Button
    private lateinit var restoreBtn: Button

    private val tick = object : Runnable {
        override fun run() { render(); ui.postDelayed(this, 500) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReverseProbe.start(this)
        if (intent.getBooleanExtra("overlay", false)) ReverseProbe.showOverlay()

        val d = resources.displayMetrics.density
        fun btn(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label; textSize = 16f; isAllCaps = false; minHeight = (56 * d).toInt()
            setOnClickListener { onClick(); render() }
        }
        overlayBtn = btn("") { if (ReverseProbe.overlayOn) ReverseProbe.hideOverlay() else ReverseProbe.showOverlay() }
        restoreBtn = btn("") { ReverseProbe.originalAudio()?.let { ReverseProbe.setAudio(it) } }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            listOf(
                overlayBtn,
                btn("Volume na ré: testar 100") { ReverseProbe.setAudio(100) },
                restoreBtn,
                btn("Reler") { ReverseProbe.readAudio() },
                btn("Encerrar teste") { ReverseProbe.stop(); finish() },
            ).forEach { addView(it, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)) }
        }
        status = TextView(this).apply { setTextColor(Color.WHITE); textSize = 17f; typeface = Typeface.MONOSPACE }
        logView = TextView(this).apply { setTextColor(Color.rgb(0xC9, 0xCD, 0xD3)); textSize = 13f; typeface = Typeface.MONOSPACE }
        val pad = (16 * d).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (150 * d).toInt(), pad, pad) // abaixo da caixa de texto da camada de teste
            addView(buttons, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(status, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = pad })
            addView(logView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = pad })
        }
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.rgb(0x0A, 0x0B, 0x0F)); addView(column) })
    }

    override fun onResume() { super.onResume(); ui.post(tick) }
    override fun onPause() { super.onPause(); ui.removeCallbacks(tick) }

    private fun render() {
        val app = application as JancarApp
        val car = app.carService.state.value
        val a = ReverseProbe.audio
        overlayBtn.text = if (ReverseProbe.overlayOn) "Camada de teste: DESLIGAR" else "Camada de teste: LIGAR"
        restoreBtn.text = "Volume na ré: voltar ao original (${ReverseProbe.originalAudio() ?: "?"})"
        restoreBtn.isEnabled = ReverseProbe.originalAudio() != null
        status.text = buildString {
            appendLine("SONDA DA RÉ ${if (ReverseProbe.running) "rodando" else "parada"}")
            appendLine("Ré (ICar, fio):    ${car.reverseOn ?: "sem dado"}   ICar conectado=${app.carService.connected.value}")
            appendLine("Último aviso:      ${ReverseProbe.lastBroadcast ?: "nenhum"}")
            appendLine("CAN conectado:     ${app.canBus.connected.value}")
            appendLine("Volante (CAN):     ${app.canBus.steering.value?.let { ReverseProbe.steerText(it) } ?: "sem dado"}")
            appendLine("Sensores (CAN):    ${app.canBus.radar.value?.let { ReverseProbe.radarText(it) } ?: "sem dado"}")
            appendLine("Volume na ré (15): valor=${a?.value ?: "-"} min=${a?.min ?: "-"} max=${a?.max ?: "-"} " +
                "padrão=${a?.default ?: "-"} existe=${a?.available ?: "-"}")
            append("Camada de teste:   ${if (ReverseProbe.overlayOn) "ligada" else "desligada"}")
        }
        logView.text = ReverseProbe.recentLog().joinToString("\n")
    }
}
