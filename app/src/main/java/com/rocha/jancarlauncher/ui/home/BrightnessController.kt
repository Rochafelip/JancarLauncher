package com.rocha.jancarlauncher.ui.home

import android.app.Activity
import android.provider.Settings

/**
 * Aplica o brilho escolhido. A janela do launcher muda sempre (efeito imediato, vale enquanto ele está na
 * frente); a configuração do sistema só muda se o app tiver WRITE_SETTINGS (concedida por `appops` via ADB).
 */
class BrightnessController(private val activity: Activity) {
    fun apply(percent: Int) {
        val attrs = activity.window.attributes
        attrs.screenBrightness = BrightnessMapper.toWindow(percent)
        activity.window.attributes = attrs
        if (Settings.System.canWrite(activity)) {
            try {
                Settings.System.putInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, BrightnessMapper.toSystem(percent))
            } catch (e: SecurityException) {
                // permissão revogada entre o canWrite e o putInt: a janela já foi ajustada
            }
        }
    }

    /** Brilho atual do sistema em %, usado como posição inicial do slider quando o usuário nunca escolheu. */
    fun currentSystemPercent(): Int = try {
        BrightnessMapper.fromSystem(Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
    } catch (e: Settings.SettingNotFoundException) {
        BrightnessMapper.MAX / 2
    }
}
