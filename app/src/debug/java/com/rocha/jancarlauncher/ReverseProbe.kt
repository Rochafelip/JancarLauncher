package com.rocha.jancarlauncher

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.rocha.jancarlauncher.can.RadarInfo
import com.rocha.jancarlauncher.can.SteerWheel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sonda da câmera de ré (só debug; plano `docs/superpowers/plans/2026-11-16-verificacao-re-plan.md`, Parte B).
 * Junta num lugar só, e loga com a etiqueta `ReProbe`:
 * - ré pelo ICar (`CarState.reverseOn`, fio da ré) e pelos broadcasts `android.backcar.action.*`;
 * - sensores e volante pelo CAN (`CanBusClient.radar` / `steering`; segura a conexão contada enquanto roda);
 * - volume da música na ré: `IAudio` param 15 (`VOLUME_MEDIA_CCD`, 0–100, padrão 40), com teste em 100 e volta
 *   ao valor original (o original fica salvo, sobrevive a reinício do app);
 * - uma camada `TYPE_APPLICATION_OVERLAY` de tela cheia, sem toque, com linhas de exemplo e os valores ao vivo,
 *   para ver se o launcher consegue desenhar **por cima** da câmera.
 * Sobrevive à activity ir para trás (a câmera abre por cima); só para em [stop].
 */
object ReverseProbe {
    private const val TAG = "ReProbe"
    private const val PREFS = "reverse_probe"
    private const val KEY_ORIGINAL = "audio15_original"

    // IAudio (com.jancar.services, AudioService$1), códigos conferidos no ivi-services descompilado.
    private const val AUDIO_ACTION = "com.jancar.services.action.audio"
    private const val AUDIO_PKG = "com.jancar.services"
    private const val AUDIO_DESCRIPTOR = "com.jancar.services.audio.IAudio"
    private const val A_IS_AVAILABLE = 4
    private const val A_GET_MIN = 5
    private const val A_GET_MAX = 6
    private const val A_GET_DEFAULT = 7
    private const val A_GET = 8
    private const val A_SET = 9
    const val PARAM_MEDIA_CCD = 15

    private val BACKCAR_ACTIONS = listOf(
        "android.backcar.action.PREPARE_START", "android.backcar.action.STARTED", "android.backcar.action.FINISH",
        "action_backcar_notification",
        "com.zjinnova.zlink.action.BACKCAR_START", "com.zjinnova.zlink.action.BACKCAR_STOP",
    )

    data class Audio(val available: Boolean?, val min: Int?, val max: Int?, val default: Int?, val value: Int?)

    private lateinit var app: JancarApp
    private var scope: CoroutineScope? = null
    private val worker by lazy { HandlerThread("ReProbe").apply { start() } }
    private val bg by lazy { Handler(worker.looper) }
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var audioBinder: IBinder? = null
    @Volatile var audio: Audio? = null; private set
    @Volatile var lastBroadcast: String? = null; private set
    var running = false; private set

    private val logLines = ArrayDeque<String>()
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    /** Últimas linhas do log da sonda (as mais novas em cima), para a tela. */
    fun recentLog(): List<String> = synchronized(logLines) { logLines.toList() }

    fun log(msg: String) {
        Log.i(TAG, msg)
        synchronized(logLines) {
            logLines.addFirst("${timeFmt.format(Date())}  $msg")
            while (logLines.size > 40) logLines.removeLast()
        }
    }

