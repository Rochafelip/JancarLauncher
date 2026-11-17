package com.rocha.jancarlauncher

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.preferencesDataStore
import com.rocha.jancarlauncher.boot.CrashRestarter
import com.rocha.jancarlauncher.can.CanBusClient
import com.rocha.jancarlauncher.can.JancarCarClient
import com.rocha.jancarlauncher.media.JancarMediaClient
import com.rocha.jancarlauncher.obd.DtcCatalog
import com.rocha.jancarlauncher.obd.HealthMonitor
import com.rocha.jancarlauncher.obd.ScanHistory
import com.rocha.jancarlauncher.obd.TripStore
import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.overlay.AutoOverlay
import com.rocha.jancarlauncher.launch.LaunchController
import com.rocha.jancarlauncher.power.ScreenStateTracker
import com.rocha.jancarlauncher.power.SharedPrefsScreenOffStore
import com.rocha.jancarlauncher.power.WakeWelcomeTracker
import com.rocha.jancarlauncher.settings.SettingsStore
import com.rocha.jancarlauncher.system.CrashStore
import com.rocha.jancarlauncher.system.RingLog
import com.rocha.jancarlauncher.system.SpeedAlarm
import java.io.File

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class JancarApp : Application() {
    /** Eventos importantes (OBD/CAN, atualização, avisos), para o relatório de erros. */
    val log = RingLog()

    /** Crashes gravados no aparelho, lidos em Ajustes › Relatório de erros. */
    val crashes: CrashStore by lazy { CrashStore(File(filesDir, "crash")) }

    val settings: SettingsStore by lazy { SettingsStore(settingsDataStore) }
    val launchController: LaunchController by lazy { LaunchController.create(this) }

    val wakeTracker: WakeWelcomeTracker by lazy {
        WakeWelcomeTracker(SharedPrefsScreenOffStore(this), SystemClock::elapsedRealtime)
    }

    // Instância única por processo (fase de calibração do CAN bus, ver can/VehicleInfoCard.kt).
    val canBus: CanBusClient by lazy { CanBusClient(this, log) }

    // Via principal (servico ICar da Jancar, somente leitura); instancia unica, ver can/CarStateCard.kt.
    val carService: JancarCarClient by lazy { JancarCarClient(this) }

    // Serviço de mídia da Jancar (faixa do app Música); instância única, conectada pelo AppRoot.
    val media: JancarMediaClient by lazy { JancarMediaClient(this, log) }

    // Leitor OBD-II (ELM327 Bluetooth): consumo para o Painel; ligado pelo AppRoot o tempo todo.
    val obd: ObdClient by lazy {
        ObdClient(this, settings, log).also { c -> c.onTripFinished = { tripStore.add(it) } }
    }

    /** Escopo do app (vida do processo) para monitores que não pertencem a uma tela. */
    val appScope = com.rocha.jancarlauncher.system.appScope(log)

    // Bateria e água pelo OBD (avisos do Painel, da tela Fox e do cartão sobre o AA).
    val health: HealthMonitor by lazy { HealthMonitor(obd.live, appScope, log) }

    // Aviso sonoro acima do limite de velocidade (CAN quando aberto, senão OBD).
    val speedAlarm: SpeedAlarm by lazy { SpeedAlarm(this) }

    // Viagens anteriores (uma por ligada do carro), gravadas quando o ObdClient fecha uma.
    val tripStore: TripStore by lazy { TripStore(File(filesDir, "obd")) }

    // Scanner de falhas: últimas leituras e a tabela própria de códigos em português.
    val scanHistory: ScanHistory by lazy { ScanHistory(File(filesDir, "obd")) }
    val dtcCatalog: DtcCatalog by lazy {
        DtcCatalog(runCatching { assets.open("obd/dtc_pt.json").bufferedReader().use { it.readText() } }.getOrDefault("{}"))
    }

    // Faixa do Painel por cima do Android Auto/CarPlay; ligada pelo AppRoot.
    val autoOverlay: AutoOverlay by lazy { AutoOverlay(this) }

    override fun onCreate() {
        super.onCreate()
        CrashRestarter.install(this)
        ScreenStateTracker(this, wakeTracker).register()
        speedAlarm.start()
    }
}
