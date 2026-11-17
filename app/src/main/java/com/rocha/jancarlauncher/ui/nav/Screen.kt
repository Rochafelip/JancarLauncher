package com.rocha.jancarlauncher.ui.nav

/**
 * [Off] = tela apagada pelo botão "Apagar tela" (preta, brilho mínimo; ver `power/ScreenOffPolicy`).
 * [Dash] = Painel de velocidade/RPM (ver `ui/dash`).
 * [Car] = foto do carro com luzes e portas, aberta pelo Painel (ver `ui/car3d`).
 * [Obd] = diagnóstico pelo OBD (viagens e falhas), aberta pelo Painel.
 * [Settings] = Ajustes do launcher (relatório de erros, atualização...), aberta pelo painel de brilho.
 */
enum class Screen { Welcome, Home, Drawer, Off, Dash, Car, Settings, Obd }
