package com.rocha.jancarlauncher

import android.app.Activity
import android.os.Bundle
import com.rocha.jancarlauncher.overlay.AutoOverlay
import com.rocha.jancarlauncher.overlay.OverlayPolicy
import com.rocha.jancarlauncher.ui.dash.DashModel

/**
 * Prévia da faixa sobre o Android Auto (só debug; o emulador não tem o AA): trata outro app como se fosse o AA e
 * usa dados falsos. `adb shell am start -n com.rocha.jancarlauncher/.OverlayPreviewActivity
 * [--es pkg com.android.settings] [--es stage none|near|shift] [--ez off true]` e depois abra o app escolhido.
 */
class OverlayPreviewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("off", false)) {
            AutoOverlay.debugTarget = null
            AutoOverlay.debugLines = null
        } else {
            val stage = when (intent.getStringExtra("stage")) {
                "near" -> DashModel.ShiftStage.Near
                "shift" -> DashModel.ShiftStage.Shift
                else -> DashModel.ShiftStage.None
            }
            val rpm = when (stage) { DashModel.ShiftStage.Near -> "2700"; DashModel.ShiftStage.Shift -> "3100"; else -> "2400" }
            AutoOverlay.debugTarget = intent.getStringExtra("pkg") ?: "com.android.settings"
            AutoOverlay.debugLines = OverlayPolicy.Lines("62", rpm, rpm.toFloat(), stage, "14,1 km/L")
        }
        finish()
    }
}
