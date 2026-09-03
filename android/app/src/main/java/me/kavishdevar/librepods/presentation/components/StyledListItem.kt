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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.secondaryLabel
import me.kavishdevar.librepods.presentation.theme.tertiaryLabel
import me.kavishdevar.librepods.presentation.theme.sectionHeader

@Composable
fun StyledListItem(
    modifier: Modifier = Modifier,
    title: String? = null,
    name: String,
    onClick: (() -> Unit)?,
    description: String? = null,
    annotatedDescription: AnnotatedString? = null,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    height: Dp = LocalAppleDesignMetrics.current.listRowMinHeight,
    enabled: Boolean = true,
    orientation: ListItemOrientation = ListItemOrientation.Horizontal,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val appleMetrics = LocalAppleDesignMetrics.current
    Column(
        modifier = Modifier.padding(bottom = if (m3eEnabled) 0.dp else appleMetrics.cardGap)
    ) {
        title?.let {
            Box(
                modifier = Modifier
                    .background(if (m3eEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = if (m3eEnabled) 16.dp else appleMetrics.cardHorizontalInset)
                    .padding(top = 4.dp, bottom = if (m3eEnabled) 8.dp else 4.dp)
            ) {
                Text(
                    text = it,
                    color = if (m3eEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.sectionHeader,
                    style = if (m3eEnabled) MaterialTheme.typography.labelSmallEmphasized else appleMetrics.sectionHeaderStyle
                )
            }
        }
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = if (m3eEnabled) 48.dp else appleMetrics.listRowMinHeight)
                .background(
                    if (m3eEnabled) Color.Transparent else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(if (m3eEnabled) 16.dp else appleMetrics.cardCornerRadius)
                )
                .clip(RoundedCornerShape(if (m3eEnabled) 16.dp else appleMetrics.cardCornerRadius))
        ) {
            StyledListItemContent(
                name = name,
                onClick = onClick,
                description = description,
                annotatedDescription = annotatedDescription,
                inlineContent = inlineContent,
                height = height,
                enabled = enabled,
                index = 0,
                count = 1,
                orientation = orientation,
                leadingContent = leadingContent,
                trailingContent = trailingContent
            )
        }
    }
}

@Composable
fun StyledListScope.StyledListItem(
    modifier: Modifier = Modifier,
    name: String,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    annotatedDescription: AnnotatedString? = null,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    enabled: Boolean = onClick != null,
    orientation: ListItemOrientation = ListItemOrientation.Horizontal,
    selected: Boolean? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    item { index, count ->
        StyledListItemContent(
            name = name,
            onClick = onClick,
            description = description,
            annotatedDescription = annotatedDescription,
            inlineContent = inlineContent,
            enabled = enabled,
            index = index,
            count = count,
            orientation = orientation,
            modifier = modifier,
            selected = selected,
            leadingContent = leadingContent,
            trailingContent = trailingContent
        )
    }
}

