package com.rocha.jancarlauncher.boot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverTest {
    @Test fun bootCompleted_isHandled() = assertTrue(BootReceiver.isBootAction("android.intent.action.BOOT_COMPLETED"))

    @Test fun otherActionsAndNull_areIgnored() {
        assertFalse(BootReceiver.isBootAction("android.intent.action.SCREEN_ON"))
        assertFalse(BootReceiver.isBootAction("android.intent.action.LOCKED_BOOT_COMPLETED"))
        assertFalse(BootReceiver.isBootAction(null))
    }
}
