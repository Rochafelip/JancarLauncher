package com.rocha.jancarlauncher.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rocha.jancarlauncher.MainActivity

/** Abre a Home quando o sistema termina de iniciar. Na API 27 abrir Activity de segundo plano é permitido. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!isBootAction(intent.action)) return
        try {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: RuntimeException) {
            Log.w("JancarBoot", "não foi possível abrir a Home no boot", e)
        }
    }

    companion object {
        fun isBootAction(action: String?): Boolean = action == Intent.ACTION_BOOT_COMPLETED
    }
}
