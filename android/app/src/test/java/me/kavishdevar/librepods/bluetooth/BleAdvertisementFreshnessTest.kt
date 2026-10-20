package me.kavishdevar.librepods.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BleAdvertisementFreshnessTest {
    private val nowMillis = 1_000_000L
    private val elapsedNanos = 100_000_000_000L

    @Test fun ordinaryBacklogCannotOverwriteANewerFirstMatchResult() {
        val firstMatch = elapsedNanos - 1_000_000L
        assertNull(BLEManager.advertisementSeenAt(nowMillis, elapsedNanos, firstMatch - 1, firstMatch))
        assertNull(BLEManager.advertisementSeenAt(nowMillis, elapsedNanos, firstMatch, firstMatch))
        assertEquals(nowMillis, BLEManager.advertisementSeenAt(
            nowMillis, elapsedNanos, elapsedNanos, firstMatch
        ))
    }

    @Test fun batchingDoesNotMakeAnOlderAdvertisementFreshAgain() {
        assertEquals(nowMillis - 5_000L, BLEManager.advertisementSeenAt(
            nowMillis, elapsedNanos, elapsedNanos - 5_000_000_000L, Long.MIN_VALUE
        ))
    }

    @Test fun receptionAgeUsesTheExistingFifteenSecondFreshnessLimit() {
        assertEquals(nowMillis - 15_000L, BLEManager.advertisementSeenAt(
            nowMillis, elapsedNanos, elapsedNanos - 15_000_000_000L, Long.MIN_VALUE
        ))
        assertNull(BLEManager.advertisementSeenAt(
            nowMillis, elapsedNanos, elapsedNanos - 15_001_000_000L, Long.MIN_VALUE
        ))
    }

    @Test fun futureRadioTimestampsAreRejected() {
        assertNull(BLEManager.advertisementSeenAt(
            nowMillis, elapsedNanos, elapsedNanos + 1, Long.MIN_VALUE
        ))
    }
}
