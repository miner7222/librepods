package me.kavishdevar.librepods.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnknownStateTakeoverPolicyTest {
    private val now = 100_000L

    private fun allows(
        lastSeen: Long? = now, leftInEar: Boolean = true, rightInEar: Boolean = false,
        matchesCurrentAirPods: Boolean = true
    ): Boolean = UnknownStateTakeoverPolicy.allows(lastSeen, now, leftInEar, rightInEar) {
        matchesCurrentAirPods
    }

    @Test fun missingAdvertisementCannotGrantTakeover() {
        assertFalse(allows(lastSeen = null))
    }

    @Test fun eitherConfirmedInEarBudCanGrantTakeover() {
        assertTrue(allows(leftInEar = true, rightInEar = false))
        assertTrue(allows(leftInEar = false, rightInEar = true))
        assertTrue(allows(leftInEar = true, rightInEar = true))
    }

    @Test fun neitherBudInEarCannotGrantTakeover() {
        assertFalse(allows(leftInEar = false, rightInEar = false))
    }

    @Test fun freshnessMatchesTheBleStaleDeviceBoundary() {
        assertTrue(allows(lastSeen = now - 15_000L))
        assertFalse(allows(lastSeen = now - 15_001L))
        // Entries can survive until the next ten-second BLE cleanup tick.
        assertFalse(allows(lastSeen = now - 24_999L))
    }

    @Test fun futureTimestampCannotGrantTakeoverAfterAClockChange() {
        assertFalse(allows(lastSeen = now + 1L))
    }

    @Test fun advertisementMustMatchTheCurrentAirPodsIdentity() {
        assertFalse(allows(matchesCurrentAirPods = false))
        assertTrue(allows(matchesCurrentAirPods = true))
    }

    @Test fun ineligibleAdvertisementsNeverReachIdentityVerification() {
        var identityChecks = 0
        val verifyIdentity = { identityChecks++; true }
        assertFalse(UnknownStateTakeoverPolicy.allows(null, now, true, true, verifyIdentity))
        assertFalse(UnknownStateTakeoverPolicy.allows(now, now, false, false, verifyIdentity))
        assertFalse(UnknownStateTakeoverPolicy.allows(now - 15_001L, now, true, true, verifyIdentity))
        assertFalse(UnknownStateTakeoverPolicy.allows(now + 1L, now, true, true, verifyIdentity))
        assertEquals(0, identityChecks)
    }
}
