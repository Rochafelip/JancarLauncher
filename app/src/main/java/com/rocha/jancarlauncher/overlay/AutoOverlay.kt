package com.rocha.jancarlauncher.overlay

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.R
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.dash.GearEstimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Cartão do Painel flutuando por cima do Android Auto/CarPlay (pedido do dono, 2026-11-13: "a view do Android Auto
 * na tela de Painel" — embutir o AA não dá sem ser app do sistema, então o Painel vai por cima dele). Visual =
 * modelo 1 "Cartão" escolhido no Claude Design (2026-11-14); **arrastável** para qualquer lugar, como um widget
 * (pedido do dono), com a posição salva.
 *
 * - Aparece só com o AA/CarPlay na frente (descoberto pelo acesso ao uso, `GET_USAGE_STATS`, concedido por adb).
 * - Janela `TYPE_APPLICATION_OVERLAY` sem foco e `FLAG_NOT_TOUCH_MODAL`: só a área do cartão recebe toque (para
 *   arrastar); o resto da tela continua indo para o AA. Toque parado não faz nada.
 * - Desenhado à mão num `View` (sem Compose fora da activity): leve, 4 quadros/s só enquanto aparece.
 * - Enquanto aparece, segura a conexão contada do CAN (velocidade/RPM); o OBD já fica ligado pelo AppRoot.
 */
class AutoOverlay(private val app: JancarApp) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var view: CardView? = null
    private var params: WindowManager.LayoutParams? = null
    private var mode = DashModel.DriveMode.Daily
    private val gearEstimator = GearEstimator()

    private var foreground: String? = null
    private var loggedForeground: String? = null
    private var lastQuery = 0L
    private val prefs by lazy { app.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            launch { app.settings.driveMode.collect { mode = DashModel.DriveMode.fromName(it) } }
            lastQuery = System.currentTimeMillis() - 60 * 60_000L // pega quem já estava na frente
            var lastCheck = 0L
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastCheck >= 1_000) {
                    lastCheck = now
                    updateForeground()
                }
                val show = OverlayPolicy.shouldShow(foreground, Settings.canDrawOverlays(app), debugTarget)
                val lines = if (show) currentLines() else null
                if (lines != null && !lines.isEmpty) showView().update(lines) else hideView()
                delay(if (view != null) 250 else 1_000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        hideView()
    }

    private fun currentLines(): OverlayPolicy.Lines {
        debugLines?.let { return it }
        val r = DashModel.reading(
            app.carService.state.value, app.canBus.dashboard.value, app.canBus.cabin.value, app.canBus.lights.value,
        )
        val gear = gearEstimator.update(r.rpm, r.speed, SystemClock.uptimeMillis())
        return OverlayPolicy.lines(r, app.obd.live.value, mode, gear)
    }

    /** Último app que veio para a frente, pelos eventos de uso desde a última consulta. */
    private fun updateForeground() {
        if (!hasUsageAccess()) { foreground = null; return }
        val usm = app.getSystemService(UsageStatsManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(lastQuery - 2_000, now)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            @Suppress("DEPRECATION")
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) foreground = e.packageName
        }
        lastQuery = now
        if (foreground != loggedForeground) {
            Log.i(TAG, "app na frente: $foreground")
            loggedForeground = foreground
        }
    }

    private fun hasUsageAccess(): Boolean {
        val ops = app.getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    private fun showView(): CardView {
        view?.let { return it }
        val v = CardView(app)
        val d = app.resources.displayMetrics.density
        // Posição salva = distância da borda direita (x) e do topo (y); padrão: canto superior direito.
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = prefs.getInt(KEY_X, (14 * d).toInt())
            y = prefs.getInt(KEY_Y, (14 * d).toInt())
            title = "JancarLauncher painel"
        }
        v.onDrag = { dx, dy -> moveBy(v, dx, dy) }
        v.onDragEnd = { params?.let { prefs.edit().putInt(KEY_X, it.x).putInt(KEY_Y, it.y).apply() } }
        try {
            app.getSystemService(WindowManager::class.java).addView(v, lp)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Cartão sobre o AA: sem permissão de sobreposição? ${e.message}")
            return v
        }
        app.canBus.connect() // contada: só liga se ninguém mais ligou
        view = v
        params = lp
        return v
    }

    /** Arrasta o cartão; nunca sai da tela. Com gravidade END, arrastar para a direita diminui o x. */
    private fun moveBy(v: View, dx: Float, dy: Float) {
        val lp = params ?: return
        val dm = app.resources.displayMetrics
        lp.x = (lp.x - dx).roundToInt().coerceIn(0, (dm.widthPixels - v.width).coerceAtLeast(0))
        lp.y = (lp.y + dy).roundToInt().coerceIn(0, (dm.heightPixels - v.height).coerceAtLeast(0))
        runCatching { app.getSystemService(WindowManager::class.java).updateViewLayout(v, lp) }
    }

    private fun hideView() {
        val v = view ?: return
        view = null
        params = null
        runCatching { app.getSystemService(WindowManager::class.java).removeView(v) }
        app.canBus.disconnect()
    }

    /**
     * Modelo 1 "Cartão": velocidade grande, barrinha de RPM em 12 segmentos, linha do giro (cor da troca) e linha
     * do consumo, com divisória. Linha sem dado some. Durante o arraste a borda acende no azul do acento.
     */
    private class CardView(context: Context) : View(context) {
        var onDrag: (Float, Float) -> Unit = { _, _ -> }
        var onDragEnd: () -> Unit = {}

        private val d = context.resources.displayMetrics.density
        private val sp = context.resources.displayMetrics.scaledDensity
        private var lines: OverlayPolicy.Lines? = null
        private var dragging = false
        private var downX = 0f
        private var downY = 0f
        private var lastX = 0f
        private var lastY = 0f
        private val slop = ViewConfiguration.get(context).scaledTouchSlop * 1.5f

        private val inter: Typeface = runCatching { context.resources.getFont(R.font.inter_variable) }.getOrDefault(Typeface.DEFAULT)
        private val semibold: Typeface =
            if (Build.VERSION.SDK_INT >= 28) Typeface.create(inter, 600, false) else Typeface.create(inter, Typeface.BOLD)

        private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(0xE6, 0x0A, 0x0B, 0x0F) }
        private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1 * d }
        private val seg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val speedP = text(52f, semibold, FG, Paint.Align.RIGHT).apply { letterSpacing = -0.02f }
        private val valueP = text(22f, semibold, FG, Paint.Align.RIGHT)
        private val unitR = text(14f, inter, MUTED, Paint.Align.RIGHT)
        private val labelL = text(13f, inter, MUTED, Paint.Align.LEFT)
        private val rect = RectF()

        private val w = 188 * d
        private val pad = 16 * d

        private fun text(size: Float, tf: Typeface, c: Int, align: Paint.Align) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size * sp; typeface = tf; color = c; textAlign = align
        }

        fun update(l: OverlayPolicy.Lines) {
            val old = lines
            lines = l
            val shape = old == null || (old.speed == null) != (l.speed == null) || (old.rpm == null) != (l.rpm == null) ||
                (old.consumption == null) != (l.consumption == null) || (old.gear == null) != (l.gear == null)
            if (shape) requestLayout()
            invalidate()
        }

        // Alturas de cada bloco (px).
        private val speedH get() = 52 * sp
        private val barH = 6 * d
        private val rowH get() = 28 * sp
        private val gap = 8 * d

        private fun contentHeight(l: OverlayPolicy.Lines): Float {
            var h = 14 * d
            if (l.speed != null) h += speedH + gap
            if (l.rpm != null) h += barH + gap + rowH
            if (l.rpm != null && l.consumption != null) h += gap + 1 * d + gap
            if (l.consumption != null) h += rowH
            if (l.gear != null) h += rowH
            return h + 12 * d
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val l = lines ?: return setMeasuredDimension(0, 0)
            setMeasuredDimension(w.toInt(), contentHeight(l).toInt())
        }

        override fun onDraw(canvas: Canvas) {
            val l = lines ?: return
            val r = 18 * d
            rect.set(0.5f * d, 0.5f * d, width - 0.5f * d, height - 0.5f * d)
            canvas.drawRoundRect(rect, r, r, bg)
            border.color = if (dragging) ACCENT else Color.argb(0x12, 0xFF, 0xFF, 0xFF)
            border.strokeWidth = (if (dragging) 2 else 1) * d
            canvas.drawRoundRect(rect, r, r, border)

            val left = pad
            val right = width - pad
            val blinkOn = (SystemClock.uptimeMillis() / 500) % 2 == 0L
            val stageColor = when (l.stage) {
                DashModel.ShiftStage.Shift -> RED
                DashModel.ShiftStage.Near -> AMBER
                DashModel.ShiftStage.None -> FG
            }
            val hidden = l.stage == DashModel.ShiftStage.Shift && !blinkOn
            var y = 14 * d

            l.speed?.let {
                val base = y + speedH * 0.86f
                val unitW = unitR.measureText("km/h")
                canvas.drawText("km/h", right, base, unitR)
                canvas.drawText(it, right - unitW - 6 * d, base, speedP)
                y += speedH + gap
            }
            if (l.rpm != null) {
                // Barrinha: 12 segmentos até 7000 rpm, na cor do giro (azul no normal).
                val n = 12
                val segGap = 3 * d
                val segW = (right - left - segGap * (n - 1)) / n
                val lit = ((l.rpmValue ?: 0f) / DashModel.RPM_MAX * n).roundToInt()
                for (i in 0 until n) {
                    seg.color = when {
                        i >= lit -> Color.argb(0x14, 0xFF, 0xFF, 0xFF)
                        hidden -> Color.argb(0x14, 0xFF, 0xFF, 0xFF)
                        l.stage == DashModel.ShiftStage.None -> ACCENT
                        else -> stageColor
                    }
                    val x0 = left + i * (segW + segGap)
                    rect.set(x0, y, x0 + segW, y + barH)
                    canvas.drawRoundRect(rect, 2 * d, 2 * d, seg)
                }
                y += barH + gap
                val base = y + rowH * 0.72f
                canvas.drawText("rpm", left, base, labelL)
                valueP.color = if (hidden) Color.TRANSPARENT else stageColor
                canvas.drawText(l.rpm, right, base, valueP)
                valueP.color = FG
                y += rowH
            }
            if (l.rpm != null && l.consumption != null) {
                y += gap
                seg.color = Color.argb(0x14, 0xFF, 0xFF, 0xFF)
                canvas.drawRect(left, y, right, y + 1 * d, seg)
                y += 1 * d + gap
            }
            l.consumption?.let {
                val base = y + rowH * 0.72f
                canvas.drawText(it.substringAfterLast(' '), left, base, labelL)
                canvas.drawText(it.substringBeforeLast(' '), right, base, valueP)
                y += rowH
            }
            // Marcha + sugestão (↑ na cor do aviso de troca, ↓ no azul do acento).
            l.gear?.let { g ->
                val base = y + rowH * 0.72f
                canvas.drawText("marcha", left, base, labelL)
                var x = right
                l.hint?.let { h ->
                    valueP.color = when {
                        l.hintDown -> ACCENT
                        l.stage == DashModel.ShiftStage.Shift -> RED
                        else -> AMBER
                    }
                    canvas.drawText(h, x, base, valueP)
                    x -= valueP.measureText(h) + 10 * d
                    valueP.color = FG
                }
                canvas.drawText(g, x, base, valueP)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; lastX = e.rawX; lastY = e.rawY
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    // Só começa a arrastar depois de um pouco de movimento: um toque sem querer não tira do lugar.
                    if (!dragging && (abs(e.rawX - downX) > slop || abs(e.rawY - downY) > slop)) {
                        dragging = true
                        lastX = e.rawX; lastY = e.rawY
                        invalidate()
                    }
                    if (dragging) {
                        onDrag(e.rawX - lastX, e.rawY - lastY)
                        lastX = e.rawX; lastY = e.rawY
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) onDragEnd()
                    dragging = false
                    invalidate()
                }
            }
            return true
        }
    }

    companion object {
        private const val TAG = "AutoOverlay"
        private const val PREFS = "auto_overlay"
        private const val KEY_X = "x_from_right"
        private const val KEY_Y = "y_from_top"
        private val FG = Color.rgb(0xEE, 0xF0, 0xF2)
        private val MUTED = Color.rgb(0x88, 0x8D, 0x94)
        private val ACCENT = Color.rgb(0x6F, 0xA3, 0xD8)
        private val AMBER = Color.rgb(0xF2, 0xA9, 0x3B)
        private val RED = Color.rgb(0xEF, 0x44, 0x44)

        /** Só para a prévia de debug: trata outro pacote como se fosse o AA e usa dados falsos. */
        @Volatile var debugTarget: String? = null
        @Volatile var debugLines: OverlayPolicy.Lines? = null
    }
}
