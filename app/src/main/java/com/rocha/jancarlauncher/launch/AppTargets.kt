package com.rocha.jancarlauncher.launch

/** Tabela única de destinos. Trocar um destino = editar uma linha aqui. */
object AppTargets {
    // Projeção/mídia nativas do firmware: todas Kind.System (Android Auto também veio no firmware, não foi
    // sideload -- a tag "App" anterior era inconsistente com CarPlay e Rádio, que fazem a mesma função).
    val ANDROID_AUTO = AppTarget(
        "android_auto", "Android Auto", Kind.System,
        "com.google.android.projection.sink", "com.google.android.projection.sink.ui.AndroidAutoActivity",
    )

    // CarPlay é com.autochips.carplayapp -- NUNCA com.autochips.carplay (sem "app"), que é só serviço de fundo.
    val CARPLAY = AppTarget(
        "carplay", "CarPlay", Kind.System,
        "com.autochips.carplayapp", "com.autochips.carplayapp.CarPlayActivity",
    )

    val RADIO = AppTarget(
        "radio", "Rádio", Kind.System,
        "com.jancar.radio", "com.jancar.radio.activity.RadioMainActivity",
    )

    // Música local e Música por Bluetooth são duas activities do MESMO pacote (confirmado via dumpsys).
    val MUSIC = AppTarget(
        "music", "Música", Kind.System,
        "com.jancar.music", "com.jancar.music.activity.MainActivity",
    )
    val BT_MUSIC = AppTarget(
        "bt_music", "Música BT", Kind.System,
        "com.jancar.music", "com.jancar.music.activity.BTMusicActivity",
    )

    val VIDEO = AppTarget(
        "video", "Vídeo", Kind.System,
        "com.jancar.video", "com.jancar.video.ui.activity.VideoListActivity",
    )

    val GALLERY = AppTarget(
        "gallery", "Galeria", Kind.System,
        "com.jancar.gallery", "com.jancar.gallery.ui.activity.GalleryListActivity",
    )

    val FILES = AppTarget(
        "files", "Arquivos", Kind.System,
        "com.mediatek.filemanager", "com.mediatek.filemanager.FileManagerOperationActivity",
    )

    val EQ = AppTarget(
        "eq", "EQ", Kind.System,
        "com.jancar.audiosettings", "com.jancar.audiosettings.ui.activity.MainActivity",
    )

    // Veículo = tela de informações do CAN bus (confirmado abrindo manualmente na central).
    val VEHICLE = AppTarget(
        "vehicle", "Veículo", Kind.System,
        "com.can.activity", "com.can.ui.CanActivity",
    )

    val SCRCPY = AppTarget(
        "scrcpy", "Scrcpy", Kind.App,
        "io.github.miuzarte.scrcpyforandroid", "io.github.miuzarte.scrcpyforandroid.MainActivity",
    )

    val OBD = AppTarget(
        "obd", "OBD-II", Kind.App,
        "com.github.pires.obd.reader", "com.github.pires.obd.reader.activity.MainActivity",
    )

    val all: List<AppTarget> = listOf(
        ANDROID_AUTO, CARPLAY, RADIO, MUSIC, BT_MUSIC, VIDEO, GALLERY, FILES, EQ, VEHICLE, SCRCPY, OBD,
    )

    fun byId(id: String): AppTarget? = all.firstOrNull { it.id == id }
}
