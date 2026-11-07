package com.rocha.jancarlauncher.ui.screenoff

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rocha.jancarlauncher.power.ScreenOffPolicy

/**
 * Tela apagada: preto total, barras do sistema escondidas e a central mantida acordada (senão ela suspende e o
 * toque deixa de acender). O brilho mínimo da janela é aplicado pelo `AppRoot`. Toque (ao soltar, para o mesmo
 * toque não acionar nada na Boas-vindas) ou tecla física acorda; volume/mídia passam direto sem acordar.
 */
@Composable
fun ScreenOffScreen(onWake: () -> Unit) {
    val view = LocalView.current
    val focus = remember { FocusRequester() }

    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        // Flag na janela, não `view.keepScreenOn`: o atributo da View só é recalculado numa passada de layout, e
        // se a tela apagada for desfeita com a activity parada (outro app por cima → ON_STOP) a flag ficava presa.
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .semantics { contentDescription = "Tela apagada. Toque para acender." }
            .focusRequester(focus)
            .onKeyEvent { event ->
                if (!ScreenOffPolicy.keyWakes(event.nativeKeyEvent.keyCode)) return@onKeyEvent false
                if (event.type == KeyEventType.KeyUp) onWake()
                true
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onWake,
            ),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
