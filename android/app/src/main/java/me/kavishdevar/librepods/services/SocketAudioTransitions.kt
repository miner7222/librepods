package me.kavishdevar.librepods.services

/** Restore triggers must be transitions seen on this control socket, never cached UI state. */
internal class SocketAudioTransitions {
    data class EarChange(val previous: List<Boolean>, val current: List<Boolean>) {
        val reworn: Boolean get() = previous.none { it } && current.any { it }
    }

    private var bothCharging: Boolean? = null
    private var inEar: List<Boolean>? = null

    @Synchronized fun reset() {
        bothCharging = null
        inEar = null
    }

    /** The first battery report only establishes a baseline. */
    @Synchronized fun batteryChanged(nowBothCharging: Boolean?): Boolean? {
        val takenOut = nowBothCharging?.let { now -> bothCharging?.let { it && !now } }
        bothCharging = nowBothCharging
        return takenOut
    }

    /** Null means the first ear report of this session; callers must not resume from it. */
    @Synchronized fun earsChanged(leftInEar: Boolean, rightInEar: Boolean): EarChange? {
        val previous = inEar
        val current = listOf(leftInEar, rightInEar)
        inEar = current
        return previous?.let { EarChange(it, current) }
    }
}
