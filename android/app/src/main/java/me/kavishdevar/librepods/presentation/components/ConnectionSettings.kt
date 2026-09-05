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
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.R
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun ConnectionSettings(
    automaticEarDetectionEnabled: Boolean,
    onAutomaticEarDetectionChanged: (Boolean) -> Unit,
    automaticConnectionEnabled: Boolean,
    navigateToConnectToThisDevice: () -> Unit,
) {
    // Material opens the option screen with the same note, and repeating it here
    // would put it above the card rather than under it.
    val isApple = LocalDesignSystem.current == DesignSystem.Apple
    StyledList(
        description = if (isApple) stringResource(R.string.connect_to_this_device_footer) else null
    ) {
        StyledToggle(
            label = stringResource(R.string.ear_detection),
            checked = automaticEarDetectionEnabled,
            onCheckedChange = onAutomaticEarDetectionChanged
        )

        // Apple states the choice, not a switch: the row carries the option in
        // force underneath its title and opens the pair to pick from.
        StyledListItem(
            name = stringResource(R.string.connect_to_this_device),
            description = stringResource(
                if (automaticConnectionEnabled) R.string.connect_automatically
                else R.string.connect_when_last_connected
            ),
            orientation = ListItemOrientation.Vertical,
            onClick = navigateToConnectToThisDevice
        )
    }
}
