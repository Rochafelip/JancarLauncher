package com.rocha.jancarlauncher

import android.app.Activity
import android.os.Bundle
import com.rocha.jancarlauncher.ui.home.HomeLayout
import kotlinx.coroutines.runBlocking

/**
 * Só debug: troca o tema da Home sem abrir a galeria (para fotografar os 12).
 * `adb shell am start -n com.rocha.jancarlauncher/.ThemeSetActivity --es layout Grade` (nome do HomeLayout).
 * Versões das outras telas: `--es style drawer=Grade` (chaves: welcome, drawer, alert, shift, empty, light_dash).
 */
class ThemeSetActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = (application as JancarApp).settings
        runBlocking {
            intent.getStringExtra("layout")?.let { settings.setHomeLayout(HomeLayout.fromName(it).name) }
            intent.getStringExtra("style")?.split("=", limit = 2)?.takeIf { it.size == 2 }?.let { (k, v) -> settings.setScreenStyle(k, v) }
        }
        finish()
    }
}
