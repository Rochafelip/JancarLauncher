package com.rocha.jancarlauncher.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Faixa como o serviço de mídia da Jancar a entrega em `IMediaInfoCallback.onMediaChange` (campos crus). */
data class JancarMediaInfo(
    val mediaType: Int,
    val text1: String?,
    val text2: String?,
    val num1: Int,
    val num2: Int,
    val art: ByteArray?,
    val num3: Int,
    val num4: Int,
    val flag: Boolean,
) {
    override fun equals(other: Any?): Boolean =
        other is JancarMediaInfo && mediaType == other.mediaType && text1 == other.text1 && text2 == other.text2 &&
            num1 == other.num1 && num2 == other.num2 && art.contentEquals(other.art) && num3 == other.num3 &&
            num4 == other.num4 && flag == other.flag

    override fun hashCode(): Int =
        listOf(mediaType, text1, text2, num1, num2, art?.contentHashCode(), num3, num4, flag).hashCode()
}

/** Estado de reprodução cru de `IMediaInfoCallback.onPlayStateChange(int, int, int, int)`. */
data class JancarPlayState(val a: Int, val b: Int, val c: Int, val d: Int)

/**
 * Cliente do serviço de mídia da Jancar (`com.jancar.services/.media.MediaService`, interface
 * `com.jancar.services.media.IMedia`). O app de Música da Jancar publica a faixa ali (`setMediaInfo`), não numa
 * MediaSession padrão — por isso o "Tocando agora" via NotificationListener não a enxerga.
 *
 * Códigos de transação lidos dos campos `TRANSACTION_*` do `IMedia$Stub` / `IMediaInfoCallback$Stub` no
 * `ivi-services.vdex` da central (não inferidos). REGRA: só registro de callback, pedido de estado e transporte
 * (play/pause/próxima/anterior, os mesmos botões do app de Música) — nunca `setMediaInfo`/`setMediaState`/zonas/
 * arquivos, que alteram o estado que o próprio sistema publica.
 */
class JancarMediaClient(context: Context) {

    private val appContext = context.applicationContext
    private val worker = HandlerThread("JancarMediaClient").apply { start() }
    private val bg = Handler(worker.looper)
    private val main = Handler(Looper.getMainLooper())

    private val _info = MutableStateFlow<JancarMediaInfo?>(null)
    private val _playState = MutableStateFlow<JancarPlayState?>(null)
    private val _connected = MutableStateFlow(false)
    val info: StateFlow<JancarMediaInfo?> = _info.asStateFlow()
    val playState: StateFlow<JancarPlayState?> = _playState.asStateFlow()
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    // Capa convertida para PNG (o serviço manda pixels ARGB crus; o card decodifica PNG/JPEG).
    private val _artPng = MutableStateFlow<ByteArray?>(null)
    val artPng: StateFlow<ByteArray?> = _artPng.asStateFlow()
    @Volatile private var lastArtHash: Int? = null

    @Volatile private var remote: IBinder? = null
    @Volatile private var wantConnected = false

    private val callback = object : Binder() {
        init { attachInterface(null, CB_DESCRIPTOR) }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code !in 1..CB_LAST_CODE) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(CB_DESCRIPTOR)
            when (code) {
                CB_MEDIA_CHANGE -> {
                    val info = JancarMediaInfo(
                        mediaType = data.readInt(), text1 = data.readString(), text2 = data.readString(),
                        num1 = data.readInt(), num2 = data.readInt(), art = data.createByteArray(),
                        num3 = data.readInt(), num4 = data.readInt(), flag = data.readInt() != 0,
                    )
                    _info.value = info
                    val artHash = info.art?.contentHashCode()
                    if (artHash != lastArtHash) {
                        lastArtHash = artHash
                        bg.post { _artPng.value = toPng(info) }
                    }
                    if (DEBUG) {
                        Log.d(TAG, "onMediaChange type=${info.mediaType} t1=${info.text1} t2=${info.text2} " +
                            "n1=${info.num1} n2=${info.num2} art=${info.art?.size} n3=${info.num3} n4=${info.num4} " +
                            "flag=${info.flag}")
                    }
                }
                CB_PLAY_STATE -> {
                    val s = JancarPlayState(data.readInt(), data.readInt(), data.readInt(), data.readInt())
                    _playState.value = s
                    if (DEBUG) Log.d(TAG, "onPlayStateChange $s")
                }
                else -> if (DEBUG) Log.d(TAG, "callback code=$code ignorado")
            }
            reply?.writeNoException()
            return true
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            remote = service
            _connected.value = true
            if (DEBUG) Log.d(TAG, "bind ok: $name")
            bg.post {
                runCatching { call(T_REGISTER_INFO_CALLBACK) { it.writeStrongBinder(callback) }?.recycle() }
                    .onFailure { Log.w(TAG, "registerMediaInfoCallback falhou", it) }
                // Pede o estado atual: sem isso só chegaria algo na próxima troca de faixa.
                runCatching { call(T_REQUEST_INFO_AND_STATE)?.recycle() }
                    .onFailure { Log.w(TAG, "requestMediaInfoAndStateEvent falhou", it) }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.w(TAG, "serviço desconectou: $name")
            lost()
        }

