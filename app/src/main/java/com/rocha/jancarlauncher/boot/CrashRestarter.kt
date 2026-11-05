package com.rocha.jancarlauncher.boot

import android.app.Application
import android.content.Intent
import android.util.Log
import com.rocha.jancarlauncher.MainActivity

/**
 * Se o launcher cair, registra e reabre a Home (no máximo 3 vezes por minuto, ver [RestartPolicy]).
 * O handler anterior continua sendo chamado para o processo terminar normalmente.
 */
object CrashRestarter {
    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val policy = RestartPolicy(SharedPrefsRestartLog(app))
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                Log.e("JancarCrash", "crash em ${thread.name}", error)
                if (policy.allow(System.currentTimeMillis())) {
                    Log.w("JancarCrash", "reinício automático autorizado")
                    app.startActivity(
                        Intent(app, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    )
                } else {
                    Log.w("JancarCrash", "reinício automático NEGADO (limite de 3 por minuto)")
                }
            } catch (e: RuntimeException) {
                // nunca deixar o tratamento do crash gerar outro crash
            } finally {
                previous?.uncaughtException(thread, error)
            }
        }
    }
}
