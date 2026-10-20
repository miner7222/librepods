package me.kavishdevar.librepods.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocketAudioTransitionsTest {
    private val transitions = SocketAudioTransitions()

    @Test fun firstBatteryAndEarPacketsOnlyEstablishIndependentBaselines() {
        assertNull(transitions.batteryChanged(false))
        assertNull(transitions.earsChanged(true, true))
        assertEquals(false, transitions.batteryChanged(false))
        assertFalse(transitions.earsChanged(true, true)!!.reworn)
    }

    @Test fun onlyObservedChargingToNotChargingRestoresOnce() {
        assertNull(transitions.batteryChanged(true))
        assertEquals(false, transitions.batteryChanged(true))
        assertEquals(true, transitions.batteryChanged(false))
        assertEquals(false, transitions.batteryChanged(false))
        assertEquals(false, transitions.batteryChanged(true))
        assertEquals(true, transitions.batteryChanged(false))
    }

    @Test fun onlyNoneToEitherEarIsARewear() {
        assertNull(transitions.earsChanged(false, false))
        assertFalse(transitions.earsChanged(false, false)!!.reworn)
        assertTrue(transitions.earsChanged(true, false)!!.reworn)
        val both = transitions.earsChanged(true, true)!!
        assertEquals(listOf(true, false), both.previous)
        assertFalse(both.reworn)
        assertFalse(transitions.earsChanged(false, true)!!.reworn)
        assertFalse(transitions.earsChanged(false, false)!!.reworn)
        assertTrue(transitions.earsChanged(false, true)!!.reworn)
    }

    @Test fun socketDropCannotTurnOldCaseOrEarStateIntoARestore() {
        transitions.batteryChanged(true)
        transitions.earsChanged(false, false)
        transitions.reset()
        // First reports after a Mac used the AirPods must not revive the old release records.
        assertNull(transitions.batteryChanged(false))
        assertNull(transitions.earsChanged(true, false))
        assertEquals(false, transitions.batteryChanged(false))
        assertFalse(transitions.earsChanged(true, true)!!.reworn)
    }

    @Test fun freshTransitionsStillWorkAfterReset() {
        transitions.batteryChanged(false)
        transitions.earsChanged(true, true)
        transitions.reset()
        assertNull(transitions.batteryChanged(true))
        assertNull(transitions.earsChanged(false, false))
        assertTrue(transitions.earsChanged(true, true)!!.reworn)
        assertEquals(true, transitions.batteryChanged(false))
    }

    @Test fun repeatedOutOfCaseBatteryReportsDoNotUndoNotWearingRelease() {
        transitions.batteryChanged(false)
        transitions.earsChanged(true, true)
        transitions.earsChanged(false, false)
        repeat(10) { assertEquals(false, transitions.batteryChanged(false)) }
    }

    @Test fun missingBatteryReadingCannotTurnChargingIntoATakeout() {
        assertNull(transitions.batteryChanged(true))
        assertNull(transitions.batteryChanged(null))
        assertNull(transitions.batteryChanged(false))
        assertEquals(false, transitions.batteryChanged(true))
        assertEquals(true, transitions.batteryChanged(false))
    }
}