    fun start(context: Context) {
        if (running) return
        app = context.applicationContext as JancarApp
        running = true
        log("sonda ligada; ICar conectado=${app.carService.connected.value}; sobreposicao=${Settings.canDrawOverlays(app)}")
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = s
        app.canBus.connect() // contada
        s.launch {
            app.carService.state.map { it.reverseOn }.distinctUntilChanged().collect { log("ICar re=$it") }
        }
        s.launch { app.canBus.radar.collect { r -> if (r != null) log("CAN radar ${radarText(r)}") } }
        s.launch { app.canBus.steering.collect { st -> if (st != null) log("CAN volante ${steerText(st)}") } }
        s.launch { app.canBus.connected.collect { log("CAN conectado=$it") } }
        s.launch {
            while (isActive) { delay(3_000); app.canBus.refresh() } // getRadarInfo/getSteerWheelInfo também
        }
        val filter = IntentFilter().apply { BACKCAR_ACTIONS.forEach { addAction(it) } }
        // Avisos de outros apps do sistema: no Android 13+ (emulador) precisa dizer que é exportado; a central é 8.1.
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        else app.registerReceiver(receiver, filter)
        bindAudio()
    }

    fun stop() {
        if (!running) return
        running = false
        hideOverlay()
        scope?.cancel(); scope = null
        runCatching { app.unregisterReceiver(receiver) }
        runCatching { app.unbindService(audioConn) }
        audioBinder = null
        app.canBus.disconnect()
        log("sonda desligada")
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras?.let { b -> b.keySet().joinToString { k -> "$k=${b.get(k)}" } } ?: ""
            val text = "${intent.action?.substringAfterLast('.')} {$extras}"
            lastBroadcast = "${timeFmt.format(Date())} $text"
            log("broadcast ${intent.action} {$extras}")
        }
    }

    // ---------- Áudio (IAudio) ----------

    private val audioConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            audioBinder = service
            log("IAudio conectado: $name")
            readAudio()
        }
        override fun onServiceDisconnected(name: ComponentName) { audioBinder = null; log("IAudio desconectou") }
    }

    private fun bindAudio() {
        val ok = runCatching {
            app.bindService(Intent(AUDIO_ACTION).setPackage(AUDIO_PKG), audioConn, Context.BIND_AUTO_CREATE)
        }.onFailure { log("bind IAudio falhou: $it") }.getOrDefault(false)
        if (!ok) log("bind IAudio retornou false")
    }

    fun readAudio() = bg.post {
        val a = runCatching {
            Audio(
                available = audioInt(A_IS_AVAILABLE)?.let { it != 0 },
                min = audioInt(A_GET_MIN), max = audioInt(A_GET_MAX), default = audioInt(A_GET_DEFAULT),
                value = audioInt(A_GET),
            )
        }.onFailure { log("leitura IAudio falhou: $it") }.getOrNull() ?: return@post
        audio = a
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_ORIGINAL) && a.value != null) prefs.edit().putInt(KEY_ORIGINAL, a.value).apply()
        log("volume na re (param 15): $a original=${originalAudio()}")
    }

    fun originalAudio(): Int? = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .let { if (it.contains(KEY_ORIGINAL)) it.getInt(KEY_ORIGINAL, 0) else null }

    /** Só o param 15 (volume da mídia na ré). Nenhum outro id é escrito por esta sonda. */
    fun setAudio(value: Int) = bg.post {
        val v = value.coerceIn(0, 100)
        runCatching {
            audioCall(A_SET) { it.writeInt(PARAM_MEDIA_CCD); it.writeInt(v) }?.recycle()
            log("setParam(15, $v) enviado")
        }.onFailure { log("setParam(15, $v) falhou: $it") }
        readAudio()
    }

    private fun audioInt(code: Int): Int? =
        audioCall(code) { it.writeInt(PARAM_MEDIA_CCD) }?.let { try { it.readInt() } finally { it.recycle() } }

    private fun audioCall(code: Int, write: (Parcel) -> Unit): Parcel? {
        val b = audioBinder ?: return null
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(AUDIO_DESCRIPTOR)
            write(data)
            b.transact(code, data, reply, 0)
            reply.readException()
            reply
        } catch (t: Throwable) {
            reply.recycle(); throw t
        } finally {
            data.recycle()
        }
    }

    // ---------- Camada por cima da câmera ----------

    private var overlay: GuideView? = null
    private var overlayJob: Job? = null
    val overlayOn get() = overlay != null

    fun showOverlay() {
        if (overlay != null) return
        if (!Settings.canDrawOverlays(app)) { log("sem permissao de sobreposicao (SYSTEM_ALERT_WINDOW)"); return }
        val v = GuideView(app)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START; title = "ReProbe linhas" }
        try {
            app.getSystemService(WindowManager::class.java).addView(v, lp)
        } catch (e: RuntimeException) {
            log("camada falhou: ${e.message}"); return
        }
        overlay = v
        log("camada de teste LIGADA")
        overlayJob = scope?.launch { while (isActive) { v.invalidate(); delay(250) } }
    }

    fun hideOverlay() {
        val v = overlay ?: return
        overlay = null
        overlayJob?.cancel(); overlayJob = null
        runCatching { app.getSystemService(WindowManager::class.java).removeView(v) }
        log("camada de teste DESLIGADA")
    }

    fun radarText(r: RadarInfo): String =
        "tras=${r.rear.joinToString(" ") { n(it) }} frente=${r.front.joinToString(" ") { n(it) }} " +
            "lig(F/T/L)=${n(r.frontOnOff)}/${n(r.rearOnOff)}/${n(r.sideOnOff)} max=${n(r.rearMax)}"

    fun steerText(s: SteerWheel): String = "angulo=${n(s.eps)} omega=${n(s.omega)}"

    private fun n(v: Int) = if (v == Int.MIN_VALUE) "-" else v.toString()

    /** Linhas de exemplo (curvam com o volante, se vier) + caixa com os valores ao vivo. */
    private class GuideView(context: Context) : View(context) {
        private val d = context.resources.displayMetrics.density
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5 * d }
        private val box = Paint().apply { color = Color.argb(0xB0, 0, 0, 0) }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 16 * d }
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            val st = app.canBus.steering.value?.eps?.takeIf { it != Int.MIN_VALUE } ?: 0
            val bend = st / 540f * 0.18f * w
            val bands = listOf(Color.RED to 0.82f, Color.YELLOW to 0.66f, Color.GREEN to 0.48f)
            var yFrom = h
            var xl = 0.22f * w; var xr = 0.78f * w
            for ((color, yTo) in bands) {
                val t = (h - yTo * h) / (h * 0.52f)          // 0 embaixo → 1 no topo das linhas
                val nxl = 0.22f * w + (0.38f - 0.22f) * w * t + bend * t * t
                val nxr = 0.78f * w - (0.78f - 0.62f) * w * t + bend * t * t
                line.color = color
                path.reset(); path.moveTo(xl, yFrom); path.lineTo(nxl, yTo * h); canvas.drawPath(path, line)
                path.reset(); path.moveTo(xr, yFrom); path.lineTo(nxr, yTo * h); canvas.drawPath(path, line)
                canvas.drawLine(nxl, yTo * h, nxl + 0.04f * w, yTo * h, line)
                canvas.drawLine(nxr, yTo * h, nxr - 0.04f * w, yTo * h, line)
                xl = nxl; xr = nxr; yFrom = yTo * h
            }
            val car = app.carService.state.value
            val rows = listOf(
                "TESTE DO LAUNCHER (sonda da re) - se aparece por cima da camera, da para desenhar linhas proprias",
                "re ICar=${car.reverseOn}  ultimo aviso=${lastBroadcast ?: "-"}",
                "volante: ${app.canBus.steering.value?.let { steerText(it) } ?: "sem dado"}",
                "sensores: ${app.canBus.radar.value?.let { radarText(it) } ?: "sem dado"}",
                "volume na re (param 15): ${audio?.value ?: "-"}",
            )
            val pad = 10 * d; val lh = 22 * d
            canvas.drawRect(0f, 0f, w, pad * 2 + lh * rows.size, box)
            rows.forEachIndexed { i, s -> canvas.drawText(s, pad, pad + lh * (i + 0.75f), text) }
        }
    }
}
