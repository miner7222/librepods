package me.kavishdevar.librepods.services

/** Ownership permission, independent of Bluetooth callbacks and the control socket's lifetime. */
internal class AudioRestoreState(private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 }) {
    data class Restore(val epoch: Int, val profiles: Set<Int>, val dispatch: Boolean = true)

    private var epoch = 0
    private var claimedAddress: String? = null
    private var releasedAddress: String? = null
    private val releasedProfiles = mutableMapOf<Int, Long>()
    private var ownershipLost = false
    private var pendingOwnershipAddress: String? = null
    private var restoringAddress: String? = null
    private var restoring: Restore? = null
    private val pendingProfiles = mutableSetOf<Int>()

    @Synchronized fun currentEpoch(): Int {
        expireReleases()
        return epoch
    }
    @Synchronized fun isCurrent(expectedEpoch: Int): Boolean {
        expireReleases()
        return epoch == expectedEpoch
    }
    @Synchronized fun hasLostOwnership(): Boolean = ownershipLost
    @Synchronized fun wasReleased(address: String): Boolean {
        expireReleases()
        return releasedAddress == address
    }
    @Synchronized fun wasReleased(address: String, profile: Int): Boolean {
        expireReleases()
        return releasedAddress == address && profile in releasedProfiles
    }
    @Synchronized fun isClaimPending(address: String): Boolean = pendingOwnershipAddress == address

    /** Keep the check and its side effect atomic with respect to ownership changes. */
    @Synchronized fun withEpoch(expectedEpoch: Int, action: () -> Unit): Boolean {
        expireReleases()
        if (epoch != expectedEpoch) return false
        action()
        return true
    }

    @Synchronized fun forget(ownershipLost: Boolean = false): Int {
        epoch++
        claimedAddress = null
        releasedAddress = null
        releasedProfiles.clear()
        pendingOwnershipAddress = null
        clearRestore()
        this.ownershipLost = ownershipLost
        return epoch
    }

    @Synchronized fun claim(address: String): Int {
        if (claimedAddress == address || pendingOwnershipAddress == address || restoringAddress == address) return epoch
        epoch++
        clearRestore()
        pendingOwnershipAddress = null
        claimedAddress = address
        ownershipLost = false
        return epoch
    }

    @Synchronized fun beginExplicitClaim(address: String): Int {
        forget()
        claimedAddress = address
        pendingOwnershipAddress = address
        return epoch
    }

    @Synchronized fun ownershipClaimSent(address: String, expectedEpoch: Int) {
        if (epoch == expectedEpoch && pendingOwnershipAddress == address) pendingOwnershipAddress = null
    }

    @Synchronized fun abandonExplicitClaim(expectedEpoch: Int) {
        if (epoch == expectedEpoch) forget()
    }

    /** Consuming an explicit action must not leave blanket permission for a later re-wear. */
    @Synchronized fun completeExplicitClaim(address: String, expectedEpoch: Int) {
        if (epoch != expectedEpoch) return
        if (claimedAddress == address) claimedAddress = null
        if (pendingOwnershipAddress == address) pendingOwnershipAddress = null
    }

    /** Releasing again revokes callbacks from a restore before either profile is forbidden. */
    @Synchronized fun beginRelease(address: String): Int {
        expireReleases()
        if (restoringAddress == address) {
            epoch++
            clearRestore()
        }
        return epoch
    }

    @Synchronized fun localAudioConnected() {
        if (ownershipLost) epoch++
        ownershipLost = false
    }

    /** A socket drop revokes callbacks, but must not strand a policy we forbade ourselves. */
    @Synchronized fun socketDisconnected() {
        // A failed control-channel attempt cannot cancel the user's profile takeover.
        if (pendingOwnershipAddress != null) return
        epoch++
        claimedAddress = null
        pendingOwnershipAddress = null
        clearRestore()
    }

    /** Only an accepted release of a connected or connecting profile grants restore permission. */
    @Synchronized fun release(
        address: String, profile: Int, expectedEpoch: Int, restorable: Boolean,
        wasActive: Boolean, forbid: () -> Boolean
    ): Boolean {
        if (epoch != expectedEpoch || (restorable && (!wasActive || ownershipLost))) return false
        val accepted = forbid()
        if (accepted && restoringAddress == address) clearRestore()
        if (restorable && accepted && wasActive) {
            if (releasedAddress != address) releasedProfiles.clear()
            releasedAddress = address
            releasedProfiles.putIfAbsent(profile, nowMillis())
        }
        return accepted
    }

    @Synchronized fun restore(address: String, claimedProfiles: Set<Int>): Restore? {
        expireReleases()
        if (ownershipLost) return null
        if (pendingOwnershipAddress == address) return null
        if (restoringAddress == address) return restoring?.copy(dispatch = false)
        val profiles = mutableSetOf<Int>()
        if (releasedAddress == address) profiles += releasedProfiles.keys
        if (claimedAddress == address) profiles += claimedProfiles
        if (profiles.isEmpty()) return null
        claimedAddress = null
        // Keep release records until the policy is actually allowed. A cancelled restore can retry.
        return Restore(++epoch, profiles).also {
            restoringAddress = address
            restoring = it
            pendingProfiles.clear()
            pendingProfiles += profiles
        }
    }

    @Synchronized fun markRestored(address: String, profile: Int, expectedEpoch: Int) {
        if (epoch != expectedEpoch || releasedAddress != address) return
        releasedProfiles.remove(profile)
        if (releasedProfiles.isEmpty()) releasedAddress = null
    }

    @Synchronized fun finishRestore(address: String, profile: Int, expectedEpoch: Int) {
        if (epoch != expectedEpoch || restoringAddress != address) return
        pendingProfiles -= profile
        if (pendingProfiles.isEmpty()) clearRestore()
    }

    private fun expireReleases() {
        val now = nowMillis()
        val expired = releasedProfiles.filterValues { now - it >= 30 * 60_000L }.keys
        if (expired.isEmpty()) return
        releasedProfiles.keys.removeAll(expired)
        if (releasedProfiles.isEmpty()) releasedAddress = null
        // A delayed proxy callback must not use permission that has expired either.
        epoch++
        clearRestore()
    }

    private fun clearRestore() {
        restoringAddress = null
        restoring = null
        pendingProfiles.clear()
    }
}
