package me.kavishdevar.librepods

import me.kavishdevar.librepods.data.AirPodsPro1
import me.kavishdevar.librepods.data.AirPodsPro2USBC
import me.kavishdevar.librepods.data.ListeningModes
import me.kavishdevar.librepods.data.NoiseControlMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListeningModeShortcutTest {
    private val pro1 = ListeningModes.available(AirPodsPro1().capabilities, offAvailable = true)
    private val pro2 = ListeningModes.available(AirPodsPro2USBC().capabilities, offAvailable = false)

    @Test
    fun `default pair is noise cancellation and transparency`() {
        assertEquals(listOf(2, 3), ListeningModeShortcut.resolveModes(null, pro1))
        assertEquals(listOf(2, 3), ListeningModeShortcut.resolveModes(null, pro2))
    }

    @Test
    fun `valid pair preserves selection order and supports off where accepted`() {
        assertEquals(listOf(4, 2), ListeningModeShortcut.resolveModes("4,2", pro2))
        assertEquals(listOf(1, 3), ListeningModeShortcut.resolveModes("1,3", pro1))
    }

    @Test
    fun `invalid or duplicate pairs fall back to first two supported modes`() {
        listOf("", "2", "2,2", "2,3,4", "garbage,2,3", "2,nope", "0,3", "99,2").forEach {
            assertEquals("stored=$it", listOf(3, 4), ListeningModeShortcut.resolveModes(it, pro2))
        }
    }

    @Test
    fun `model change or disabling off invalidates the whole pair`() {
        assertEquals(listOf(1, 3), ListeningModeShortcut.resolveModes("4,2", pro1))
        assertEquals(listOf(3, 4), ListeningModeShortcut.resolveModes("1,2", pro2))
    }

    @Test
    fun `picking a new mode replaces the least recently selected mode`() {
        assertEquals(listOf(3, 4), ListeningModeShortcut.selectMode("2,3", pro2, 4))
        assertEquals(listOf(4, 2), ListeningModeShortcut.selectMode("3,4", pro2, 2))
        assertEquals(listOf(2, 3), ListeningModeShortcut.selectMode("2,3", pro2, 3))
        assertEquals(listOf(2, 3), ListeningModeShortcut.selectMode("2,3", pro2, 1))
        assertEquals(listOf(3, 2), ListeningModeShortcut.selectMode("4,2", pro1, 2))
    }

    @Test
    fun `toggle switches both ways and starts with first mode from outside the pair`() {
        assertEquals(3, ListeningModeShortcut.nextMode(2, listOf(2, 3)))
        assertEquals(2, ListeningModeShortcut.nextMode(3, listOf(2, 3)))
        assertEquals(2, ListeningModeShortcut.nextMode(4, listOf(2, 3)))
        assertEquals(2, ListeningModeShortcut.nextMode(0, listOf(2, 3)))
    }

    @Test
    fun `fewer than two distinct supported modes cannot be toggled`() {
        listOf(emptyList(), listOf(NoiseControlMode.OFF), List(2) { NoiseControlMode.OFF }).forEach {
            val modes = ListeningModeShortcut.resolveModes("2,3", it)
            assertEquals(emptyList<Int>(), modes)
            assertNull(ListeningModeShortcut.nextMode(2, modes))
            assertEquals(emptyList<Int>(), ListeningModeShortcut.selectMode(null, it, 1))
        }
        assertNull(ListeningModeShortcut.nextMode(2, listOf(2, 2)))
    }
}
