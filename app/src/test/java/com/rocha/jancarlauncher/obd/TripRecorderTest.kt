package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TripRecorderTest {
    private val wall0 = 1_700_000_000_000L

    /** Anda [seconds] s a [kmh] km/h gastando [lph] L/h, um dado por segundo a partir de [from] ms; devolve o fim. */
    private fun TripRecorder.drive(from: Long, seconds: Int, kmh: Float, lph: Float = 6f, eth: Float = 0.27f): Long {
        var t = from
        repeat(seconds) {
            t += 1_000
            sample(t, wall0 + t, kmh, lph, eth, 1.0)
        }
        return t
    }

    @Test fun startsOnlyWhenMoving_andCountsMovingTimeOnlyAbove0() {
        val r = TripRecorder()
        r.drive(0, 30, 0f)
        assertNull(r.current)
        var t = r.drive(30_000, 60, 60f)
        t = r.drive(t, 60, 0f)
        val cur = r.current!!
        assertEquals(60.0, cur.movingS, 1e-6)
        assertEquals(1.0, cur.km, 1e-6)
        assertEquals(60f, cur.maxKmh)
    }

    @Test fun shortStop_isTheSameTrip() {
        val r = TripRecorder()
        val t = r.drive(0, 60, 60f)
        r.disconnected(t, wall0 + t)
        assertNull(r.sample(t + 120_000, wall0 + t + 120_000, 30f, 3f, 0.27f, 1.0))
        assertEquals(1.0, r.current!!.km, 0.01)
    }

    @Test fun longStop_closesPreviousAndStartsNew() {
        val r = TripRecorder()
        val t = r.drive(0, 60, 60f)
        r.disconnected(t, wall0 + t)
        val closed = r.sample(t + 240_000, wall0 + t + 240_000, 30f, 3f, 0.27f, 1.0)
        assertNotNull(closed)
        assertEquals(1.0, closed!!.km, 1e-6)
        assertEquals(wall0 + t, closed.endWallMs)
        assertEquals(wall0 + t + 240_000, r.current!!.startWallMs)
    }

    @Test fun suspendedOvernight_endsAtLastData() {
        val r = TripRecorder()
        val t = r.drive(0, 60, 60f)
        r.disconnected(t, wall0 + t)
        val closed = r.tick(t + 9 * 3_600_000L)
        assertEquals(wall0 + t, closed!!.endWallMs)
        assertNull(r.current)
    }

    @Test fun tooShort_isDiscarded() {
        val r = TripRecorder()
        val t = r.drive(0, 17, 60f) // ~0,28 km
        r.disconnected(t, wall0 + t)
        assertNull(r.tick(t + 200_000))
        assertNull(r.current)
    }

    @Test fun averagesAndConsumption() {
        val r = TripRecorder()
        var t = r.drive(0, 1800, 40f, lph = 2f, eth = 1f)
        t = r.drive(t, 600, 0f, lph = 0.6f, eth = 1f)
        r.disconnected(t, wall0 + t)
        val rec = r.tick(t + 200_000)!!
        assertEquals(20.0, rec.km, 1e-6)
        assertEquals(40.0, rec.avgKmh!!, 1e-6)
        assertEquals(1.0 + 0.1, rec.liters, 1e-6)
        assertEquals(rec.liters, rec.ethanolLiters, 1e-9)
        assertEquals(20.0 / 1.1, rec.kmPerLiter!!, 1e-6)
    }

    @Test fun suspendedBeforeDisconnect_wakeAndDriveSoon_closesYesterdaysTrip() {
        val r = TripRecorder()
        val t = r.drive(0, 60, 60f)
        // A central suspendeu antes da sessão OBD cair: a queda só é vista ao acordar, 9 h depois.
        val wake = t + 9 * 3_600_000L
        r.disconnected(wake, wall0 + wake)
        val closed = r.sample(wake + 60_000, wall0 + wake + 60_000, 20f, 3f, 0.27f, 1.0)
        assertEquals(wall0 + t, closed!!.endWallMs)
        assertEquals(1.0, closed.km, 1e-6)
    }

    @Test fun ignitionOnEngineOff_withoutSpeed_doesNotKeepTripOpen() {
        val r = TripRecorder()
        val t = r.drive(0, 60, 60f)
        // ACC: o ELM responde mas o carro não (velocidade nula); sessões curtas não contam como dado.
        var x = t
        var closed: TripRecord? = null
        repeat(40) { x += 10_000; closed = closed ?: r.sample(x, wall0 + x, null, null, 0.27f, 1.0) }
        // Fecha 3 min depois do último dado, com o fim nele.
        assertEquals(wall0 + t, closed!!.endWallMs)
        assertNull(r.current)
    }
}
