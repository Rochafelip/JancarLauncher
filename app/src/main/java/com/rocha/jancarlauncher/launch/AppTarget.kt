package com.rocha.jancarlauncher.launch

enum class Kind { App, System }

/**
 * Destino de lançamento. Só tipos Kotlin/Java puros (sem ComponentName) para ser testável na JVM.
 * - [pkg] + [activityClass]: intent explícito (nunca cai no seletor de apps).
 * - só [pkg]: usa a intent de lançamento do pacote.
 * - ambos nulos: ainda não configurado (ver [unconfigured]).
 */
data class AppTarget(
    val id: String,
    val displayName: String,
    val kind: Kind,
    val pkg: String?,
    val activityClass: String?,
) {
    companion object {
        fun unconfigured(id: String, displayName: String, kind: Kind) = AppTarget(id, displayName, kind, null, null)
    }
}
