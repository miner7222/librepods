package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningModesTest {
    @Test
    fun `the buds' own answer beats a stale stored switch`() {
        val pro2 = AirPodsPro2USBC().capabilities
        assertFalse(ListeningModes.offAvailable(pro2, reported = 0x02, stored = true))
        assertTrue(ListeningModes.offAvailable(pro2, reported = 0x01, stored = false))
        assertTrue(ListeningModes.offAvailable(pro2, reported = null, stored = true))
    }

    @Test
    fun `models without the off switch always have off`() {
        assertTrue(ListeningModes.offAvailable(AirPodsPro1().capabilities, reported = 0x02, stored = false))
        assertTrue(ListeningModes.offAvailable(AirPods4ANC().capabilities, reported = null, stored = false))
    }

    @Test
    fun `airpods pro 1 never offer adaptive`() {
        assertEquals(
            listOf(NoiseControlMode.OFF, NoiseControlMode.TRANSPARENCY, NoiseControlMode.NOISE_CANCELLATION),
            ListeningModes.available(AirPodsPro1().capabilities, offAvailable = true)
        )
    }

    @Test
    fun `an unknown model keeps every mode`() {
        assertEquals(
            listOf(NoiseControlMode.TRANSPARENCY, NoiseControlMode.ADAPTIVE, NoiseControlMode.NOISE_CANCELLATION),
            ListeningModes.available(capabilities = null, offAvailable = false)
        )
    }
}
