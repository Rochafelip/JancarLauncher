package com.rocha.jancarlauncher.system

import android.content.Context
import android.provider.Settings
import java.io.File

/** Lê o estado real do ADB sem fio. Melhor esforço: arquivo ilegível nunca derruba o app. */
object AdbStatusReader {
    fun read(context: Context): AdbStatus {
        val enabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        if (!enabled) return AdbStatus.Disabled

        val remoteIp = runCatching {
            val lines = File("/proc/net/tcp").readLines()
            ProcNetTcpParser.findEstablishedRemoteIp(lines)
        }.getOrNull()

        return if (remoteIp != null) AdbStatus.EnabledConnected(remoteIp) else AdbStatus.Enabled
    }
}
