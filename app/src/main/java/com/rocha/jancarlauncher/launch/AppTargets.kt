package com.rocha.jancarlauncher.launch

/** Tabela única de destinos. Trocar um destino = editar uma linha aqui. */
object AppTargets {
    val ANDROID_AUTO = AppTarget(
        "android_auto", "Android Auto", Kind.App,
        "com.google.android.projection.sink", "com.google.android.projection.sink.ui.AndroidAutoActivity",
    )

    // CarPlay é com.autochips.carplayapp — NUNCA com.autochips.carplay (sem "app"), que é só serviço de fundo.
    val CARPLAY = AppTarget(
        "carplay", "CarPlay", Kind.System,
        "com.autochips.carplayapp", "com.autochips.carplayapp.CarPlayActivity",
    )

    val RADIO = AppTarget(
        "radio", "Rádio", Kind.System,
        "com.jancar.radio", "com.jancar.radio.activity.RadioMainActivity",
    )

    // Música e Vídeo: activity fixa escolhida pelo usuário (Fase 0, dumpsys na central). Até lá: "não configurado".
    val MUSIC = AppTarget.unconfigured("music", "Música", Kind.System)
    val VIDEO = AppTarget.unconfigured("video", "Vídeo", Kind.System)

    // Câmera de ré manual: aberta pelo pacote (activity desconhecida; não confirmado que aceita abertura manual).
    val BACK_CAMERA = AppTarget("back_camera", "Câmera de ré", Kind.System, "com.autochips.backcarapp", null)

    val all: List<AppTarget> = listOf(ANDROID_AUTO, CARPLAY, RADIO, MUSIC, VIDEO, BACK_CAMERA)

    fun byId(id: String): AppTarget? = all.firstOrNull { it.id == id }
}
