package me.kavishdevar.librepods.services

/** Ownership permission, independent of Bluetooth callbacks and the control socket's lifetime. */
internal class AudioRestoreState {
    data class Restore(val epoch: Int, val profiles: Set<Int>)

    private var epoch = 0
    private var claimedAddress: String? = null
    private var releasedAddress: String? = null
    private val releasedProfiles = mutableSetOf<Int>()
    private var ownershipLost = false

    @Synchronized fun currentEpoch(): Int = epoch
    @Synchronized fun isCurrent(expectedEpoch: Int): Boolean = epoch == expectedEpoch
    @Synchronized fun hasLostOwnership(): Boolean = ownershipLost
    @Synchronized fun wasReleased(address: String): Boolean = releasedAddress == address

    /** Keep the check and its side effect atomic with respect to ownership changes. */
    @Synchronized fun withEpoch(expectedEpoch: Int, action: () -> Unit): Boolean {
        if (epoch != expectedEpoch) return false
        action()
        return true
    }

    @Synchronized fun forget(ownershipLost: Boolean = false): Int {
        epoch++
        claimedAddress = null
        releasedAddress = null
        releasedProfiles.clear()
        this.ownershipLost = ownershipLost
        return epoch
    }

    @Synchronized fun claim(address: String): Int {
        epoch++
        claimedAddress = address
        ownershipLost = false
        return epoch
    }

    @Synchronized fun localAudioConnected() {
        if (ownershipLost) epoch++
        ownershipLost = false
    }

    /** A socket drop revokes callbacks, but must not strand a policy we forbade ourselves. */
    @Synchronized fun socketDisconnected() {
        epoch++
        claimedAddress = null
    }

    /** Only an accepted release of a fully connected profile grants background restore permission. */
    @Synchronized fun release(
        address: String, profile: Int, expectedEpoch: Int, restorable: Boolean,
        wasConnected: Boolean, forbid: () -> Boolean
    ): Boolean {
        if (epoch != expectedEpoch || (restorable && ownershipLost)) return false
        val accepted = forbid()
        if (restorable && accepted && wasConnected) {
            if (releasedAddress != address) releasedProfiles.clear()
            releasedAddress = address
            releasedProfiles += profile
        }
        return accepted
    }

    @Synchronized fun restore(address: String, claimedProfiles: Set<Int>): Restore? {
        if (ownershipLost) return null
        val profiles = mutableSetOf<Int>()
        if (releasedAddress == address) profiles += releasedProfiles
        if (claimedAddress == address) profiles += claimedProfiles
        if (profiles.isEmpty()) return null
        claimedAddress = null
        // Keep release records until the policy is actually allowed. A cancelled restore can retry.
        return Restore(++epoch, profiles)
    }

    @Synchronized fun markRestored(address: String, profile: Int) {
        if (releasedAddress != address) return
        releasedProfiles -= profile
        if (releasedProfiles.isEmpty()) releasedAddress = null
    }
}
