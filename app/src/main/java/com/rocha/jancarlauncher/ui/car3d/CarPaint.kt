package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Cores prontas para ver o Fox em outra cor, só por diversão (pedido do dono, 2026-11-17). Nomes genéricos (sem
 * nomes de fábrica). A lataria (máscara `assets/car/body_mask.png`) é tingida pela luminância da foto prata:
 * canal = L × cor × [gain] + [lift] × cor × 255, então reflexos e sombras continuam. [swatch] = bolinha da escolha.
 */
enum class CarPaint(val label: String, val swatch: Long?, private val rgb: FloatArray?, private val gain: Float, private val lift: Float) {
    Original("Original", null, null, 1f, 0f),
    Vermelho("Vermelho", 0xFFC8141D, floatArrayOf(0.85f, 0.07f, 0.08f), 1.25f, 0f),
    Preto("Preto", 0xFF141518, floatArrayOf(0.19f, 0.19f, 0.21f), 0.95f, 0f),
    Branco("Branco", 0xFFF2F3F4, floatArrayOf(1f, 1f, 1f), 1.15f, 0.06f),
    Grafite("Grafite", 0xFF4A4E55, floatArrayOf(0.42f, 0.44f, 0.47f), 1.0f, 0f),
    AzulMarinho("Azul-marinho", 0xFF1B2A55, floatArrayOf(0.12f, 0.2f, 0.48f), 1.2f, 0f),
    AzulClaro("Azul-claro", 0xFF5E9FE0, floatArrayOf(0.36f, 0.62f, 0.95f), 1.15f, 0f),
    Amarelo("Amarelo", 0xFFF2C21B, floatArrayOf(1f, 0.8f, 0.1f), 1.15f, 0f),
    VerdeLimao("Verde-limão", 0xFF8BD62A, floatArrayOf(0.55f, 0.9f, 0.16f), 1.1f, 0f),
    Laranja("Laranja", 0xFFF07A1A, floatArrayOf(1f, 0.46f, 0.06f), 1.2f, 0f),
    ;

    /** Matriz 4x5 do `ColorMatrix` (linha por canal, alfa intacto); `null` = cor original, sem camada. */
    fun matrix(): FloatArray? {
        val c = rgb ?: return null
        val m = FloatArray(20)
        for (row in 0..2) {
            val k = c[row] * gain
            m[row * 5] = LUM_R * k
            m[row * 5 + 1] = LUM_G * k
            m[row * 5 + 2] = LUM_B * k
            m[row * 5 + 4] = lift * c[row] * 255f
        }
        m[18] = 1f
        return m
    }

    companion object {
        private const val LUM_R = 0.299f
        private const val LUM_G = 0.587f
        private const val LUM_B = 0.114f

        fun fromName(n: String?): CarPaint = entries.firstOrNull { it.name == n } ?: Original
    }
}

/** A fileira de cores só aparece se a camada da lataria existir (máscara no APK). */
fun paintAvailable(body: ImageBitmap?): Boolean = body != null
