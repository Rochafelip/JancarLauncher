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

data class DashBoard(
    val gearShiftPosition: Int, val odo: Int, val cruisingRange: Int, val rotationRate: Int,
    val speed: Int, val runningTime: Int, val fuelTankage: Int,
    val fuel: Float, val fuelPercentage: Float, val avgFuelCont: Float, val instFuelCont: Float,
    val coolantTemp: Float, val engineOilTemp: Float, val inletTemp: Float, val ambientTemp: Float,
    val accPedal: Int, val brakePedal: Int, val throttlePos: Int,
)

data class Cabin(
    val bonnetStatus: Int, val flDoorStatus: Int, val frDoorStatus: Int, val rlDoorStatus: Int,
    val rrDoorStatus: Int, val trunkStatus: Int, val electricTrunk: Int, val electricTrunkDirection: Int,
    val leftSafetyBelt: Int, val rightSafetyBelt: Int, val sunroofStatus: Int, val sunroofSunshadeStatus: Int,
    val flWindowStatus: Int, val frWindowStatus: Int, val rlWindowStatus: Int, val rrWindowStatus: Int,
    val lockCarStatus: Int, val seatUpDownMoveStatus: Int, val seatFrontRearMoveStatus: Int,
    val seatHighLowMoveStatus: Int, val seatBackUpDownMoveStatus: Int, val seatBackFrontRearMoveStatus: Int,
)

data class Lights(
    val leftDirectionIndicator: Int, val rightDirectionIndicator: Int, val emergencyFlasher: Int,
)

/**
 * Cliente do serviço CAN bus da central (`com.can.activity/com.can.ui.CanPopWind`), via Binder.transact
 * direto (sem AIDL gerado). Instância única por processo -- ver [com.rocha.jancarlauncher.JancarApp.canBus].
 *
 * ATENÇÃO: a ORDEM dos campos no Parcel foi INFERIDA do bytecode (engenharia reversa do APK de sistema),
 * NÃO validada com dados reais. Calibrar com `adb logcat -s CanBusClient` + o carro ligado antes de confiar
 * nos valores (ver `restante` no log: se != 0 em algum Parcel, a ordem/tamanho dos campos está errada).
 */
class CanBusClient(context: Context) {

    private val appContext = context.applicationContext
    private val worker = HandlerThread("CanBusClient").apply { start() }
    private val bg = Handler(worker.looper)
    private val main = Handler(Looper.getMainLooper())

    private val _dashboard = MutableStateFlow<DashBoard?>(null)
    private val _cabin = MutableStateFlow<Cabin?>(null)
    private val _lights = MutableStateFlow<Lights?>(null)
    private val _connected = MutableStateFlow(false)

    val dashboard: StateFlow<DashBoard?> = _dashboard.asStateFlow()
    val cabin: StateFlow<Cabin?> = _cabin.asStateFlow()
    val lights: StateFlow<Lights?> = _lights.asStateFlow()
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    @Volatile private var remote: IBinder? = null
    @Volatile private var wantConnected = false

    private val listener = object : Binder() {
        init { attachInterface(null, LISTENER) }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            when (code) {
                L_DASHBOARD, L_CABIN, L_LIGHT -> {
                    data.enforceInterface(LISTENER)
                    if (data.readInt() != 0) {
                        when (code) {
                            L_DASHBOARD -> _dashboard.value = readDashboard(data)
                            L_CABIN -> _cabin.value = readCabin(data)
                            L_LIGHT -> _lights.value = readLights(data)
                        }
                    }
                    reply?.writeNoException()
                    return true
                }
            }
            return super.onTransact(code, data, reply, flags)
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            bg.post {
                // O onBind devolve um ICanUI ("porteiro"); o ICanBus vem de getCanService("CanBusManager").
                val canBus = runCatching { resolveCanBus(service) }
                    .onFailure { Log.w(TAG, "getCanService falhou", it) }
                    .getOrNull()
                if (canBus == null) {
                    Log.w(TAG, "ICanBus indisponivel; tentando de novo em 3s")
                    scheduleReconnect()
                    return@post
                }
                remote = canBus
                _connected.value = true
                runCatching { transact(T_REGISTER_LISTENER) { it.writeStrongBinder(listener) }?.recycle() }
                    .onFailure { Log.w(TAG, "registerListener falhou", it) }
                refresh()
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            remote = null; _connected.value = false; scheduleReconnect()
        }
        override fun onBindingDied(name: ComponentName) {
            remote = null; _connected.value = false; scheduleReconnect()
        }
    }

    fun connect() {
        wantConnected = true
        val intent = Intent(ACTION).setComponent(ComponentName(PKG, SERVICE))
        val ok = runCatching { appContext.bindService(intent, conn, Context.BIND_AUTO_CREATE) }
            .getOrDefault(false)
        if (!ok) scheduleReconnect()
    }

    fun disconnect() {
        wantConnected = false
        main.removeCallbacksAndMessages(null)
        bg.post { runCatching { transact(T_UNREGISTER_LISTENER) { it.writeStrongBinder(listener) }?.recycle() } }
        runCatching { appContext.unbindService(conn) }
        remote = null
        _connected.value = false
    }