        override fun onBindingDied(name: ComponentName) {
            Log.w(TAG, "binding morreu: $name")
            runCatching { appContext.unbindService(this) }
            lost()
        }
    }

    fun connect() {
        wantConnected = true
        val intent = Intent(ACTION).setComponent(ComponentName(PKG, SERVICE))
        val ok = runCatching { appContext.bindService(intent, conn, Context.BIND_AUTO_CREATE) }
            .onFailure { Log.w(TAG, "bindService falhou", it) }
            .getOrDefault(false)
        if (!ok) {
            Log.w(TAG, "bindService retornou false; tentando de novo em 3s")
            scheduleReconnect()
        }
    }

    fun disconnect() {
        wantConnected = false
        main.removeCallbacksAndMessages(null)
        val binder = remote
        if (binder != null) {
            bg.post {
                runCatching { call(T_UNREGISTER_INFO_CALLBACK, binder) { it.writeStrongBinder(callback) }?.recycle() }
            }
        }
        runCatching { appContext.unbindService(conn) }
        remote = null
        _connected.value = false
    }

    fun play() = command(T_PLAY)
    fun pause() = command(T_PAUSE)
    fun next() = command(T_NEXT)
    fun previous() = command(T_PREV)

    private fun command(code: Int) {
        bg.post { runCatching { call(code)?.recycle() }.onFailure { Log.w(TAG, "comando $code falhou", it) } }
    }

    /** Pixels ARGB crus (num1 × num2) → PNG; null se o tamanho não bate ou a conversão falhar. */
    private fun toPng(info: JancarMediaInfo): ByteArray? {
        val art = info.art
        if (art == null || !JancarNowPlaying.isRawArgb(art, info.num1, info.num2)) return null
        return runCatching {
            val bmp = Bitmap.createBitmap(info.num1, info.num2, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(ByteBuffer.wrap(art))
            ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it); bmp.recycle() }.toByteArray()
        }.onFailure { Log.w(TAG, "capa não convertida", it) }.getOrNull()
    }

    private fun lost() {
        remote = null
        _connected.value = false
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (!wantConnected) return
        main.postDelayed({ if (wantConnected && remote == null) connect() }, 3000)
    }

    /** Transação síncrona no IMedia. Só códigos de registro/pedido de estado (ver companion). */
    private fun call(code: Int, binder: IBinder? = remote, write: (Parcel) -> Unit = {}): Parcel? {
        if (binder == null) return null
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESCRIPTOR)
            write(data)
            binder.transact(code, data, reply, 0)
            reply.readException()
            reply
        } catch (t: Throwable) {
            reply.recycle(); throw t
        } finally {
            data.recycle()
        }
    }

    companion object {
        private const val TAG = "JancarMediaClient"
        private const val DEBUG = false // calibrado na central em 2026-11-07; ligar para depurar

        const val PKG = "com.jancar.services"
        const val SERVICE = "com.jancar.services.media.MediaService"
        const val ACTION = "com.jancar.services.action.media"

        private const val DESCRIPTOR = "com.jancar.services.media.IMedia"
        private const val CB_DESCRIPTOR = "com.jancar.services.media.IMediaInfoCallback"

        // IMedia$Stub.TRANSACTION_* (só leitura/registro). NÃO adicionar setMediaInfo(8)/setMediaState/zonas.
        private const val T_REGISTER_INFO_CALLBACK = 5
        private const val T_UNREGISTER_INFO_CALLBACK = 6
        private const val T_REQUEST_INFO_AND_STATE = 22
        private const val T_PLAY = 15
        private const val T_PAUSE = 16
        private const val T_PREV = 18
        private const val T_NEXT = 19

        // IMediaInfoCallback$Stub.TRANSACTION_*
        private const val CB_MEDIA_CHANGE = 1
        private const val CB_PLAY_STATE = 2
        private const val CB_LAST_CODE = 4
    }
}
