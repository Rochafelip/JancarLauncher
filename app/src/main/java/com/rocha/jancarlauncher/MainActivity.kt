package com.rocha.jancarlauncher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rocha.jancarlauncher.launch.AndroidPackageGateway
import com.rocha.jancarlauncher.launch.ContextSettingsStarter
import com.rocha.jancarlauncher.launch.SystemSettingsOpener
import com.rocha.jancarlauncher.ui.AppRoot
import com.rocha.jancarlauncher.ui.UiActions
import com.rocha.jancarlauncher.ui.home.BrightnessController
import com.rocha.jancarlauncher.ui.nav.Screen
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val events = MutableSharedFlow<Screen>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as JancarApp
        val bootCount = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
        val actions = UiActions(this, app.launchController, SystemSettingsOpener(ContextSettingsStarter(this)))
        val gateway = AndroidPackageGateway(packageManager)
        val brightness = BrightnessController(this)
        // Boas-vindas pedida pelo aviso de "acordou", ou porque o SCREEN_ON passou enquanto o processo estava morto.
        val forceWelcome = intent?.getBooleanExtra(EXTRA_SHOW_WELCOME, false) == true || app.wakeTracker.onLaunch()
        setContent {
            AppRoot(
                settings = app.settings,
                actions = actions,
                gateway = gateway,
                brightness = brightness,
                events = events,
                bootCount = bootCount,
                forceWelcome = forceWelcome,
            )
        }
    }

    /** singleTask: o botão Home do sistema e o aviso de "acordou" chegam aqui, com a Activity já aberta. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        when {
            intent.getBooleanExtra(EXTRA_SHOW_WELCOME, false) -> events.tryEmit(Screen.Welcome)
            Intent.CATEGORY_HOME in (intent.categories ?: emptySet()) -> events.tryEmit(Screen.Home)
        }
    }

    companion object {
        const val EXTRA_SHOW_WELCOME = "show_welcome"
    }
}
