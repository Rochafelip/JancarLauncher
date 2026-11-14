package com.rocha.jancarlauncher.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.rocha.jancarlauncher.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * Leitor OBD-II pelo adaptador ELM327 Bluetooth pareado com a central (pedido do dono, 2026-11-13: média
 * instantânea e média da viagem no Painel). **Somente leitura**, só PIDs padrão ([ElmProtocol]).
 *
 * Fica conectado sempre que o launcher roda (escolha do dono: a média da viagem conta tudo até zerar). Enquanto
 * isso, outro app de OBD não consegue usar o adaptador (o ELM327 aceita uma conexão só).
 */
class ObdClient(context: Context, private val settings: SettingsStore) {
    enum class Status { Off, NoAdapter, Connecting, Connected, NoFuelData }

    data class Live(
        val status: Status = Status.Off,
        val adapter: String? = null,
        val speedKmh: Float? = null,
        val litersPerHour: Float? = null,
        val kmPerLiter: Float? = null,
        /** % de etanol informado pelo carro (PID 0x52); `null` = o carro não informa (vale a escolha do Painel). */
        val ethanolPercent: Float? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var blend = FuelModel.Blend.Gasolina

    private val _live = MutableStateFlow(Live())
    val live: StateFlow<Live> = _live
    private val _trip = MutableStateFlow(Trip())
    val trip: StateFlow<Trip> = _trip

    /** Chamado no thread principal pelo AppRoot. */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            val (l, k) = settings.trip.first()
            _trip.value = Trip(l, k)
            launch { settings.fuelBlend.collect { blend = FuelModel.Blend.fromName(it) } }
            loop()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        closeSocket()
        persistTrip()
        _live.value = Live()
    }

    fun resetTrip() {
        _trip.value = Trip()
        persistTrip()
    }

    private fun persistTrip() {
        val t = _trip.value
        scope.launch { settings.setTrip(t.liters, t.km) }
    }

