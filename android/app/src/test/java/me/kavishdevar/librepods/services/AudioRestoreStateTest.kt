package me.kavishdevar.librepods.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRestoreStateTest {
    private val state = AudioRestoreState()
    private val address = "AA:BB:CC:DD:EE:FF"
    private val otherAddress = "11:22:33:44:55:66"
    private val a2dp = 2
    private val headset = 1
    private val profiles = setOf(a2dp, headset)

    private fun release(
        profile: Int, connected: Boolean = true, accepted: Boolean = true,
        restorable: Boolean = true, epoch: Int = state.currentEpoch(), target: String = address
    ): Boolean = state.release(target, profile, epoch, restorable, connected) { accepted }

    @Test fun startupAndUnrelatedDevicesCannotRestoreAudio() {
        assertNull(state.restore(address, profiles))
        release(headset)
        assertNull(state.restore(otherAddress, profiles))
        assertEquals(setOf(headset), state.restore(address, profiles)?.profiles)
    }

    @Test fun onlyAcceptedActiveProfilesAreRestorable() {
        release(a2dp, accepted = false)
        release(headset, connected = false)
        assertNull(state.restore(address, profiles))
        release(headset)
        assertEquals(setOf(headset), state.restore(address, profiles)?.profiles)
    }

    @Test fun nonRestorableDisconnectDoesNotGrantPermission() {
        release(a2dp, restorable = false)
        release(headset, restorable = false)
        assertNull(state.restore(address, profiles))
    }

    @Test fun bothReleasedProfilesCanBeRestoredIndependently() {
        release(a2dp)
        release(headset)
        assertEquals(profiles, state.restore(address, profiles)?.profiles)
        val firstEpoch = state.currentEpoch()
        state.markRestored(address, a2dp, firstEpoch)
        assertTrue(state.wasReleased(address))
        state.finishRestore(address, a2dp, firstEpoch)
        state.finishRestore(address, headset, firstEpoch)
        assertEquals(setOf(headset), state.restore(address, profiles)?.profiles)
        state.markRestored(address, headset, state.currentEpoch())
        state.finishRestore(address, headset, state.currentEpoch())
        assertFalse(state.wasReleased(address))
        assertNull(state.restore(address, profiles))
    }

    @Test fun restoringAnotherDeviceCannotConsumeReleaseRecords() {
        release(headset)
        state.markRestored(otherAddress, headset, state.currentEpoch())
        assertEquals(setOf(headset), state.restore(address, profiles)?.profiles)
    }

    @Test fun cancelledRestoreRetainsForbiddenPoliciesForRetry() {
        release(a2dp)
        val first = state.restore(address, profiles)!!
        state.socketDisconnected()
        val retry = state.restore(address, profiles)!!
        assertFalse(state.isCurrent(first.epoch))
        assertTrue(state.isCurrent(retry.epoch))
        assertEquals(setOf(a2dp), retry.profiles)
    }

    @Test fun releaseRecordsSurviveSocketDropButClaimAndCallbacksDoNot() {
        release(headset)
        val claimedEpoch = state.claim(address)
        state.socketDisconnected()
        assertFalse(state.isCurrent(claimedEpoch))
        assertTrue(state.wasReleased(address))
        assertEquals(setOf(headset), state.restore(address, profiles)?.profiles)
    }

    @Test fun ownershipLossRevokesPendingRestoreAndPlaySideEffects() {
        release(a2dp)
        val restore = state.restore(address, profiles)!!
        state.forget(ownershipLost = true)
        var called = false
        assertFalse(state.withEpoch(restore.epoch) { called = true })
        assertFalse(called)
        assertFalse(state.wasReleased(address))
        assertNull(state.restore(address, profiles))
    }

    @Test fun delayedReleaseCannotForbidAudioAfterRestoreStarts() {
        release(a2dp)
        val delayedEpoch = state.currentEpoch()
        state.restore(address, profiles)
        var called = false
        assertFalse(state.release(address, headset, delayedEpoch, true, true) {
            called = true
            true
        })
        assertFalse(called)
    }

    @Test fun delayedReleaseCannotForbidAudioAfterOwnershipLossOrExplicitConnect() {
        val oldEpoch = state.currentEpoch()
        state.forget(ownershipLost = true)
        var calls = 0
        assertFalse(state.release(address, a2dp, oldEpoch, true, true) { calls++; true })
        val lostEpoch = state.currentEpoch()
        state.forget()
        assertFalse(state.release(address, headset, lostEpoch, false, true) { calls++; true })
        assertEquals(0, calls)
    }

    @Test fun freshBatteryReleaseAfterOwnershipLossCannotGrantPermission() {
        state.forget(ownershipLost = true)
        var called = false
        assertFalse(state.release(address, a2dp, state.currentEpoch(), true, true) {
            called = true
            true
        })
        assertFalse(called)
        assertNull(state.restore(address, profiles))
    }

    @Test fun ownershipLossStillAllowsItsOwnNonRestorableDisconnect() {
        state.forget(ownershipLost = true)
        assertTrue(release(a2dp, restorable = false))
        assertNull(state.restore(address, profiles))
    }

    @Test fun newLocalConnectionAllowsNewReleasesButDoesNotReviveOldCallbacks() {
        val oldEpoch = state.currentEpoch()
        state.forget(ownershipLost = true)
        val ownershipLostEpoch = state.currentEpoch()
        state.localAudioConnected()
        assertFalse(state.hasLostOwnership())
        assertFalse(state.isCurrent(oldEpoch))
        assertFalse(state.isCurrent(ownershipLostEpoch))
        assertNull(state.restore(address, profiles))
        release(headset)
        assertEquals(setOf(headset), state.restore(address, profiles)?.profiles)
    }

    @Test fun claimIsAddressScopedAndConsumedOnce() {
        val claimEpoch = state.claim(address)
        assertNull(state.restore(otherAddress, profiles))
        assertEquals(profiles, state.restore(address, profiles)?.profiles)
        assertFalse(state.isCurrent(claimEpoch))
        state.finishRestore(address, a2dp, state.currentEpoch())
        state.finishRestore(address, headset, state.currentEpoch())
        assertNull(state.restore(address, profiles))
    }

    @Test fun explicitConnectClearsLostOwnershipAndOldReleasePermission() {
        release(a2dp)
        val oldEpoch = state.currentEpoch()
        state.forget(ownershipLost = true)
        val explicitEpoch = state.forget()
        assertFalse(state.isCurrent(oldEpoch))
        assertTrue(state.isCurrent(explicitEpoch))
        assertFalse(state.hasLostOwnership())
        assertNull(state.restore(address, profiles))
    }

    @Test fun staleNonRestorableDisconnectCannotUndoNewClaim() {
        val oldEpoch = state.currentEpoch()
        state.claim(address)
        assertFalse(release(a2dp, restorable = false, epoch = oldEpoch))
        assertEquals(profiles, state.restore(address, profiles)?.profiles)
    }

    @Test fun newDeviceReleaseDoesNotInheritPreviousDevicesProfiles() {
        release(a2dp)
        release(headset, target = otherAddress)
        assertNull(state.restore(address, profiles))
        assertEquals(setOf(headset), state.restore(otherAddress, profiles)?.profiles)
    }

    @Test fun socketDropDoesNotEraseOwnershipLoss() {
        state.forget(ownershipLost = true)
        state.socketDisconnected()
        assertTrue(state.hasLostOwnership())
        assertFalse(release(a2dp))
        assertNull(state.restore(address, profiles))
    }

    @Test fun normalProfileTransitionsDoNotCancelPendingRestore() {
        release(a2dp)
        val restore = state.restore(address, profiles)!!
        state.localAudioConnected()
        assertTrue(state.isCurrent(restore.epoch))
    }

    @Test fun repeatedClaimsForTheSameDeviceKeepTheEpoch() {
        val first = state.claim(address)
        assertEquals(first, state.claim(address))
        assertEquals(first, state.claim(address))
        assertTrue(state.isCurrent(first))
        val other = state.claim(otherAddress)
        assertFalse(state.isCurrent(first))
        assertTrue(state.isCurrent(other))
    }

    @Test fun bleClaimsCannotRevokeExplicitSetupOrItsPendingRestore() {
        val explicit = state.beginExplicitClaim(address)
        assertTrue(state.isClaimPending(address))
        assertFalse(state.isClaimPending(otherAddress))
        assertEquals(explicit, state.claim(address))
        assertNull(state.restore(address, profiles))
        state.ownershipClaimSent(address, explicit)
        assertFalse(state.isClaimPending(address))
        assertEquals(explicit, state.claim(address))
        val restore = state.restore(address, profiles)!!
        assertEquals(restore.epoch, state.claim(address))
        assertTrue(state.isCurrent(restore.epoch))
    }

    @Test fun explicitSetupIgnoresOnlyItsOwnStartupOwnershipReports() {
        val explicit = state.beginExplicitClaim(address)
        state.ownershipClaimSent(otherAddress, explicit)
        state.ownershipClaimSent(address, explicit - 1)
        assertTrue(state.isClaimPending(address))
        state.ownershipClaimSent(address, explicit)
        assertFalse(state.isClaimPending(address))
        state.forget(ownershipLost = true)
        assertTrue(state.hasLostOwnership())
        assertFalse(state.isCurrent(explicit))
    }

    @Test fun failedExplicitSetupAndSocketDropClearStartupProtection() {
        val explicit = state.beginExplicitClaim(address)
        state.abandonExplicitClaim(explicit - 1)
        assertTrue(state.isClaimPending(address))
        state.abandonExplicitClaim(explicit)
        assertFalse(state.isClaimPending(address))
        assertFalse(state.isCurrent(explicit))
        state.beginExplicitClaim(address)
        state.socketDisconnected()
        assertFalse(state.isClaimPending(address))
    }

    @Test fun inFlightRestoreDoesNotBumpEpochOrRedispatchEvenWhenPolicyHasNotSucceeded() {
        release(a2dp)
        release(headset)
        val first = state.restore(address, profiles)!!
        assertTrue(first.dispatch)
        repeat(10) {
            val repeated = state.restore(address, profiles)!!
            assertEquals(first.epoch, repeated.epoch)
            assertEquals(first.profiles, repeated.profiles)
            assertFalse(repeated.dispatch)
        }
        state.finishRestore(address, headset, first.epoch)
        assertFalse(state.restore(address, profiles)!!.dispatch)
        assertTrue(state.isCurrent(first.epoch))
    }

    @Test fun completedFailedPolicyCanRetryOnANewTrigger() {
        release(a2dp)
        val first = state.restore(address, profiles)!!
        state.finishRestore(address, a2dp, first.epoch)
        assertTrue(state.wasReleased(address, a2dp))
        val retry = state.restore(address, profiles)!!
        assertTrue(retry.dispatch)
        assertFalse(state.isCurrent(first.epoch))
        assertTrue(state.isCurrent(retry.epoch))
    }

    @Test fun releaseQueriesAndCompletionsAreProfileAndEpochScoped() {
        release(a2dp)
        assertTrue(state.wasReleased(address, a2dp))
        assertFalse(state.wasReleased(address, headset))
        assertFalse(state.wasReleased(otherAddress, a2dp))
        val old = state.restore(address, profiles)!!
        state.socketDisconnected()
        val current = state.restore(address, profiles)!!
        state.markRestored(address, a2dp, old.epoch)
        state.finishRestore(address, a2dp, old.epoch)
        assertTrue(state.wasReleased(address, a2dp))
        assertFalse(state.restore(address, profiles)!!.dispatch)
        state.markRestored(address, a2dp, current.epoch)
        state.finishRestore(address, a2dp, current.epoch)
        assertFalse(state.wasReleased(address, a2dp))
        assertNull(state.restore(address, profiles))
    }

    @Test fun disconnectedProfilesAreNeverForbidden() {
        var called = false
        assertFalse(state.release(address, headset, state.currentEpoch(), true, false) {
            called = true
            true
        })
        assertFalse(called)
        assertFalse(state.wasReleased(address))
    }

    @Test fun connectingProfilesHaveTheSameRestorePermissionAsConnectedProfiles() {
        // The service treats CONNECTING and CONNECTED as active; DISCONNECTED is rejected above.
        for (profile in profiles) {
            assertTrue(state.release(address, profile, state.currentEpoch(), true, true) { true })
        }
        assertEquals(profiles, state.restore(address, profiles)!!.profiles)
    }
}
