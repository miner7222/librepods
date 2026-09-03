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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics

@Composable
fun AppleSettingsIconTile(
    @DrawableRes drawableRes: Int,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val metrics = LocalAppleDesignMetrics.current

    Box(modifier = modifier.padding(end = metrics.settingsHubIconLabelGapAdjustment)) {
        Box(
            modifier = Modifier
                .size(metrics.settingsHubIconTileSize)
                .background(
                    color = containerColor,
                    shape = RoundedCornerShape(metrics.settingsHubIconTileCornerRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(metrics.settingsHubIconTint),
                modifier = Modifier.width(metrics.settingsHubIconGlyphWidth)
            )
        }
    }
}
