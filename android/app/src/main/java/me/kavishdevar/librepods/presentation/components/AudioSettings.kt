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

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun AudioSettings(
    adaptiveVolumeCapability: Boolean,
    conversationalAwarenessCapability: Boolean,
    loudSoundReductionCapability: Boolean,
    adaptiveAudioCapability: Boolean,
    customEqCapability: Boolean,

    adaptiveVolumeChecked: Boolean,
    onAdaptiveVolumeCheckedChange: (Boolean) -> Unit,

    conversationalAwarenessChecked: Boolean,
    onConversationalAwarenessCheckedChange: (Boolean) -> Unit,

    loudSoundReductionChecked: Boolean,
    onLoudSoundReductionCheckedChange: (Boolean) -> Unit,

    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit,

    vendorIdHook: Boolean,
    isPremium: Boolean,
    firstInColumn: Boolean = false
) {
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material

    if (m3eEnabled) {
        if (adaptiveVolumeCapability || conversationalAwarenessCapability || loudSoundReductionCapability || adaptiveAudioCapability) {
            StyledList(
                title = stringResource(R.string.audio),
                firstInColumn = firstInColumn
            ) {
                if (adaptiveVolumeCapability) {
                    StyledToggle(
                        label = stringResource(R.string.personalized_volume),
                        description = stringResource(R.string.personalized_volume_description),
                        checked = adaptiveVolumeChecked,
                        onCheckedChange = onAdaptiveVolumeCheckedChange,
                        enabled = isPremium,
                    )
                }

                if (conversationalAwarenessCapability) {
                    StyledToggle(
                        label = stringResource(R.string.conversational_awareness),
                        description = stringResource(R.string.conversational_awareness_description),
                        checked = conversationalAwarenessChecked,
                        onCheckedChange = onConversationalAwarenessCheckedChange,
                        enabled = isPremium,
                    )
                }

                if (loudSoundReductionCapability && vendorIdHook) {
                    StyledToggle(
                        label = stringResource(R.string.loud_sound_reduction),
                        description = stringResource(R.string.loud_sound_reduction_description),
                        checked = loudSoundReductionChecked,
                        onCheckedChange = onLoudSoundReductionCheckedChange,
                        enabled = isPremium,
                    )
                }

                if (adaptiveAudioCapability) {
                    StyledListItem(
                        name = stringResource(R.string.adaptive_audio),
                        onClick = navigateToAdaptiveStrength,
                    )
                }

                if (customEqCapability) {
                    StyledListItem(
                        name = stringResource(R.string.equalizer),
                        onClick = navigateToEqualizer,
                    )
                }
            }
        }
        return
    }

    // iOS gives each of these its own card and puts the explanation underneath it
    // on the grouped background, rather than stacking them inside one card.
    if (adaptiveVolumeCapability) {
        StyledToggle(
            label = stringResource(R.string.personalized_volume),
            description = stringResource(R.string.personalized_volume_description),
            checked = adaptiveVolumeChecked,
            onCheckedChange = onAdaptiveVolumeCheckedChange,
            enabled = isPremium,
            firstInColumn = firstInColumn,
        )
    }

    if (conversationalAwarenessCapability) {
        StyledToggle(
            label = stringResource(R.string.conversational_awareness),
            description = stringResource(R.string.conversational_awareness_description),
            checked = conversationalAwarenessChecked,
            onCheckedChange = onConversationalAwarenessCheckedChange,
            enabled = isPremium,
            firstInColumn = firstInColumn && !adaptiveVolumeCapability,
        )
    }

    if (loudSoundReductionCapability && vendorIdHook) {
        StyledToggle(
            label = stringResource(R.string.loud_sound_reduction),
            description = stringResource(R.string.loud_sound_reduction_description),
            checked = loudSoundReductionChecked,
            onCheckedChange = onLoudSoundReductionCheckedChange,
            enabled = isPremium,
            firstInColumn = firstInColumn &&
                !adaptiveVolumeCapability &&
                !conversationalAwarenessCapability,
        )
    }

    if (adaptiveAudioCapability) {
        StyledList(
            firstInColumn = firstInColumn &&
                !adaptiveVolumeCapability &&
                !conversationalAwarenessCapability &&
                !(loudSoundReductionCapability && vendorIdHook)
        ) {
            StyledListItem(
                name = stringResource(R.string.adaptive_audio),
                onClick = navigateToAdaptiveStrength,
            )
        }
    }

    if (customEqCapability) {
        StyledList(
            firstInColumn = firstInColumn &&
                !adaptiveVolumeCapability &&
                !conversationalAwarenessCapability &&
                !(loudSoundReductionCapability && vendorIdHook) &&
                !adaptiveAudioCapability
        ) {
            StyledListItem(
                name = stringResource(R.string.equalizer),
                onClick = navigateToEqualizer,
            )
        }
    }
}
