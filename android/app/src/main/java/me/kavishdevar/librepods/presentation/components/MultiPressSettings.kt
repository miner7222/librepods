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

package me.kavishdevar.librepods.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.StemPressType
import me.kavishdevar.librepods.data.StemAction

@Composable
fun stemActionLabel(action: StemAction): String = when (action) {
    StemAction.PLAY_PAUSE -> stringResource(R.string.stem_action_play_pause)
    StemAction.NEXT_TRACK -> stringResource(R.string.stem_action_next_track)
    StemAction.PREVIOUS_TRACK -> stringResource(R.string.stem_action_previous_track)
    StemAction.VOLUME_UP -> stringResource(R.string.stem_action_volume_up)
    StemAction.VOLUME_DOWN -> stringResource(R.string.stem_action_volume_down)
    StemAction.CYCLE_NOISE_CONTROL_MODES -> stringResource(R.string.listening_mode)
    StemAction.DIGITAL_ASSISTANT -> stringResource(R.string.digital_assistant)
}

/**
 * The double and triple press actions, a section each with a row per AirPod. A row opens
 * the picker for that press and AirPod.
 */
@Composable
fun MultiPressSettings(
    pressActions: Map<String, StemAction>,
    navigateToStemPress: (press: StemPressType, left: Boolean) -> Unit,
    firstInColumn: Boolean = false
) {
    val presses = listOf(
        StemPressType.DOUBLE_PRESS to R.string.double_press,
        StemPressType.TRIPLE_PRESS to R.string.triple_press,
    )
    presses.forEachIndexed { index, (press, titleRes) ->
        StyledList(
            title = stringResource(titleRes),
            firstInColumn = firstInColumn && index == 0
        ) {
            listOf(true to R.string.left, false to R.string.right).forEach { (left, sideRes) ->
                val key = StemAction.multiPressPrefKey(press, left)!!
                val action = pressActions[key] ?: StemAction.multiPressFallbacks.getValue(key)
                StyledListItem(
                    name = stringResource(sideRes),
                    description = stemActionLabel(action),
                    onClick = { navigateToStemPress(press, left) }
                )
            }
        }
    }
}
