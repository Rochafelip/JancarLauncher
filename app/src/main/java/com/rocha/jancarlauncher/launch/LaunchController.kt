package com.rocha.jancarlauncher.launch

import android.content.Context

/** Fachada usada pela UI: abre um alvo, ou o "Auto" já resolvido para Android Auto/CarPlay. */
class LaunchController(
    private val launcher: AppLauncher,
    private val bondedPhones: () -> List<BondedPhone>,
    private val usbPhoneAttached: () -> Boolean,
) {
    fun open(target: AppTarget): LaunchResult = launcher.launch(target)

    fun openAuto(userOverride: Projection? = null): LaunchResult {
        val projection = ProjectionChooser.choose(
            ProjectionInput(bondedPhones(), usbPhoneAttached(), userOverride),
        )
        return open(targetFor(projection))
    }

    fun targetFor(projection: Projection): AppTarget = when (projection) {
        Projection.AndroidAuto -> AppTargets.ANDROID_AUTO
        Projection.CarPlay -> AppTargets.CARPLAY
    }

    companion object {
        fun create(context: Context): LaunchController {
            val app = context.applicationContext
            return LaunchController(
                launcher = AppLauncher(AndroidPackageGateway(app.packageManager), ContextActivityStarter(app)),
                bondedPhones = { readBondedPhones(app) },
                usbPhoneAttached = { usbPhoneAttached(app) },
            )
        }
    }
}
