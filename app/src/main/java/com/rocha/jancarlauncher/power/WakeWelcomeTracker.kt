package com.rocha.jancarlauncher.power

import android.content.Context
import androidx.core.content.edit
import com.rocha.jancarlauncher.settings.WelcomePolicy

/** Guarda quando a tela apagou. Em disco: o processo do launcher pode morrer durante a suspensão. */
interface ScreenOffStore {
    fun read(): Long?
    fun write(ms: Long?)
}

/**
 * Decide se a Boas-vindas aparece ao "ligar o carro" (tela apagada por ≥ [WelcomePolicy.WAKE_THRESHOLD_MS]).
 * O relógio é `elapsedRealtime` (conta o tempo de suspensão; recomeça no reboot — um registro "do futuro" vira falso).
 * O registro é consumido na primeira decisão, então a Boas-vindas não repete.
 */
class WakeWelcomeTracker(private val store: ScreenOffStore, private val now: () -> Long) {
    fun onScreenOff() = store.write(now())

    /** SCREEN_ON chegou com o processo vivo. */
    fun onScreenOn(): Boolean = consume()

    /** O app abriu (ex.: o sistema reiniciou a Home depois de matar o processo) — o SCREEN_ON já passou. */
    fun onLaunch(): Boolean = consume()

    private fun consume(): Boolean {
        val off = store.read()
        store.write(null)
        return WelcomePolicy.shouldShowOnWake(off, now())
    }
}

class SharedPrefsScreenOffStore(context: Context) : ScreenOffStore {
    private val prefs = context.applicationContext.getSharedPreferences("screen_state", Context.MODE_PRIVATE)

    override fun read(): Long? = if (prefs.contains(KEY)) prefs.getLong(KEY, 0L) else null

    override fun write(ms: Long?) {
        prefs.edit {
            if (ms == null) remove(KEY) else putLong(KEY, ms)
        }
    }

    private companion object { const val KEY = "screen_off_at" }
}
