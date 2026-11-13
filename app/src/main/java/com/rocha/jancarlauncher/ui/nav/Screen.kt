package com.rocha.jancarlauncher.ui.nav

/**
 * [Off] = tela apagada pelo botão "Apagar tela" (preta, brilho mínimo; ver `power/ScreenOffPolicy`).
 * [Dash] = Painel de velocidade/RPM (ver `ui/dash`).
 * [Car] = foto do carro com luzes e portas, aberta pelo Painel (ver `ui/car3d`).
 */
enum class Screen { Welcome, Home, Drawer, Off, Dash, Car }
