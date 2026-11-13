package com.rocha.jancarlauncher.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenRestoreTest {
    @Test fun nothingSaved_staysNull_soStartupDecidesTheScreen() {
        assertNull(ScreenRestore.onRestore(null))
    }

    @Test fun dash_isNeverRestored_goesHome() {
        assertEquals(Screen.Home, ScreenRestore.onRestore(Screen.Dash))
    }

    @Test fun car3d_isNeverRestored_goesHome() {
        assertEquals(Screen.Home, ScreenRestore.onRestore(Screen.Car))
    }

    @Test fun drawer_isNeverRestored_goesHome() {
        assertEquals(Screen.Home, ScreenRestore.onRestore(Screen.Drawer))
    }

    @Test fun off_isNeverRestored_goesToWelcome() {
        // Voltar numa tela preta, sem contexto, depois do processo recriado seria pior que a Boas-vindas.
        assertEquals(Screen.Welcome, ScreenRestore.onRestore(Screen.Off))
    }

    @Test fun homeAndWelcome_areRestoredAsIs() {
        assertEquals(Screen.Home, ScreenRestore.onRestore(Screen.Home))
        assertEquals(Screen.Welcome, ScreenRestore.onRestore(Screen.Welcome))
    }

    @Test fun saverRoundTrip_appliesTheRule() {
        assertEquals(Screen.Home, ScreenRestore.restore(ScreenRestore.save(Screen.Dash)))
        assertEquals(Screen.Welcome, ScreenRestore.restore(ScreenRestore.save(Screen.Off)))
        assertEquals(Screen.Home, ScreenRestore.restore(ScreenRestore.save(Screen.Home)))
    }

    @Test fun unknownSavedName_isIgnored() {
        // Estado gravado por uma versão antiga do app com uma tela que não existe mais.
        assertNull(ScreenRestore.restore("TelaQueSumiu"))
    }
}
