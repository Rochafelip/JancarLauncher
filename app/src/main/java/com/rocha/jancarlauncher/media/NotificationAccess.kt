package com.rocha.jancarlauncher.media

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/** Verifica se o usuário concedeu acesso a notificações ao launcher (exigido por [NowPlayingListenerService]). */
object NotificationAccess {
    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
