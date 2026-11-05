package com.rocha.jancarlauncher.power

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.rocha.jancarlauncher.MainActivity

/**
 * Liga os broadcasts de tela ao [WakeWelcomeTracker]: tela acendeu depois de ≥ 10 min apagada → pede a Boas-vindas.
 * SCREEN_ON/OFF só podem ser registrados dinamicamente. Se a central dormir sem esses eventos (broadcast próprio do
 * MCU), este gatilho precisa mudar; o `onLaunch` do tracker cobre o caso de o processo ter morrido na suspensão.
 */
class ScreenStateTracker(private val app: Application, private val tracker: WakeWelcomeTracker) {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> tracker.onScreenOff()
                Intent.ACTION_SCREEN_ON -> if (tracker.onScreenOn()) showWelcome()
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        app.registerReceiver(receiver, filter)
    }

    private fun showWelcome() {
        try {
            app.startActivity(
                Intent(app, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(MainActivity.EXTRA_SHOW_WELCOME, true),
            )
        } catch (e: RuntimeException) {
            Log.w("JancarWake", "não foi possível abrir a Boas-vindas", e)
        }
    }
}
