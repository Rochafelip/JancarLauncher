package com.rocha.jancarlauncher

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.preferencesDataStore
import com.rocha.jancarlauncher.boot.CrashRestarter
import com.rocha.jancarlauncher.can.CanBusClient
import com.rocha.jancarlauncher.launch.LaunchController
import com.rocha.jancarlauncher.power.ScreenStateTracker
import com.rocha.jancarlauncher.power.SharedPrefsScreenOffStore
import com.rocha.jancarlauncher.power.WakeWelcomeTracker
import com.rocha.jancarlauncher.settings.SettingsStore

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class JancarApp : Application() {
    val settings: SettingsStore by lazy { SettingsStore(settingsDataStore) }
    val launchController: LaunchController by lazy { LaunchController.create(this) }

    val wakeTracker: WakeWelcomeTracker by lazy {
        WakeWelcomeTracker(SharedPrefsScreenOffStore(this), SystemClock::elapsedRealtime)
    }

    // Instância única por processo (fase de calibração do CAN bus, ver can/VehicleInfoCard.kt).
    val canBus: CanBusClient by lazy { CanBusClient(this) }

    override fun onCreate() {
        super.onCreate()
        CrashRestarter.install(this)
        ScreenStateTracker(this, wakeTracker).register()
    }
}
