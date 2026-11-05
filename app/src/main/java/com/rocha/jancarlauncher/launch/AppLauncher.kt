package com.rocha.jancarlauncher.launch

sealed interface LaunchResult {
    data object Started : LaunchResult
    data object NotInstalled : LaunchResult
    data object Disabled : LaunchResult
    data object NotConfigured : LaunchResult
    data class Failed(val error: Throwable) : LaunchResult
}

interface PackageGateway {
    fun isInstalled(pkg: String): Boolean
    fun isEnabled(pkg: String): Boolean
}

interface ActivityStarter {
    /** Intent explícita para [pkg]/[activityClass]. Pode lançar (ActivityNotFound, Security...). */
    fun startComponent(pkg: String, activityClass: String)

    /** Abre a activity de lançamento do pacote; false se o pacote não tem uma. */
    fun startLaunchIntentFor(pkg: String): Boolean
}

/**
 * Único ponto do app que abre outros apps. Nunca propaga exceção: o launcher é a tela HOME,
 * um crash aqui deixaria a central sem interface.
 */
class AppLauncher(private val gateway: PackageGateway, private val starter: ActivityStarter) {
    fun launch(target: AppTarget): LaunchResult {
        val pkg = target.pkg ?: return LaunchResult.NotConfigured
        if (!gateway.isInstalled(pkg)) return LaunchResult.NotInstalled
        if (!gateway.isEnabled(pkg)) return LaunchResult.Disabled
        return try {
            val activity = target.activityClass
            when {
                activity != null -> { starter.startComponent(pkg, activity); LaunchResult.Started }
                starter.startLaunchIntentFor(pkg) -> LaunchResult.Started
                else -> LaunchResult.NotInstalled
            }
        } catch (e: RuntimeException) {
            // Cobre ActivityNotFoundException, SecurityException e demais falhas de runtime.
            LaunchResult.Failed(e)
        }
    }
}
