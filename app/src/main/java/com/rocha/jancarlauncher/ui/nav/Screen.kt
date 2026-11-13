package com.rocha.jancarlauncher.ui.nav

/**
 * [Off] = tela apagada pelo botão "Apagar tela" (preta, brilho mínimo; ver `power/ScreenOffPolicy`).
 * [Dash] = Painel de velocidade/RPM (ver `ui/dash`).
 * [Car] = carro 3D, aberto pelo Painel (ver `ui/car3d`).
 */
enum class Screen { Welcome, Home, Drawer, Off, Dash, Car }
