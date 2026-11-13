package com.rocha.jancarlauncher.ui.nav

/**
 * Tela restaurada quando o Android recria a activity (processo morto e retomado). Num launcher de carro, voltar
 * direto no Painel, na Gaveta ou na tela preta do "Apagar tela" depois disso não faz sentido: Painel/Gaveta → Home,
 * tela apagada → Boas-vindas. Também protege contra estado gravado antes do usuário sair dessas telas.
 */
object ScreenRestore {
    fun onRestore(saved: Screen?): Screen? = when (saved) {
        Screen.Dash, Screen.Car, Screen.Drawer -> Screen.Home
        Screen.Off -> Screen.Welcome
        Screen.Home, Screen.Welcome, null -> saved
    }

    /** Grava pelo nome (estável entre versões); null = nada a gravar. */
    fun save(screen: Screen?): String? = screen?.name

    /** Nome desconhecido (tela removida numa versão nova) é ignorado: a abertura decide a tela. */
    fun restore(saved: String?): Screen? =
        onRestore(saved?.let { name -> Screen.entries.firstOrNull { it.name == name } })
}
