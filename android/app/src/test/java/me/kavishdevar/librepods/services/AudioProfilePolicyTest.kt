package me.kavishdevar.librepods.services

import android.bluetooth.BluetoothProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioProfilePolicyTest {
    @Test fun forbidOnlyConnectedOrConnectingProfiles() {
        assertTrue(AudioProfilePolicy.canRelease(BluetoothProfile.STATE_CONNECTED))
        assertTrue(AudioProfilePolicy.canRelease(BluetoothProfile.STATE_CONNECTING))
        assertFalse(AudioProfilePolicy.canRelease(BluetoothProfile.STATE_DISCONNECTED))
        assertFalse(AudioProfilePolicy.canRelease(BluetoothProfile.STATE_DISCONNECTING))
    }

    @Test fun anOutputPortDoesNotProveTheProfileIsReady() {
        for (state in listOf(BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_CONNECTING)) {
            assertFalse(AudioProfilePolicy.canPlayA2dp(state, released = false, hasOutput = true))
        }
    }

    @Test fun connectedA2dpNeedsBothItsOwnOutputAndNoOutstandingRelease() {
        assertFalse(AudioProfilePolicy.canPlayA2dp(BluetoothProfile.STATE_CONNECTED, released = true, hasOutput = true))
        assertFalse(AudioProfilePolicy.canPlayA2dp(BluetoothProfile.STATE_CONNECTED, released = false, hasOutput = false))
        assertTrue(AudioProfilePolicy.canPlayA2dp(BluetoothProfile.STATE_CONNECTED, released = false, hasOutput = true))
    }

    @Test fun confirmedConnectionCanPlayBeforeItsOutputPortAppearsButMustStillBeConnectedAndUnreleased() {
        assertTrue(AudioProfilePolicy.canPlayA2dp(BluetoothProfile.STATE_CONNECTED, false, false, true))
        assertFalse(AudioProfilePolicy.canPlayA2dp(BluetoothProfile.STATE_CONNECTED, true, false, true))
        assertFalse(AudioProfilePolicy.canPlayA2dp(BluetoothProfile.STATE_DISCONNECTING, false, true, true))
    }

    @Test fun lateDisconnectCannotCloseNewSessionOrInterruptAnExplicitClaim() {
        assertTrue(ControlSessionPolicy.acceptsDisconnect(1, 1, false, false))
        assertFalse(ControlSessionPolicy.acceptsDisconnect(1, 2, false, false))
        assertFalse(ControlSessionPolicy.acceptsDisconnect(1, 1, true, false))
        assertFalse(ControlSessionPolicy.acceptsDisconnect(1, 1, false, true))
    }

}
