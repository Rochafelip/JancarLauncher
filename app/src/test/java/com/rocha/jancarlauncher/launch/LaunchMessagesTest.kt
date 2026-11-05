package com.rocha.jancarlauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchMessagesTest {
    private val radio = AppTarget("radio", "Rádio", Kind.System, "com.jancar.radio", "x.Y")

    @Test fun started_hasNoMessage() = assertNull(LaunchResult.Started.userMessage(radio))

    @Test fun notInstalled_message() = assertEquals("Rádio não está instalado", LaunchResult.NotInstalled.userMessage(radio))

    @Test fun disabled_message() = assertEquals("Rádio está desativado", LaunchResult.Disabled.userMessage(radio))

    @Test fun notConfigured_message() = assertEquals("Rádio não configurado", LaunchResult.NotConfigured.userMessage(radio))

    @Test fun failed_message() = assertEquals("Não foi possível abrir Rádio", LaunchResult.Failed(RuntimeException()).userMessage(radio))
}
