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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem

@Composable
fun NoiseControlButton(
    icon: ImageBitmap,
    onClick: () -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
    usePadding: Boolean = true,
    /**
     * Drawn faded underneath the icon. Apple's Off glyph is the noise cancelling
     * one with its arc dimmed rather than a person on its own, and the two renders
     * share a canvas, so laying one over the other reproduces it.
     */
    underlay: ImageBitmap? = null
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .then(if (usePadding) Modifier.padding(horizontal = 4.dp, vertical = 4.dp) else Modifier)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (underlay != null) {
                Icon(
                    bitmap = underlay,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier
                        .size(NoiseControlIconSize)
                        .alpha(NoiseControlOffArcAlpha)
                )
            }
            Icon(
                bitmap = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(NoiseControlIconSize)
            )
        }
    }
}

/**
 * Apple draws every listening mode glyph at 34dp, measured off the iOS 27
 * captures where they sat 17% over what we had. M3 gives an icon no size of its
 * own; it takes the size of whatever holds it, and the two things holding these
 * agree: the 20dp icon of a small button, which ToggleButtonDefaults carries, is
 * also the expressive list's leading icon size.
 */
val NoiseControlIconSize: Dp
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable get() =
        if (LocalDesignSystem.current == DesignSystem.Material) ToggleButtonDefaults.IconSize
        else AppleNoiseControlIconSize

private val AppleNoiseControlIconSize = 34.dp

/** What Apple leaves of the Off glyph's arc; measured at half strength. */
const val NoiseControlOffArcAlpha = 0.5f

@Preview
@Composable
fun NoiseControlButtonPreview() {
    NoiseControlButton(
        icon = ImageBitmap.imageResource(R.drawable.noise_cancellation),
        onClick = {},
        textColor = Color.White,
    )
}
