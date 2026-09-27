/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.data

/**
 * The listening modes the connected AirPods can actually be put in.
 *
 * The app, the tile, its dialog, the widgets and the stem cycle all offer these, and
 * each used to decide on its own. A mode the buds refuse is worse than a missing one:
 * a cycle that lands on it stops there (kavishdevar/librepods#755, #769).
 *
 * [capabilities] is null while the model is not known, which keeps every mode.
 */
object ListeningModes {
    /**
     * Whether Off can be selected.
     *
     * Models without the Off Listening Mode switch (Pro 1, AirPods 4 with ANC) carry
     * Off in their cycle unconditionally. The rest have the switch on the buds, so
     * what they report wins; [stored] is only the last report, for before they have
     * said anything. It used to be ORed in, and since it defaults to on, a stale value
     * kept offering Off to buds that had turned it off.
     */
    fun offAvailable(capabilities: Set<Capability>?, reported: Byte?, stored: Boolean): Boolean {
        if (capabilities != null &&
            Capability.LISTENING_MODE in capabilities &&
            Capability.OFF_LISTENING_MODE !in capabilities
        ) {
            return true
        }
        return reported?.let { it == 0x01.toByte() } ?: stored
    }

    /** Adaptive exists only where Adaptive Audio does; the AirPods Pro 1 have none. */
    fun adaptiveAvailable(capabilities: Set<Capability>?): Boolean =
        capabilities == null || Capability.ADAPTIVE_AUDIO in capabilities

    /** The selectable modes, in the order iOS shows them. */
    fun available(capabilities: Set<Capability>?, offAvailable: Boolean): List<NoiseControlMode> =
        buildList {
            if (offAvailable) add(NoiseControlMode.OFF)
            add(NoiseControlMode.TRANSPARENCY)
            if (adaptiveAvailable(capabilities)) add(NoiseControlMode.ADAPTIVE)
            add(NoiseControlMode.NOISE_CANCELLATION)
        }
}
