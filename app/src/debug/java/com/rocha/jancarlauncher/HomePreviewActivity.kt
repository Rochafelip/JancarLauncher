package com.rocha.jancarlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rocha.jancarlauncher.launch.ContextSettingsStarter
import com.rocha.jancarlauncher.launch.SystemSettingsOpener
import com.rocha.jancarlauncher.media.NowPlayingState
import com.rocha.jancarlauncher.ui.UiActions
import com.rocha.jancarlauncher.ui.home.ClockInfo
import com.rocha.jancarlauncher.ui.home.HomeLayout
import com.rocha.jancarlauncher.ui.home.HomeScreen
import com.rocha.jancarlauncher.ui.theme.JancarTheme

/**
 * Só debug: a Home de um tema com mídia falsa, para conferir o player nos 12 temas sem música tocando.
 * `adb shell am start -f 0x10008000 -n com.rocha.jancarlauncher/.HomePreviewActivity --es layout Grade
 *  [--es media none|music|radio|long] [--ez light true] [--ez paused true]`
 */
class HomePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as JancarApp
        val layout = HomeLayout.fromName(intent.getStringExtra("layout"))
        val light = intent.getBooleanExtra("light", false)
        val playing = !intent.getBooleanExtra("paused", false)
        val np = when (intent.getStringExtra("media")) {
            "music" -> NowPlayingState("Garota de Ipanema", "Tom Jobim", null, playing, false)
            "radio" -> NowPlayingState("FM 99.1", null, null, playing, false, isRadio = true)
            "long" -> NowPlayingState(
                "Uma faixa com um nome muito, muito comprido para ver se cabe numa linha só sem empurrar nada",
                "Um artista com nome comprido também", null, playing, false,
            )
            else -> NowPlayingState.EMPTY
        }
        val actions = UiActions(this, app.launchController, SystemSettingsOpener(ContextSettingsStarter(this)))
        setContent {
            JancarTheme(dark = !light) {
                HomeScreen(
                    clock = ClockInfo("18:41", "Terça-feira, 17 de novembro"),
                    isDark = !light,
                    brightnessPercent = 60,
                    brightnessOpen = false,
                    actions = actions,
                    onToggleTheme = {},
                    onToggleBrightness = {},
                    onCloseBrightness = {},
                    onBrightnessChange = {},
                    onBrightnessChangeFinished = {},
                    onOpenDrawer = {},
                    nowPlaying = np,
                    layout = layout,
                )
            }
        }
    }
}
