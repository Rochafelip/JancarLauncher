package com.rocha.jancarlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rocha.jancarlauncher.obd.MilStatus
import com.rocha.jancarlauncher.obd.ScanResult
import com.rocha.jancarlauncher.ui.obd.FaultsTab
import com.rocha.jancarlauncher.ui.obd.ObdScreen
import com.rocha.jancarlauncher.ui.obd.ObdTab
import com.rocha.jancarlauncher.ui.theme.JancarTheme

/**
 * Só debug: tela OBD com uma leitura de falhas falsa (sem adaptador).
 * `adb shell am start -f 0x10008000 -n com.rocha.jancarlauncher/.ObdPreviewActivity [--ez moving true] [--ez light true]
 *  [--es tab Viagens]`
 */
class ObdPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val moving = intent.getBooleanExtra("moving", false)
        val light = intent.getBooleanExtra("light", false)
        val tab = ObdTab.entries.firstOrNull { it.name == intent.getStringExtra("tab") } ?: ObdTab.Falhas
        val app = application as JancarApp
        val now = System.currentTimeMillis()
        val fake = ScanResult(now, MilStatus(true, 2), listOf("P0301", "P0171"), listOf("P0420"), null)
        val old = listOf(
            fake,
            ScanResult(now - 86_400_000L * 3, MilStatus(false, 0), emptyList(), emptyList(), emptyList()),
        )
        setContent {
            var result by remember { mutableStateOf<ScanResult?>(null) }
            JancarTheme(dark = !light) {
                ObdScreen(
                    onBack = { finish() },
                    tripsTab = PreviewTrips.content,
                    faultsTab = {
                        FaultsTab(
                            canScan = !moving, scanning = false, result = result, history = if (result == null) old.drop(1) else old,
                            catalog = app.dtcCatalog, onScan = { result = fake },
                        )
                    },
                    initialTab = tab,
                )
            }
        }
    }
}

/** Conteúdo da aba Viagens na prévia (preenchido quando a aba existir). */
object PreviewTrips {
    var content: @androidx.compose.runtime.Composable () -> Unit = {}
}
