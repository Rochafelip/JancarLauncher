package com.rocha.jancarlauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Versões das outras telas que o usuário escolhe livremente na galeria (abas Boas-vindas · Gaveta · Painel; pedido do
 * dono, 2026-11-15: "quero que o usuário clique para escolher e fique livre"). Desenhos no Claude Design:
 * https://claude.ai/artifact/R7LQt89MuSyr3Gvb3oF2fd. As atuais continuam como opção (e são o padrão).
 *
 * [Choice.preview] = retângulos da miniatura (fração 0..1 de x, y, largura, altura; `dark` = faixa escura).
 */
object ScreenStyles {
    data class Block(val x: Float, val y: Float, val w: Float, val h: Float, val dark: Boolean = false, val accent: Boolean = false)

    interface Choice {
        val name: String
        val label: String
        val preview: List<Block>
    }

    enum class Welcome(override val label: String, override val preview: List<Block>) : Choice {
        Centro("Relógio no centro", listOf(b(.36, .32, .28, .22), b(.40, .82, .20, .05))),
        Topo("Relógio no topo", listOf(b(.36, .05, .28, .20), b(.38, .86, .24, .08))),
        Canto("Embaixo à esquerda", listOf(b(.04, .62, .34, .28), b(.74, .80, .22, .09))),
        Faixa("Faixa embaixo", listOf(b(0.0, .82, 1.0, .18, dark = true), b(.04, .86, .20, .10), b(.74, .86, .22, .10))),
    }

    enum class Drawer(override val label: String, override val preview: List<Block>) : Choice {
        Atual("Atual", (0 until 12).map { i -> b(.06 + (i % 6) * .148, .22 + (i / 6) * .24, .13, .20) }),
        Grade("Grade grande", (0 until 12).map { i -> b(.04 + (i % 4) * .235, .16 + (i / 4) * .27, .22, .24) }),
        Grupos("Por grupos", (0 until 3).map { i -> b(.04 + i * .315, .16, .29, .78) }),
        Pilulas("Pílulas sobre o carro", (0 until 12).map { i -> b(.04 + (i % 4) * .235, .56 + (i / 4) * .14, .21, .10) }),
    }

    /** Aviso do Painel (porta aberta, reserva): faixa de ponta a ponta ou cartão no meio. */
    enum class Alert(override val label: String, override val preview: List<Block>) : Choice {
        Faixa("Faixa", listOf(b(.04, .62, .92, .14, accent = true))),
        Cartao("Cartão no meio", listOf(b(.34, .36, .32, .22, accent = true))),
    }

    /** Ponto de troca no BRAVO: faixa amarela "SHIFT" ou a própria barra de RPM piscando com "TROQUE". */
    enum class Shift(override val label: String, override val preview: List<Block>) : Choice {
        Faixa("Faixa SHIFT", listOf(b(.04, .14, .92, .07), b(.04, .62, .92, .14, accent = true))),
        Barra("Na barra de RPM", listOf(b(.04, .12, .92, .12, accent = true))),
    }

    /** Painel sem dados do carro: mensagens soltas (atual) ou um cartão de status só. */
    enum class Empty(override val label: String, override val preview: List<Block>) : Choice {
        Atual("Atual", listOf(b(.62, .06, .16, .04), b(.40, .52, .20, .03))),
        Unico("Status único", listOf(b(.30, .30, .40, .14))),
    }

    /** Painel no tema claro. */
    enum class LightDash(override val label: String, override val preview: List<Block>) : Choice {
        Foto("Foto clareada", listOf(b(.04, .14, .92, .07), b(.04, .40, .20, .30), b(.76, .40, .20, .22))),
        Liso("Liso, sem foto", listOf(b(.04, .14, .92, .07), b(.38, .36, .26, .26), b(.04, .84, .92, .11))),
        Cartoes("Cartões sobre a foto", listOf(b(.04, .12, .92, .11), b(.04, .34, .24, .36), b(.38, .36, .26, .30), b(.72, .36, .24, .26), b(.04, .84, .92, .11))),
    }

    data class Set(
        val welcome: Welcome = Welcome.Centro,
        val drawer: Drawer = Drawer.Atual,
        val alert: Alert = Alert.Faixa,
        val shift: Shift = Shift.Faixa,
        val empty: Empty = Empty.Atual,
        val lightDash: LightDash = LightDash.Foto,
    )

    inline fun <reified E : Enum<E>> parse(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    /** Monta as versões a partir das preferências (`SettingsStore.screenStyles`). */
    fun fromPrefs(m: Map<String, String>) = Set(
        welcome = parse(m["welcome"], Welcome.Centro),
        drawer = parse(m["drawer"], Drawer.Atual),
        alert = parse(m["alert"], Alert.Faixa),
        shift = parse(m["shift"], Shift.Faixa),
        empty = parse(m["empty"], Empty.Atual),
        lightDash = parse(m["light_dash"], LightDash.Foto),
    )
}

/** Versões escolhidas, para qualquer tela ler sem passar parâmetro (fornecido pelo AppRoot). */
val LocalScreenStyles = staticCompositionLocalOf<ScreenStyles.Set?> { null }

/**
 * Versões em uso: as do AppRoot ou, fora dele (prévias de debug), as salvas nas preferências.
 */
@Composable
fun screenStyles(): ScreenStyles.Set {
    LocalScreenStyles.current?.let { return it }
    val app = LocalContext.current.applicationContext as? com.rocha.jancarlauncher.JancarApp ?: return ScreenStyles.Set()
    val prefs by app.settings.screenStyles.collectAsState(initial = emptyMap())
    return ScreenStyles.fromPrefs(prefs)
}

private fun b(x: Double, y: Double, w: Double, h: Double, dark: Boolean = false, accent: Boolean = false) =
    ScreenStyles.Block(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), dark, accent)

