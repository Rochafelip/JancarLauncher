package com.rocha.jancarlauncher.launch

import android.content.Context
import android.content.Intent

/** Ações de Settings usadas pelos chips (strings literais = valores de `android.provider.Settings.ACTION_*`). */
object SettingsActions {
    const val WIFI = "android.settings.WIFI_SETTINGS"
    const val BLUETOOTH = "android.settings.BLUETOOTH_SETTINGS"
    const val LOCATION = "android.settings.LOCATION_SOURCE_SETTINGS"
    const val MAIN = "android.settings.SETTINGS"
}

interface SettingsStarter {
    /** Abre a tela de Settings da [action]. Pode lançar se a central não tiver essa tela. */
    fun start(action: String)
}

/** Tenta a tela específica; se não existir, cai nos Ajustes gerais; se nada abrir, devolve false (sem exceção). */
class SystemSettingsOpener(private val starter: SettingsStarter) {
    fun open(action: String): Boolean {
        for (candidate in listOf(action, SettingsActions.MAIN).distinct()) {
            try {
                starter.start(candidate)
                return true
            } catch (e: RuntimeException) {
                // tenta o próximo
            }
        }
        return false
    }
}

class ContextSettingsStarter(private val context: Context) : SettingsStarter {
    override fun start(action: String) {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
