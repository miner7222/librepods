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

import android.annotation.SuppressLint
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.ListeningModes
import me.kavishdevar.librepods.data.NoiseControlMode
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalSectionMetrics
import me.kavishdevar.librepods.presentation.theme.sectionHeader
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt
import me.kavishdevar.librepods.presentation.theme.LocalIsDarkTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnspecifiedRegisterReceiverFlag", "UnusedBoxWithConstraintsScope")
@Composable
fun NoiseControlSettings(
    /** The modes these AirPods accept, in display order; see ListeningModes. */
    modes: List<NoiseControlMode>,
    noiseControlModeValue: Int,
    onNoiseControlModeChanged: (Int) -> Unit
) {
    when (LocalDesignSystem.current) {
        DesignSystem.Material -> {
            val options = listOf(
                Triple(NoiseControlMode.OFF, R.string.off, R.drawable.noise_control_off),
                Triple(NoiseControlMode.TRANSPARENCY, R.string.transparency, R.drawable.transparency),
                Triple(NoiseControlMode.ADAPTIVE, R.string.adaptive, R.drawable.adaptive),
                Triple(
                    NoiseControlMode.NOISE_CANCELLATION,
                    R.string.noise_cancellation,
                    R.drawable.noise_cancellation
                )
            ).filter { it.first in modes }

            val selectedMode = NoiseControlMode.entries[(noiseControlModeValue - 1).coerceIn(0, NoiseControlMode.entries.lastIndex)]

            Column(
                modifier = Modifier.padding(
                    top = LocalSectionMetrics.current.sectionHeaderTopGap
                )
            ) {
                SectionHeader(stringResource(R.string.noise_control))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    options.forEachIndexed { index, (mode, labelRes, iconRes) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            ToggleButton(
                                checked = selectedMode == mode,
                                onCheckedChange = {
                                    if (it) {
                                        onNoiseControlModeChanged(mode.ordinal + 1)
                                    }
                                },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                                // Unselected filled toggles are surface
                                // container, which is what the screen behind them
                                // is - so they take surface, the tone the list
                                // items beside them use for the same reason.
                                colors = ToggleButtonDefaults.toggleButtonColors()
                                    .copy(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    // Apple's Off glyph is the noise cancelling one
                                    // with its arc dimmed, and the two renders share
                                    // a canvas, so one goes faded under the other.
                                    if (mode == NoiseControlMode.OFF) {
                                        Icon(
                                            bitmap = ImageBitmap.imageResource(
                                                R.drawable.noise_cancellation
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(NoiseControlIconSize)
                                                .alpha(NoiseControlOffArcAlpha)
                                        )
                                    }
                                    Icon(
                                        bitmap = ImageBitmap.imageResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(NoiseControlIconSize)
                                    )
                                }
                            }

                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        DesignSystem.Apple -> {
            val appleMetrics = LocalAppleDesignMetrics.current
            val isDarkTheme = LocalIsDarkTheme.current
            val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFE2E2E7)
            val textColor = MaterialTheme.colorScheme.onSurface
            val textColorSelected = MaterialTheme.colorScheme.onSurface
            val selectedBackground = if (isDarkTheme) Color(0xBF5C5A5F) else Color(0xFFFFFFFF)

            val noiseControlMode = remember { mutableStateOf(NoiseControlMode.OFF) }

            fun onModeSelected(mode: NoiseControlMode, received: Boolean = false) {
                val previousMode = noiseControlMode.value

                // A mode these AirPods do not have shows as Transparency, which
                // every model with listening modes has.
                val targetMode = if (mode in modes) mode else NoiseControlMode.TRANSPARENCY

                noiseControlMode.value = targetMode

                if (!received && targetMode != previousMode) onNoiseControlModeChanged(targetMode.ordinal + 1)
            }


            val index = (noiseControlModeValue - 1).coerceIn(0, NoiseControlMode.entries.size - 1)
            noiseControlMode.value = NoiseControlMode.entries[index]

            onModeSelected(noiseControlMode.value, received = true)

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    // Not one of the card groups, so it has to ask for the gap above
                    // its header itself.
                    .padding(top = appleMetrics.sectionHeaderTopGap)
                    .padding(horizontal = appleMetrics.cardHorizontalInset)
                    .padding(top = 4.dp, bottom = appleMetrics.sectionHeaderBottomGap)
            ) {
                Text(
                    text = stringResource(R.string.noise_control),
                    color = MaterialTheme.colorScheme.sectionHeader,
                    style = appleMetrics.sectionHeaderStyle
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    // This block is not a card, so it was ending flush against the
                    // next one; iOS leaves the same gap here that it leaves between
                    // cards.
                    // No top padding: the header above already carries the common
                    // sectionHeaderBottomGap, and adding to it set this section's
                    // buttons 8dp lower than every other header's card.
                    .padding(bottom = appleMetrics.cardGap)
            ) {
                val density = LocalDensity.current
                val buttonCount = modes.size
                val buttonWidth = maxWidth / buttonCount
                fun slotOf(mode: NoiseControlMode): Int = modes.indexOf(mode).coerceAtLeast(0)

                val isDragging = remember { mutableStateOf(false) }
                var dragOffset by remember {
                    mutableFloatStateOf(
                        with(density) { (buttonWidth * slotOf(noiseControlMode.value)).toPx() }
                    )
                }

                val animationSpec: AnimationSpec<Float> = SpringSpec(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = 0.01f
                )

                val targetOffset = buttonWidth * slotOf(noiseControlMode.value)

                val animatedOffset by animateFloatAsState(
                    targetValue = with(density) {
                        if (isDragging.value) dragOffset else targetOffset.toPx()
                    },
                    animationSpec = animationSpec,
                    label = "selector"
                )

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(backgroundColor, RoundedCornerShape(28.dp))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // No underlay on this pass: the row above draws the
                            // same buttons over it, and two half-strength arcs
                            // stacked read as three quarters. Only the slot under
                            // the selected pill is hidden here, which is why the
                            // arc looked right there and nowhere else.
                            modes.forEach { mode ->
                                NoiseControlButton(
                                    icon = ImageBitmap.imageResource(noiseControlIcon(mode)),
                                    onClick = { onModeSelected(mode) },
                                    textColor = if (noiseControlMode.value == mode) textColorSelected else textColor,
                                    modifier = Modifier.weight(1f),
                                    usePadding = false
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(buttonWidth)
                                .fillMaxHeight()
                                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                                .zIndex(0f)
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { delta ->
                                        dragOffset = (dragOffset + delta).coerceIn(
                                            0f,
                                            with(density) { (buttonWidth * (buttonCount - 1)).toPx() }
                                        )
                                    },
                                    onDragStarted = { isDragging.value = true },
                                    onDragStopped = {
                                        isDragging.value = false
                                        val position =
                                            dragOffset / with(density) { buttonWidth.toPx() }
                                        val newIndex = position.roundToInt()
                                        val newMode = modes.getOrElse(newIndex) {
                                            noiseControlMode.value
                                        }
                                        onModeSelected(newMode)
                                    }
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                                    .background(selectedBackground, RoundedCornerShape(26.dp))
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(1f)
                        ) {
                            modes.forEach { mode ->
                                NoiseControlButton(
                                    icon = ImageBitmap.imageResource(noiseControlIcon(mode)),
                                    underlay = if (mode == NoiseControlMode.OFF) {
                                        ImageBitmap.imageResource(R.drawable.noise_cancellation)
                                    } else null,
                                    onClick = { onModeSelected(mode) },
                                    textColor = if (noiseControlMode.value == mode) textColorSelected else textColor,
                                    modifier = Modifier.weight(1f),
                                    usePadding = false
                                )
                            }
                        }
                    }

                    // These four were a bare TextStyle, so they took neither the
                    // typeface nor the weight the rest of the theme carries: default
                    // Roboto at regular, where iOS sets them in its own face at
                    // semibold - 33% more ink for the same 13pt and the same width.
                    val modeLabelStyle = appleMetrics.sectionFooterStyle.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        modes.forEach { mode ->
                            Text(
                                text = stringResource(noiseControlLabel(mode)),
                                style = modeLabelStyle,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun noiseControlIcon(mode: NoiseControlMode): Int = when (mode) {
    NoiseControlMode.OFF -> R.drawable.noise_control_off
    NoiseControlMode.TRANSPARENCY -> R.drawable.transparency
    NoiseControlMode.ADAPTIVE -> R.drawable.adaptive
    NoiseControlMode.NOISE_CANCELLATION -> R.drawable.noise_cancellation
}

private fun noiseControlLabel(mode: NoiseControlMode): Int = when (mode) {
    NoiseControlMode.OFF -> R.string.off
    NoiseControlMode.TRANSPARENCY -> R.string.transparency
    NoiseControlMode.ADAPTIVE -> R.string.adaptive
    NoiseControlMode.NOISE_CANCELLATION -> R.string.noise_cancellation
}

@Preview
@Composable
fun NoiseControlSettingsPreview() {
    LibrePodsTheme(
        m3eEnabled = true
    ) {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            NoiseControlSettings(
                modes = ListeningModes.available(capabilities = null, offAvailable = false),
                noiseControlModeValue = 2,
                onNoiseControlModeChanged = { }
            )
        }
    }
}
