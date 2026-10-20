package me.kavishdevar.librepods.services

/** Retain observed transitions across brief control-channel interruptions only. */
internal class SocketAudioTransitions(private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 }) {
    data class EarChange(val previous: List<Boolean>, val current: List<Boolean>) {
        val reworn: Boolean get() = previous.none { it } && current.any { it }
    }

    private var bothCharging: Boolean? = null
    private var inEar: List<Boolean>? = null

    private var disconnectedAt: Long? = null
    private var address: String? = null

    @Synchronized fun reset() {
        disconnectedAt = null
        address = null
        bothCharging = null
        inEar = null
    }

    @Synchronized fun socketDisconnected() {
        if (disconnectedAt == null) disconnectedAt = nowMillis()
    }

    @Synchronized fun socketConnected(targetAddress: String) {
        val downFor = disconnectedAt?.let { nowMillis() - it }
        if (address != targetAddress || (downFor != null && downFor !in 0L until 60_000L)) {
            bothCharging = null
            inEar = null
        }
        address = targetAddress
        disconnectedAt = null
    }

    /** A first report without a recent baseline cannot trigger a restore. */
    @Synchronized fun batteryChanged(nowBothCharging: Boolean?): Boolean? {
        val takenOut = nowBothCharging?.let { now -> bothCharging?.let { it && !now } }
        bothCharging = nowBothCharging
        return takenOut
    }

    /** Null means there is no recent ear baseline; callers must not resume from it. */
    @Synchronized fun earsChanged(leftInEar: Boolean, rightInEar: Boolean): EarChange? {
        val previous = inEar
        val current = listOf(leftInEar, rightInEar)
        inEar = current
        return previous?.let { EarChange(it, current) }
    }
}
