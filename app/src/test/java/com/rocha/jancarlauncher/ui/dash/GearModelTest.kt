package com.rocha.jancarlauncher.ui.dash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GearModelTest {
    private val daily = DashModel.DriveMode.Daily
    private val sport = DashModel.DriveMode.Sport

    @Test fun ratios_matchTheFox10Spec() {
        // 5ª a 100 km/h ~3400 rpm (1.0 8V de câmbio curto); 1ª ~185 rpm por km/h.
        assertEquals(3412.0, GearModel.RPM_PER_KMH[4] * 100, 30.0)
        assertEquals(185.0, GearModel.RPM_PER_KMH[0], 3.0)
    }

    @Test fun gearFor_findsEachGear() {
        for (g in 1..5) {
            val speed = 40f
            val rpm = (GearModel.RPM_PER_KMH[g - 1] * speed).toFloat()
            assertEquals(g, GearModel.gearFor(rpm, speed))
        }
    }

    @Test fun gearFor_toleratesSpeedometerError_butNotNeutral() {
        // Velocímetro 6% acima do real: ainda 4ª.
        assertEquals(4, GearModel.gearFor((GearModel.RPM_PER_KMH[3] * 60).toFloat(), 60f * 1.06f))
        // Embreagem pisada a 60 km/h com o motor em marcha lenta: nenhuma marcha.
        assertNull(GearModel.gearFor(900f, 60f))
        // Parado / sem dado.
        assertNull(GearModel.gearFor(850f, 0f))
        assertNull(GearModel.gearFor(null, 40f))
    }

    @Test fun suggest_upNearTheShiftPoint_downWhenLugging() {
        assertEquals(GearModel.Suggestion(GearModel.Hint.Up, 3), GearModel.suggest(2, 2700f, 27f, daily))
        assertEquals(GearModel.Hint.None, GearModel.suggest(2, 2700f, 27f, sport).hint) // BRAVO troca em 5000
        assertEquals(GearModel.Hint.None, GearModel.suggest(5, 3500f, 100f, daily).hint) // não existe 6ª
        // 5ª a 35 km/h ≈ 1200 rpm: descer para a 4ª (≈1500 rpm, longe do ponto de troca).
        assertEquals(GearModel.Suggestion(GearModel.Hint.Down, 4), GearModel.suggest(5, 1200f, 35f, daily))
        // 2ª a 12 km/h ≈ 1200 rpm, mas a 1ª daria ~2200: ok no dia a dia, desce.
        assertEquals(GearModel.Hint.Down, GearModel.suggest(2, 1200f, 12f, daily).hint)
        assertEquals("↑3", GearModel.hintText(GearModel.Suggestion(GearModel.Hint.Up, 3)))
    }

    @Test fun estimator_waitsBeforeChangingGear() {
        val e = GearEstimator()
        val third = (GearModel.RPM_PER_KMH[2] * 40).toFloat()
        assertNull(e.update(third, 40f, 0))
        assertEquals(3, e.update(third, 40f, 700))
        // No meio da troca (embreagem): continua 3 até ficar estável no novo valor.
        assertEquals(3, e.update(900f, 40f, 800))
        assertNull(e.update(900f, 40f, 1500))
    }

    @Test fun estimator_dropsAStaleGear_whenReadingsKeepChanging() {
        val e = GearEstimator()
        val r = { g: Int, v: Float -> (GearModel.RPM_PER_KMH[g - 1] * v).toFloat() }
        e.update(r(5, 60f), 60f, 0); assertEquals(5, e.update(r(5, 60f), 60f, 700))
        // Reduzindo rápido: 4ª, 3ª, 2ª a cada 300 ms (nenhuma fica estável).
        e.update(r(4, 40f), 40f, 1000)
        e.update(r(3, 30f), 30f, 1300)
        assertNull(e.update(r(2, 20f), 20f, 1650))
    }
}
