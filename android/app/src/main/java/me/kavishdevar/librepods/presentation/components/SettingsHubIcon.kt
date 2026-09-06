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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem

/**
 * Which accent a hub row carries in the Material theme. Apple gives each row of its
 * settings hub a fixed colour; Material has no fixed colours to give, so a row names
 * the weight it wants instead and the scheme decides what that looks like - which is
 * also what lets the row follow a dynamic-colour wallpaper the fixed values cannot.
 */
enum class SettingsHubAccent { Primary, Secondary, Tertiary }

/**
 * The picture at the head of a settings hub row.
 *
 * The two themes disagree about more than colour here: Apple sets an SF symbol in a
 * small square tile, Material sets a Material symbol in a round one, so a row hands
 * over both glyphs and lets the theme choose. Sizes are M3's own list tokens -
 * `ItemLeadingAvatarSize` 40dp, `ItemLeadingIconSize` 24dp, `ItemLeadingAvatarShape`
 * a full corner; putting an icon inside that avatar rather than a portrait is ours.
 */
@Composable
fun SettingsHubIcon(
    @DrawableRes appleRes: Int,
    appleContainerBrush: Brush,
    @DrawableRes materialRes: Int,
    accent: SettingsHubAccent,
    modifier: Modifier = Modifier
) {
    if (LocalDesignSystem.current == DesignSystem.Apple) {
        AppleSettingsIconTile(
            drawableRes = appleRes,
            containerBrush = appleContainerBrush,
            modifier = modifier
        )
        return
    }

    val scheme = MaterialTheme.colorScheme
    val container = when (accent) {
        SettingsHubAccent.Primary -> scheme.primaryContainer
        SettingsHubAccent.Secondary -> scheme.secondaryContainer
        SettingsHubAccent.Tertiary -> scheme.tertiaryContainer
    }
    val onContainer = when (accent) {
        SettingsHubAccent.Primary -> scheme.onPrimaryContainer
        SettingsHubAccent.Secondary -> scheme.onSecondaryContainer
        SettingsHubAccent.Tertiary -> scheme.onTertiaryContainer
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(materialRes),
            contentDescription = null,
            tint = onContainer,
            modifier = Modifier.size(24.dp)
        )
    }
}
