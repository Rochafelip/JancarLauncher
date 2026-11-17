package com.rocha.jancarlauncher.system

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class AppScopeTest {
    @Test fun exceptionInAMonitor_isLogged_andDoesNotKillTheScope() = runBlocking {
        val log = RingLog()
        val scope = appScope(log)
        scope.launch { error("laço quebrou") }
        delay(200)
        assertTrue(log.lines().any { it.contains("laço quebrou") })
        assertTrue(scope.isActive)
        var ran = false
        scope.launch { ran = true }.join()
        assertTrue(ran)
    }
}
