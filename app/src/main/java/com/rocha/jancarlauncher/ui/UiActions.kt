package com.rocha.jancarlauncher.ui

import android.content.Context
import android.widget.Toast
import com.rocha.jancarlauncher.launch.AppTarget
import com.rocha.jancarlauncher.launch.Kind
import com.rocha.jancarlauncher.launch.LaunchController
import com.rocha.jancarlauncher.launch.LaunchResult
import com.rocha.jancarlauncher.launch.SystemSettingsOpener
import com.rocha.jancarlauncher.launch.userMessage
import com.rocha.jancarlauncher.ui.drawer.DrawerEntry

/** Tudo que a UI dispara para fora do app. Toda falha vira toast curto; nada propaga exceção. */
class UiActions(
    private val context: Context,
    private val controller: LaunchController,
    private val settings: SystemSettingsOpener,
) {
    private val autoLabel = AppTarget.unconfigured("auto", "Auto", Kind.System)

    fun open(target: AppTarget) = report(controller.open(target), target)

    fun openAuto() = report(controller.openAuto(), autoLabel)

    fun openSettings(action: String) {
        if (!settings.open(action)) toast("Não foi possível abrir os ajustes")
    }

    fun openDrawerEntry(entry: DrawerEntry) {
        when {
            entry.settingsAction != null -> openSettings(entry.settingsAction)
            entry.target != null -> open(entry.target)
            else -> report(LaunchResult.NotConfigured, AppTarget.unconfigured(entry.id, entry.name, entry.tag))
        }
    }

    private fun report(result: LaunchResult, target: AppTarget) {
        result.userMessage(target)?.let(::toast)
    }

    private fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
