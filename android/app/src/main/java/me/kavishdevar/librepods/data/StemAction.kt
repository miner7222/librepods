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

import me.kavishdevar.librepods.bluetooth.AACPManager

enum class StemAction {
    PLAY_PAUSE,
    PREVIOUS_TRACK,
    NEXT_TRACK,
    DIGITAL_ASSISTANT,
    CYCLE_NOISE_CONTROL_MODES,
    VOLUME_UP,
    VOLUME_DOWN;
    companion object {
        fun fromString(action: String): StemAction? {
            return entries.find { it.name == action }
        }

        /**
         * A stored action, or [default] when nothing is stored or the name is not one this
         * build knows (written by another build, or by a version that has since renamed or
         * dropped the entry). Names are stored, so adding an entry never invalidates one.
         */
        fun fromStored(stored: String?, default: StemAction): StemAction =
            stored?.let(::fromString) ?: default

        /** What the double and triple press pickers offer, in the order they are listed. */
        val multiPressChoices: List<StemAction> = listOf(
            PLAY_PAUSE,
            NEXT_TRACK,
            PREVIOUS_TRACK,
            VOLUME_UP,
            VOLUME_DOWN,
            CYCLE_NOISE_CONTROL_MODES,
            DIGITAL_ASSISTANT,
        )

        /** The preference key of a double or triple press action; null for any other press. */
        fun multiPressPrefKey(type: AACPManager.Companion.StemPressType, left: Boolean): String? {
            val side = if (left) "left" else "right"
            return when (type) {
                AACPManager.Companion.StemPressType.DOUBLE_PRESS -> "${side}_double_press_action"
                AACPManager.Companion.StemPressType.TRIPLE_PRESS -> "${side}_triple_press_action"
                else -> null
            }
        }

        /**
         * What the service falls back to when a double or triple press key holds nothing
         * usable (AirPodsService.initializeConfig); the settings screens show the same.
         */
        val multiPressFallbacks: Map<String, StemAction> = mapOf(
            "left_double_press_action" to PREVIOUS_TRACK,
            "right_double_press_action" to NEXT_TRACK,
            "left_triple_press_action" to PREVIOUS_TRACK,
            "right_triple_press_action" to PREVIOUS_TRACK,
        )
        val defaultActions: Map<AACPManager.Companion.StemPressType, StemAction> = mapOf(
            AACPManager.Companion.StemPressType.SINGLE_PRESS to PLAY_PAUSE,
            AACPManager.Companion.StemPressType.DOUBLE_PRESS to NEXT_TRACK,
            AACPManager.Companion.StemPressType.TRIPLE_PRESS to PREVIOUS_TRACK,
            AACPManager.Companion.StemPressType.LONG_PRESS to CYCLE_NOISE_CONTROL_MODES,
        )
    }
}
