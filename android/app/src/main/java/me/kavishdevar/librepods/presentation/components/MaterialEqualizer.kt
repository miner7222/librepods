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

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package me.kavishdevar.librepods.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import kotlin.math.roundToInt

/**
 * The equalizer as M3 draws one.
 *
 * There is no equalizer in the M3 catalogue — nothing in it draws a response
 * curve — but every part of one is there, and two arrived with Expressive. A
 * **centered** slider is for "a value from a positive and negative value range…
 * when zero, or the default value, is in the middle", which is a band exactly; and
 * an orientation of **vertical**. Three centered sliders stood on end is what an
 * M3 equalizer is, and it says the same thing Apple's curve does: how far each
 * band sits from flat.
 *
 * Vertical is the one piece Compose does not ship. M3 lists the orientation as
 * available on Android Views and "as tokens on other platforms", so there is no
 * preset to call here. Rather than redraw a slider and lose the track geometry,
 * the gap the track leaves around the handle, the stop indicator at the centre and
 * the spring the handle moves on, this turns the real one on its side: the layout
 * swaps its constraints and the layer rotates a quarter turn, which carries the
 * pointer input round with it, so a drag upward still raises the value.
 */
/**
 * How far a band reads either side of flat. The wire value is 0 to 100 with 50 in
 * the middle, which is far too fine to put in front of anyone; an equalizer is
 * marked in steps, so this is the number of them each way.
 */
private const val BandSteps = 10

private val BandLength = 200.dp
private val BandThickness = 48.dp

@Composable
fun MaterialEqualizerCard(
    lowOffset: MutableState<Float>,
    midOffset: MutableState<Float>,
    highOffset: MutableState<Float>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val maxOffset = with(LocalDensity.current) { BandLength.toPx() } / 2f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EqualizerBand(stringResource(R.string.equalizer_low), lowOffset, maxOffset, enabled)
        EqualizerBand(stringResource(R.string.equalizer_mid), midOffset, maxOffset, enabled)
        EqualizerBand(stringResource(R.string.equalizer_high), highOffset, maxOffset, enabled)
    }
}

@Composable
private fun EqualizerBand(
    label: String,
    offset: MutableState<Float>,
    maxOffset: Float,
    enabled: Boolean
) {
    val interactions = remember { MutableInteractionSource() }
    val step = (-offset.value / maxOffset * BandSteps).roundToInt()
        // Label shows while its interaction source reports a press, and a slider
        // hands the press over to a drag as soon as the finger moves - which is the
        // moment the number is actually wanted. So the label watches a source of its
        // own, holding one press for as long as the value is being changed.
        val labelInteractions = remember { MutableInteractionSource() }
        val labelPress = remember { PressInteraction.Press(Offset.Zero) }
        var showValue by remember { mutableStateOf(false) }
        LaunchedEffect(showValue) {
            if (showValue) labelInteractions.emit(labelPress)
            else labelInteractions.emit(PressInteraction.Release(labelPress))
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Slider(
            // The screen stores each band the way the curve wants it: a pixel
            // offset from the flat line, positive downward because that is where a
            // canvas puts it. A slider counts the other way.
            value = -offset.value / maxOffset,
            onValueChange = {
                showValue = true
                offset.value = -it * maxOffset
            },
            onValueChangeFinished = { showValue = false },
            valueRange = -1f..1f,
            enabled = enabled,
            modifier = Modifier
                .width(BandThickness)
                .height(BandLength)
                .rotateQuarterTurn(),
            interactionSource = interactions,
            // M3 shows the value in a label over the handle for as long as it is
            // held, and a band that is otherwise only a position on a track has
            // nowhere else to say what it is set to.
            thumb = {
                Label(
                    label = {
                        // M3's value indicator is a pill of the inverse surface carrying a
                        // label-large number. A plain tooltip is the nearest ready-made thing
                        // and it is the wrong one: square corners, small print, and padding
                        // that leaves space for a caret it never draws, which pushed the
                        // number off to one side of its own balloon.
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.inverseSurface,
                                    CircleShape
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (step > 0) "+$step" else "$step",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                        }
                    },
                    interactionSource = labelInteractions
                ) {
                    SliderDefaults.Thumb(interactionSource = interactions, enabled = enabled)
                }
            },
            track = { SliderDefaults.CenteredTrack(sliderState = it, enabled = enabled) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Stands a horizontal component on its end. The layout hands the child the
 * constraints the other way round and reports back the size it was given, so a
 * slider measures itself as wide as this is tall; the layer then rotates the drawn
 * result about its top left corner and the placement slides it back into frame.
 * Pointer input goes through the same transform, so the drag follows the track.
 */
private fun Modifier.rotateQuarterTurn(): Modifier =
    graphicsLayer {
        rotationZ = 270f
        transformOrigin = TransformOrigin(0f, 0f)
    }.layout { measurable, constraints ->
        val placeable = measurable.measure(
            Constraints(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.place(-placeable.width, 0)
        }
    }
