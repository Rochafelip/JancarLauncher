package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TripStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun rec(t: Long) = TripRecord(t, t + 1_000, 600.0, 10.0, 0.8, 0.2, 90f)

    @Test fun keepsNewest20_newestFirst_andRoundTrips() {
        val s = TripStore(tmp.root)
        for (t in 1L..22L) s.add(rec(t))
        val list = TripStore(tmp.root).list()
        assertEquals(20, list.size)
        assertEquals(22L, list.first().startWallMs)
        assertEquals(rec(22L), list.first())
    }
}
