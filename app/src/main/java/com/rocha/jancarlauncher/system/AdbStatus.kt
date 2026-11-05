package com.rocha.jancarlauncher.system

/** Estado do ADB sem fio, exibido como lembrete de segurança na Home (porta sem senha). */
sealed class AdbStatus {
    object Disabled : AdbStatus()
    object Enabled : AdbStatus()
    data class EnabledConnected(val remoteIp: String?) : AdbStatus()
}