enum class ListItemOrientation{
    Horizontal,
    Vertical
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StyledListItemContent(
    modifier: Modifier = Modifier,
    name: String,
    onClick: (() -> Unit)?,
    description: String? = null,
    annotatedDescription: AnnotatedString? = null,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    height: Dp = LocalAppleDesignMetrics.current.listRowMinHeight,
    enabled: Boolean = true,
    index: Int,
    count: Int,
    orientation: ListItemOrientation = ListItemOrientation.Horizontal,
    selected: Boolean? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val appleMetrics = LocalAppleDesignMetrics.current
    val descriptionText = annotatedDescription ?: description?.let { AnnotatedString(it) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceDimColor = MaterialTheme.colorScheme.surfaceDim
    var backgroundColor by remember { mutableStateOf(surfaceColor) }
    val animatedBackgroundColor by animateColorAsState(targetValue = backgroundColor, animationSpec = tween(durationMillis = 500))
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    when (LocalDesignSystem.current) {
        DesignSystem.Apple -> {
            val trailingContentDefault: @Composable () -> Unit = {
                if (trailingContent == null) {
                    if (onClick != null) {
                        if (selected != null) {
                            val floatAnimateState by animateFloatAsState(
                                targetValue = if (selected) 1f else 0f,
                                animationSpec = tween(durationMillis = 300)
                            )

                            Icon(
                                painter = painterResource(R.drawable.sf_checkmark),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = floatAnimateState),
                                modifier = Modifier.padding(end = 4.dp).size(24.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.sf_chevron_forward),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiaryLabel,
                                modifier = Modifier
                                    .padding(start = if (descriptionText != null) 6.dp else 0.dp)
                                    .size(19.dp)
                            )
                        }
                    }
                } else {
                    trailingContent()
                }
            }
            Column (
                modifier = Modifier
                    .background(
                        animatedBackgroundColor,
                        when {
                            (index == 0 && count == 1) -> {
                                RoundedCornerShape(appleMetrics.cardCornerRadius)
                            }

                            (index == 0) -> {
                                RoundedCornerShape(
                                    topStart = appleMetrics.cardCornerRadius,
                                    topEnd = appleMetrics.cardCornerRadius,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            }

                            (index + 1 == count) -> {
                                RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = appleMetrics.cardCornerRadius,
                                    bottomEnd = appleMetrics.cardCornerRadius
                                )
                            }

                            else -> {
                                RectangleShape
                            }
                        }
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (enabled) {
                                    backgroundColor = surfaceDimColor
                                    tryAwaitRelease()
                                    backgroundColor = surfaceColor
                                }
                            },
                            onTap = {
                                if (enabled) {
                                    scope.launch {
                                        haptics.performHapticFeedback(
                                            HapticFeedbackType.ContextClick
                                        )
                                    }
                                    onClick?.invoke()
                                }
                            }
                        )
                    }
                    .heightIn(min = height)
                    .padding(horizontal = appleMetrics.cardHorizontalInset)
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = height)
                        .padding(
                            vertical = if (orientation == ListItemOrientation.Vertical) {
                                appleMetrics.stackedRowVerticalPadding
                            } else {
                                0.dp
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingContent != null) {
                        leadingContent()
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column (verticalArrangement = Arrangement.Center) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (descriptionText != null && orientation == ListItemOrientation.Vertical) {
                            // No spacer: the two styles' line leading already puts ~6dp
                            // between the ink, which is what iOS shows.
                            Text(
                                text = descriptionText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondaryLabel,
                                inlineContent = inlineContent,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (orientation == ListItemOrientation.Horizontal && descriptionText != null) {
                        Text(
                            text = descriptionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondaryLabel,
                            inlineContent = inlineContent,
                        )
                    }

                    trailingContentDefault()
                }
                if (index+1 != count) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(0x40888888),
                        modifier = Modifier
                            .padding(start = if (leadingContent != null) 12.dp else 0.dp)
                    )
                }
            }
        }

        DesignSystem.Material -> {
            val defaultShape = when {
                count == 1 -> RoundedCornerShape(24.dp)

                index == 0 -> RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )

                index == count - 1 -> RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                )

                else -> RoundedCornerShape(8.dp)
            }
            Column {
                SegmentedListItem(
                    modifier = modifier.heightIn(min = 64.dp),
                    shapes = ListItemDefaults.shapes().copy(
                        shape = defaultShape,
                        pressedShape = RoundedCornerShape(24.dp),
                        selectedShape = RoundedCornerShape(24.dp),
                        hoveredShape = RoundedCornerShape(24.dp),
                    ),
                    onClick = onClick ?: {},
                    leadingContent = leadingContent,
                    trailingContent = {
                        if (trailingContent == null) {
                            if (onClick != null) {
                                if (selected == true) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                } else if (selected == null) {
                                    Icon(
                                        Icons.AutoMirrored.Default.KeyboardArrowRight,
                                        contentDescription = null
                                    )
                                }
                            }
                        } else {
                            trailingContent()
                        }
                    },
                    supportingContent = {
                        if (descriptionText != null) Text(
                            descriptionText,
                            style = MaterialTheme.typography.bodySmall,
                            inlineContent = inlineContent,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    },
                    content = {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            modifier = Modifier.padding(
                                top = 4.dp,
                                bottom = if (descriptionText != null) 0.dp else 4.dp
                            )
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    colors = if (onClick == null) {
                            ListItemDefaults.segmentedColors().run {
                                copy(
                                    disabledContentColor = contentColor,
                                    disabledSupportingContentColor = supportingContentColor,
                                    disabledTrailingContentColor = trailingContentColor
                                )
                            }
                        } else ListItemDefaults.segmentedColors(),
                    enabled = onClick != null && enabled,
                    selected = selected ?: false
                )
                if (index+1 != count) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}