    fun refresh() {
        bg.post {
            runCatching { transact(T_GET_DASHBOARD) {}?.consume { _dashboard.value = readNullableDash(it) } }
                .onFailure { Log.w(TAG, "getDashBoardInfo falhou", it) }
            runCatching { transact(T_GET_CABIN) {}?.consume { _cabin.value = readNullableCabin(it) } }
                .onFailure { Log.w(TAG, "getCabinInfo falhou", it) }
            runCatching { transact(T_GET_LIGHT) {}?.consume { _lights.value = readNullableLights(it) } }
                .onFailure { Log.w(TAG, "getLightInfo falhou", it) }
        }
    }

    /** ICanUI.getCanService(String): IBinder (codigo 1), conferido no com.can.activity descompilado. */
    private fun resolveCanBus(ui: IBinder): IBinder? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(UI_DESCRIPTOR)
            data.writeString(CANBUS_SERVICE_NAME)
            ui.transact(T_GET_CAN_SERVICE, data, reply, 0)
            reply.readException()
            return reply.readStrongBinder()
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    private fun scheduleReconnect() {
        if (!wantConnected) return
        main.postDelayed({ if (wantConnected && remote == null) connect() }, 3000)
    }

    private fun transact(code: Int, write: (Parcel) -> Unit): Parcel? {
        val binder = remote ?: return null
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESCRIPTOR)
            write(data)
            binder.transact(code, data, reply, 0)
            reply.readException()
            reply // fica com o chamador, que recicla via consume {} ou .recycle()
        } catch (t: Throwable) {
            reply.recycle()
            throw t
        } finally {
            data.recycle()
        }
    }

    private inline fun <T> Parcel.consume(block: (Parcel) -> T): T =
        try { block(this) } finally { recycle() }

    private fun readNullableDash(p: Parcel) = if (p.readInt() != 0) readDashboard(p) else null
    private fun readNullableCabin(p: Parcel) = if (p.readInt() != 0) readCabin(p) else null
    private fun readNullableLights(p: Parcel) = if (p.readInt() != 0) readLights(p) else null

    // Ordem inferida do writeToParcel (NÃO validada com dados reais)
    private fun readDashboard(p: Parcel): DashBoard {
        val d = DashBoard(
            gearShiftPosition = p.readInt(), odo = p.readInt(), cruisingRange = p.readInt(),
            rotationRate = p.readInt(), speed = p.readInt(), runningTime = p.readInt(),
            fuelTankage = p.readInt(),
            fuel = p.readFloat(), fuelPercentage = p.readFloat(), avgFuelCont = p.readFloat(),
            instFuelCont = p.readFloat(), coolantTemp = p.readFloat(), engineOilTemp = p.readFloat(),
            inletTemp = p.readFloat(), ambientTemp = p.readFloat(),
            accPedal = p.readInt(), brakePedal = p.readInt(), throttlePos = p.readInt(),
        )
        if (DEBUG) Log.d(TAG, "dash=$d restante=${p.dataAvail()}")
        return d
    }

    private fun readCabin(p: Parcel): Cabin {
        val c = Cabin(
            bonnetStatus = p.readInt(), flDoorStatus = p.readInt(), frDoorStatus = p.readInt(),
            rlDoorStatus = p.readInt(), rrDoorStatus = p.readInt(), trunkStatus = p.readInt(),
            electricTrunk = p.readInt(), electricTrunkDirection = p.readInt(),
            leftSafetyBelt = p.readInt(), rightSafetyBelt = p.readInt(),
            sunroofStatus = p.readInt(), sunroofSunshadeStatus = p.readInt(),
            flWindowStatus = p.readInt(), frWindowStatus = p.readInt(),
            rlWindowStatus = p.readInt(), rrWindowStatus = p.readInt(),
            lockCarStatus = p.readInt(),
            seatUpDownMoveStatus = p.readInt(), seatFrontRearMoveStatus = p.readInt(),
            seatHighLowMoveStatus = p.readInt(), seatBackUpDownMoveStatus = p.readInt(),
            seatBackFrontRearMoveStatus = p.readInt(),
        )
        if (DEBUG) Log.d(TAG, "cabin=$c restante=${p.dataAvail()}")
        return c
    }

    private fun readLights(p: Parcel): Lights {
        val l = Lights(
            leftDirectionIndicator = p.readInt(), rightDirectionIndicator = p.readInt(),
            emergencyFlasher = p.readInt(),
        )
        if (DEBUG) Log.d(TAG, "lights=$l restante=${p.dataAvail()}")
        return l
    }

    companion object {
        private const val TAG = "CanBusClient"
        private const val DEBUG = true // desligar depois de calibrar

        const val PKG = "com.can.activity"
        const val SERVICE = "com.can.ui.CanPopWind"
        const val ACTION = "autoai.intent.action.CANBUS_SERVICE"

        private const val UI_DESCRIPTOR = "com.autoai.canbus.ICanUI"
        private const val T_GET_CAN_SERVICE = 1
        private const val CANBUS_SERVICE_NAME = "CanBusManager"

        private const val DESCRIPTOR = "com.autoai.canbus.ICanBus"
        private const val LISTENER = "com.autoai.canbus.ICanBusListener"

        private const val T_REGISTER_LISTENER = 1
        private const val T_UNREGISTER_LISTENER = 2
        private const val T_GET_CABIN = 14
        private const val T_GET_LIGHT = 17
        private const val T_GET_DASHBOARD = 18

        private const val L_CABIN = 2
        private const val L_LIGHT = 5
        private const val L_DASHBOARD = 6
    }
}
