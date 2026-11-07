package com.rocha.jancarlauncher.can

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Cliente SOMENTE LEITURA do servico de carro da Jancar:
 *   com.jancar.services / com.jancar.services.car.CarService
 *   action com.jancar.services.action.car, interface com.jancar.services.car.ICar
 * REGRA: so leitura e registro de callback. NAO chamar set*, upgrade* ou send* do ICar
 * (sendKeyToMcu, upgradeMcu, setCarSetting, ...): podem alterar ou danificar o hardware.
 *
 * Via principal para velocidade/RPM/bateria/combustivel/freio de mao; [CanBusClient] e a secundaria.
 * Instancia unica por processo: use [com.rocha.jancarlauncher.JancarApp.carService].
 */

data class CarState(
    val speed: Float? = null,            // unidade a confirmar (provavel km/h)
    val rpm: Float? = null,
    val batteryVoltage: Float? = null,   // volts
    val fuelLiters: Float? = null,
    val fuelRangeKm: Float? = null,      // unidade a confirmar
    val outsideTempRaw: Int? = null,     // valor cru (255 = desconhecido); em °C: [outsideTempC]
    val handbrakeOn: Boolean? = null,    // HOLD=1
    val accOn: Boolean? = null,
    val doorMask: Int? = null,           // bit por indice de porta (a confirmar)
    val lightMask: Int? = null,          // bit por indice de luz (a confirmar)
    val powerStatus: Int? = null,        // POWER_ON=1, POWER_OFF=2, ...
    val headlightOn: Boolean? = null,    // fio de iluminação do chicote (GPIO); validado no Fox
) {
    /** Temperatura externa em °C, decodificada de [outsideTempRaw] (ver [decodeOutsideTempC]). */
    val outsideTempC: Float? get() = decodeOutsideTempC(outsideTempRaw)

    /** FRONT_LEFT=0, FRONT_RIGHT=1, REAR_LEFT=2, REAR_RIGHT=3, HOOD=4, FUEL_CAP=5, TRUNK=6 */
    fun isDoorOpen(index: Int): Boolean? = doorMask?.let { (it shr index) and 1 == 1 }

    /** TURN_LEFT=0, TURN_RIGHT=1, MARKER=2, EMERGENCY=3, FRONT_FOG=4, REAR_FOG=5, HIGH_BEAM=6, LOW_BEAM=7 */
    fun isLightOn(index: Int): Boolean? = lightMask?.let { (it shr index) and 1 == 1 }
}

class JancarCarClient(context: Context) {

    private val appContext = context.applicationContext
    private val worker = HandlerThread("JancarCarClient").apply { start() }
    private val bg = Handler(worker.looper)
    private val main = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(CarState())
    private val _connected = MutableStateFlow(false)
    val state: StateFlow<CarState> = _state.asStateFlow()
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    @Volatile private var remote: IBinder? = null
    @Volatile private var wantConnected = false