    private suspend fun loop() {
        while (coroutineContext.isActive) {
            val device = findAdapter()
            if (device == null) {
                _live.value = Live(Status.NoAdapter)
                delay(30_000)
                continue
            }
            val name = deviceName(device)
            _live.value = Live(Status.Connecting, name)
            try {
                session(device, name)
            } catch (e: Exception) {
                Log.w(TAG, "OBD: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                closeSocket()
                persistTrip()
            }
            _live.value = Live(Status.Connecting, name)
            delay(10_000)
        }
    }

    /** ELM327 pareado: pelo nome que esses adaptadores costumam ter. */
    @SuppressLint("MissingPermission")
    private fun findAdapter(): BluetoothDevice? = try {
        @Suppress("DEPRECATION")
        val bt = BluetoothAdapter.getDefaultAdapter()
        if (bt == null || !bt.isEnabled) null
        else bt.bondedDevices?.firstOrNull { d -> deviceName(d)?.let { OBD_NAME.containsMatchIn(it) } == true }
    } catch (e: SecurityException) {
        Log.w(TAG, "OBD: sem permissão de Bluetooth: ${e.message}")
        null
    }

    @SuppressLint("MissingPermission")
    private fun deviceName(d: BluetoothDevice): String? = try { d.name } catch (_: SecurityException) { null }

    @SuppressLint("MissingPermission")
    private suspend fun session(device: BluetoothDevice, name: String?) {
        val s = device.createRfcommSocketToServiceRecord(SPP)
        socket = s
        @Suppress("DEPRECATION")
        runCatching { BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() }
        s.connect()
        val io = ElmIo(s.inputStream, s.outputStream)
        for (cmd in ElmProtocol.INIT) io.send(cmd, if (cmd == "ATZ") 2_500 else 1_200)

        // Só usa o que o carro diz que tem (0100 / 0120 / 0140).
        val supported = mutableSetOf<Int>()
        for (base in listOf(ElmProtocol.PIDS_01_20, ElmProtocol.PIDS_21_40, ElmProtocol.PIDS_41_60)) {
            if (base != 0 && base !in supported) break
            ElmProtocol.parse(io.send(ElmProtocol.request(base), 6_000), base, 4)
                ?.let { supported += ElmProtocol.supported(it, base) }
        }
        Log.i(TAG, "OBD conectado em $name; PIDs: ${supported.sorted().joinToString { "%02X".format(it) }}")
        val useMaf = ElmProtocol.MAF in supported
        val useMap = !useMaf && listOf(ElmProtocol.MAP, ElmProtocol.IAT, ElmProtocol.RPM).all { it in supported }
        val hasEthanol = ElmProtocol.ETHANOL in supported

        var last = SystemClock.elapsedRealtime()
        var lastPersist = last
        var lastEthanolRead = 0L
        var ethanol: Float? = null
        var misses = 0
        while (coroutineContext.isActive) {
            val speed = io.read(ElmProtocol.SPEED, 1)?.let(ElmProtocol::speed)
            val air = when {
                useMaf -> io.read(ElmProtocol.MAF, 2)?.let(ElmProtocol::mafGramsPerSecond)
                useMap -> {
                    val map = io.read(ElmProtocol.MAP, 1)?.let(ElmProtocol::manifoldPressure)
                    val iat = io.read(ElmProtocol.IAT, 1)?.let(ElmProtocol::intakeTemp)
                    val rpm = io.read(ElmProtocol.RPM, 2)?.let(ElmProtocol::rpm)
                    if (map != null && iat != null && rpm != null) FuelModel.airFromManifold(map, iat, rpm) else null
                }
                else -> null
            }
            val now = SystemClock.elapsedRealtime()
            if (hasEthanol && now - lastEthanolRead > 30_000) {
                ethanol = io.read(ElmProtocol.ETHANOL, 1)?.let(ElmProtocol::ethanolPercent) ?: ethanol
                lastEthanolRead = now
            }
            // Sem resposta seguida (ignição desligada, adaptador travado): cai para reconectar.
            misses = if (speed == null) misses + 1 else 0
            if (misses >= 8) throw IllegalStateException("sem resposta do carro")

            val lph = air?.let { FuelModel.litersPerHour(it, ethanol?.div(100f) ?: blend.ethanol) }
            val dt = (now - last) / 1000.0
            last = now
            if (lph != null && speed != null) _trip.value = _trip.value.add(lph, speed, dt)
            _live.value = Live(
                status = if (useMaf || useMap) Status.Connected else Status.NoFuelData,
                adapter = name,
                speedKmh = speed,
                litersPerHour = lph,
                kmPerLiter = if (lph != null && speed != null) FuelModel.kmPerLiter(speed, lph) else null,
                ethanolPercent = ethanol,
            )
            if (now - lastPersist > 15_000) { persistTrip(); lastPersist = now }
            delay(250)
        }
    }

    private fun closeSocket() {
        runCatching { socket?.close() }
        socket = null
    }

    /** Envia um comando ao ELM e lê até o prompt '>' (ou o tempo acabar). */
    private class ElmIo(private val input: InputStream, private val output: OutputStream) {
        fun send(cmd: String, timeoutMs: Long = 1_500): String {
            while (input.available() > 0) input.read() // descarta sobra de resposta anterior
            output.write("$cmd\r".toByteArray())
            output.flush()
            val sb = StringBuilder()
            val end = SystemClock.elapsedRealtime() + timeoutMs
            while (SystemClock.elapsedRealtime() < end) {
                if (input.available() > 0) {
                    val c = input.read()
                    if (c < 0) break
                    if (c.toChar() == '>') break
                    sb.append(c.toChar())
                } else {
                    Thread.sleep(10)
                }
            }
            return sb.toString()
        }

        fun read(pid: Int, bytes: Int): IntArray? = ElmProtocol.parse(send(ElmProtocol.request(pid)), pid, bytes)
    }

    companion object {
        private const val TAG = "ObdClient"
        private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        /** Nomes comuns de adaptadores ELM327 (o do dono é Bluetooth, pareado com a central). */
        private val OBD_NAME = Regex("OBD|ELM|V-?LINK|VEEPEAK|KONNWEI|VGATE|ICAR|CAR SCANNER", RegexOption.IGNORE_CASE)
    }
}
