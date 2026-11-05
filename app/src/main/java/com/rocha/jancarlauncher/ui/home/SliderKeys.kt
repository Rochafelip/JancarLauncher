package com.rocha.jancarlauncher.ui.home

/** Teclas de direção (D-pad / volante) no slider: ajusta em passos e **persiste** a cada tecla. */
object SliderKeys {
    const val STEP = 5

    fun step(value: Int, direction: Int, min: Int, max: Int, onChange: (Int) -> Unit, onFinished: () -> Unit) {
        onChange((value + direction * STEP).coerceIn(min, max))
        onFinished()
    }
}