    private val callback = object : Binder() {
        init { attachInterface(null, CB_DESCRIPTOR) }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code !in 1..CB_LAST_CODE) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(CB_DESCRIPTOR)
            when (code) {
                CB_REALTIME -> {                       // onRealTimeInfoChanged(int id, float value)
                    val id = data.readInt(); val v = data.readFloat()
                    _state.update {
                        when (id) {
                            RT_SPEED -> it.copy(speed = v.known())
                            RT_RPM -> it.copy(rpm = v.known())
                            else -> it
                        }
                    }
                }
                CB_EXTRA -> {                          // onExtraStateChanged(int id, float value)
                    val id = data.readInt(); val v = data.readFloat()
                    _state.update {
                        when (id) {
                            EX_BATTERY -> it.copy(batteryVoltage = v.known())
                            EX_FUEL -> it.copy(fuelLiters = v.known())
                            EX_RANGE -> it.copy(fuelRangeKm = v.known())
                            else -> it
                        }
                    }
                }
                CB_ACC, CB_HANDBRAKE, CB_DOOR, CB_LIGHT, CB_HEADLIGHT, CB_OUTSIDE_TEMP, CB_POWER ->
                    bg.post { refreshSnapshot() }
                else -> Unit
            }
            reply?.writeNoException()
            return true
        }
    }

    private val pollTick = object : Runnable {
        override fun run() {
            if (wantConnected && remote != null) {
                refreshSnapshot()
                bg.postDelayed(this, POLL_MS)
            }
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            remote = service
            _connected.value = true
            if (DEBUG) Log.d(TAG, "bind ok: $name")
            bg.post {
                runCatching { call(T_REGISTER_CALLBACK) { it.writeStrongBinder(callback) }?.recycle() }
                    .onFailure { Log.w(TAG, "registerCallback falhou", it) }
                for (id in intArrayOf(RT_SPEED, RT_RPM)) {
                    runCatching {
                        call(T_REGISTER_REALTIME) { it.writeInt(id); it.writeStrongBinder(callback) }?.recycle()
                    }.onFailure { Log.w(TAG, "registerRealTimeInfo($id) falhou", it) }
                }
                // Reconexao rapida: nao deixa dois loops de polling rodando.
                bg.removeCallbacks(pollTick)
                pollTick.run()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.w(TAG, "servico desconectou: $name")
            lost()
        }

        override fun onBindingDied(name: ComponentName) {
            Log.w(TAG, "binding morreu: $name")
            // Depois de onBindingDied o binding nao volta sozinho: precisa desfazer e refazer.
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
        bg.removeCallbacksAndMessages(null)
        // Captura o binder antes de zerar `remote`, senao o desregistro abaixo vira no-op.
        val binder = remote
        if (binder != null) {
            bg.post {
                runCatching { call(T_UNREGISTER_CALLBACK, binder) { it.writeStrongBinder(callback) }?.recycle() }
                for (id in intArrayOf(RT_SPEED, RT_RPM)) {
                    runCatching {
                        call(T_UNREGISTER_REALTIME, binder) { it.writeInt(id); it.writeStrongBinder(callback) }?.recycle()
                    }
                }
            }
        }
        runCatching { appContext.unbindService(conn) }
        remote = null
        _connected.value = false
        _state.value = CarState() // sem conexão = desconhecido (o modo noturno volta ao horário)
    }

    private fun lost() {
        remote = null
        _connected.value = false
        _state.value = CarState()
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (!wantConnected) return
        main.postDelayed({ if (wantConnected && remote == null) connect() }, 3000)
    }

    private fun refreshSnapshot() {
        runCatching {
            val speed = readFloat(T_GET_REALTIME) { it.writeInt(RT_SPEED) }
            val rpm = readFloat(T_GET_REALTIME) { it.writeInt(RT_RPM) }
            val batt = readFloat(T_GET_EXTRA) { it.writeInt(EX_BATTERY) }
            val fuel = readFloat(T_GET_EXTRA) { it.writeInt(EX_FUEL) }
            val range = readFloat(T_GET_EXTRA) { it.writeInt(EX_RANGE) }
            val hb = readInt(T_GET_HANDBRAKE)
            val acc = readInt(T_IS_ACC_ON)
            val doors = readInt(T_GET_DOOR_MASK)
            val lights = readInt(T_GET_LIGHT_MASK)
            val temp = readInt(T_GET_OUTSIDE_TEMP)
            val power = readInt(T_GET_POWER)
            val head = readInt(T_GET_HEADLIGHT) // boolean no AIDL = int 0/1
            val next = CarState(
                speed = speed?.known(), rpm = rpm?.known(), batteryVoltage = batt?.known(),
                fuelLiters = fuel?.known(), fuelRangeKm = range?.known(),
                outsideTempRaw = temp?.takeIf { it != 255 },
                handbrakeOn = hb?.let { if (it < 0) null else it == HANDBRAKE_HOLD },
                accOn = acc?.let { it != 0 }, doorMask = doors, lightMask = lights, powerStatus = power,
                headlightOn = head?.let { it != 0 },
            )
            val changed = next != _state.value
            _state.value = next
            // A conexão fica aberta o tempo todo (farol -> modo noturno): só loga quando algo muda.
            if (DEBUG && changed) {
                // Valores crus junto, pra calibrar escala/unidade (ex.: temp externa, mascaras).
                Log.d(
                    TAG,
                    "state=$next raw[speed=$speed rpm=$rpm batt=$batt fuel=$fuel range=$range " +
                        "hb=$hb acc=$acc doors=$doors lights=$lights temp=$temp(${next.outsideTempC}°C) " +
                        "power=$power head=$head]",
                )
            }
        }.onFailure { Log.w(TAG, "refreshSnapshot falhou", it) }
    }

    /** Transacao sincrona no ICar. So use com codigos de leitura/registro (ver companion). */
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

    private fun readFloat(code: Int, write: (Parcel) -> Unit = {}): Float? =
        call(code, write = write)?.let { try { it.readFloat() } finally { it.recycle() } }

    private fun readInt(code: Int): Int? =
        call(code)?.let { try { it.readInt() } finally { it.recycle() } }

    private fun Float.known(): Float? = takeIf { it >= 0f && !it.isNaN() }

    companion object {
        private const val TAG = "JancarCarClient"
        private const val DEBUG = true        // desligar depois de calibrar
        private const val POLL_MS = 1000L

        const val PKG = "com.jancar.services"
        const val SERVICE = "com.jancar.services.car.CarService"
        const val ACTION = "com.jancar.services.action.car"

        private const val DESCRIPTOR = "com.jancar.services.car.ICar"
        private const val CB_DESCRIPTOR = "com.jancar.services.car.ICarCallback"

        // ICar$Stub (somente leitura + registro de callback). NAO adicionar codigos de set*/upgrade*/send*.
        private const val T_REGISTER_CALLBACK = 3
        private const val T_UNREGISTER_CALLBACK = 4
        private const val T_REGISTER_REALTIME = 5
        private const val T_UNREGISTER_REALTIME = 6
        private const val T_GET_REALTIME = 9
        private const val T_GET_HANDBRAKE = 11
        private const val T_GET_DOOR_MASK = 12
        private const val T_GET_LIGHT_MASK = 13
        private const val T_GET_OUTSIDE_TEMP = 24
        private const val T_GET_EXTRA = 26
        private const val T_IS_ACC_ON = 52
        private const val T_GET_POWER = 65
        private const val T_GET_HEADLIGHT = 14 // getHeadLightStatus(): boolean

        private const val RT_SPEED = 1
        private const val RT_RPM = 3
        private const val EX_BATTERY = 0
        private const val EX_FUEL = 1
        private const val EX_RANGE = 2
        private const val HANDBRAKE_HOLD = 1

        // ICarCallback$Stub
        private const val CB_ACC = 2
        private const val CB_HANDBRAKE = 4
        private const val CB_DOOR = 5
        private const val CB_LIGHT = 6
        private const val CB_OUTSIDE_TEMP = 9
        private const val CB_REALTIME = 13
        private const val CB_EXTRA = 14
        private const val CB_POWER = 32
        private const val CB_HEADLIGHT = 7 // onHeadLightChanged(boolean)
        private const val CB_LAST_CODE = 33
    }
}
