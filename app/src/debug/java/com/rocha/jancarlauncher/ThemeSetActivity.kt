package com.rocha.jancarlauncher

import android.app.Activity
import android.os.Bundle
import com.rocha.jancarlauncher.ui.home.HomeLayout
import kotlinx.coroutines.runBlocking

/**
 * Só debug: troca o tema da Home sem abrir a galeria (para fotografar os 12).
 * `adb shell am start -n com.rocha.jancarlauncher/.ThemeSetActivity --es layout Grade` (nome do HomeLayout).
 */
class ThemeSetActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = HomeLayout.fromName(intent.getStringExtra("layout"))
        runBlocking { (application as JancarApp).settings.setHomeLayout(layout.name) }
        finish()
    }
}
