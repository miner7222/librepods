package me.kavishdevar.librepods.data

import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.StemPressType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StemActionTest {
    @Test
    fun `every name an older build stored still parses`() {
        for (name in listOf(
            "PLAY_PAUSE", "PREVIOUS_TRACK", "NEXT_TRACK", "DIGITAL_ASSISTANT", "CYCLE_NOISE_CONTROL_MODES"
        )) {
            assertEquals(name, StemAction.fromString(name)?.name)
        }
    }

    @Test
    fun `the volume actions parse by name`() {
        assertEquals(StemAction.VOLUME_UP, StemAction.fromString("VOLUME_UP"))
        assertEquals(StemAction.VOLUME_DOWN, StemAction.fromString("VOLUME_DOWN"))
    }

    @Test
    fun `an unknown name is not an action`() {
        assertNull(StemAction.fromString("VOLUME_MUTE"))
        assertNull(StemAction.fromString(""))
    }

    @Test
    fun `a stored name this build does not know falls back to the default`() {
        assertEquals(
            StemAction.NEXT_TRACK, StemAction.fromStored("SOMETHING_FROM_A_NEWER_BUILD", StemAction.NEXT_TRACK)
        )
        assertEquals(StemAction.NEXT_TRACK, StemAction.fromStored(null, StemAction.NEXT_TRACK))
        assertEquals(StemAction.NEXT_TRACK, StemAction.fromStored("", StemAction.NEXT_TRACK))
    }

    @Test
    fun `a stored name this build knows wins over the default`() {
        assertEquals(
            StemAction.VOLUME_DOWN, StemAction.fromStored("VOLUME_DOWN", StemAction.NEXT_TRACK)
        )
    }

    @Test
    fun `double and triple presses have a key per bud and nothing else does`() {
        assertEquals("left_double_press_action", StemAction.multiPressPrefKey(StemPressType.DOUBLE_PRESS, true))
        assertEquals("right_double_press_action", StemAction.multiPressPrefKey(StemPressType.DOUBLE_PRESS, false))
        assertEquals("left_triple_press_action", StemAction.multiPressPrefKey(StemPressType.TRIPLE_PRESS, true))
        assertEquals("right_triple_press_action", StemAction.multiPressPrefKey(StemPressType.TRIPLE_PRESS, false))
        assertNull(StemAction.multiPressPrefKey(StemPressType.SINGLE_PRESS, true))
        assertNull(StemAction.multiPressPrefKey(StemPressType.LONG_PRESS, false))
    }

    @Test
    fun `every double and triple key has a fallback`() {
        for (press in listOf(StemPressType.DOUBLE_PRESS, StemPressType.TRIPLE_PRESS)) {
            for (left in listOf(true, false)) {
                val key = StemAction.multiPressPrefKey(press, left)!!
                assertTrue(key, key in StemAction.multiPressFallbacks)
            }
        }
        assertEquals(4, StemAction.multiPressFallbacks.size)
    }

    @Test
    fun `the picker offers the volume actions once each`() {
        val choices = StemAction.multiPressChoices
        assertTrue(StemAction.VOLUME_UP in choices)
        assertTrue(StemAction.VOLUME_DOWN in choices)
        assertEquals(choices.size, choices.toSet().size)
        assertEquals(StemAction.entries.toSet(), choices.toSet())
    }
}
