package com.rocha.jancarlauncher.launch

/** Texto curto (pt-BR) para o toast; null quando não há nada a dizer. */
fun LaunchResult.userMessage(target: AppTarget): String? = when (this) {
    LaunchResult.Started -> null
    LaunchResult.NotInstalled -> "${target.displayName} não está instalado"
    LaunchResult.Disabled -> "${target.displayName} está desativado"
    LaunchResult.NotConfigured -> "${target.displayName} não configurado"
    is LaunchResult.Failed -> "Não foi possível abrir ${target.displayName}"
}
