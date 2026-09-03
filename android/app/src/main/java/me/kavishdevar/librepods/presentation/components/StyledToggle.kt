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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.sectionHeader
import me.kavishdevar.librepods.presentation.theme.secondaryLabel
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun StyledToggle(
    title: String? = null,
    label: String,
    description: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    header: Boolean = false,
    firstInColumn: Boolean = false
) {
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val appleMetrics = LocalAppleDesignMetrics.current
    val appleTopPadding = when {
        title != null && firstInColumn -> appleMetrics.sectionHeaderColumnTopInset
        title != null -> appleMetrics.sectionHeaderTopGap
        firstInColumn -> appleMetrics.cardColumnTopInset
        else -> appleMetrics.cardGap
    }
    Column(
        modifier = Modifier.padding(
            top = if (m3eEnabled) 12.dp else appleTopPadding,
            bottom = if (m3eEnabled) 12.dp else 0.dp
        )
    ) {
        title?.let {
            Box(
                modifier = Modifier
                    .background(if (m3eEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = if (m3eEnabled) 16.dp else appleMetrics.cardHorizontalInset)
                    .padding(
                        top = 4.dp,
                        bottom = if (m3eEnabled) 12.dp else appleMetrics.sectionHeaderBottomGap
                    )
            ) {
                Text(
                    text = it,
                    color = if (m3eEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.sectionHeader,
                    style = if (m3eEnabled) MaterialTheme.typography.labelSmallEmphasized else appleMetrics.sectionHeaderStyle
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (m3eEnabled) if (header) MaterialTheme.colorScheme.primaryContainer else Color.Transparent else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(if (m3eEnabled) (if (header) 64.dp else 16.dp) else appleMetrics.cardCornerRadius)
                )
                .clip(RoundedCornerShape(if (m3eEnabled) (if (header) 64.dp else 16.dp) else appleMetrics.cardCornerRadius))
        ) {
            if (m3eEnabled) {
                StyledToggleContent(
                    label = label,
                    description = description,
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = onCheckedChange,
                    index = 0,
                    count = 1,
                    header = header
                )
            } else {
                StyledToggleContent(
                    label = label,
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = onCheckedChange,
                    index = 0,
                    count = 1
                )
            }
        }
        if (description != null && !m3eEnabled) {
            Spacer(modifier = Modifier.height(appleMetrics.cardFooterGap))
            Text(
                text = description,
                style = appleMetrics.sectionFooterStyle,
                color = MaterialTheme.colorScheme.secondaryLabel,
                modifier = Modifier.padding(horizontal = appleMetrics.cardHorizontalInset)
            )
        }
    }
}

@Composable
fun StyledListScope.StyledToggle(
    label: String,
    description: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    item { index, count ->
        StyledToggleContent(
            label = label,
            description = description,
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            index = index,
            count = count
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StyledToggleContent(
    label: String,
    description: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
    header: Boolean = false
) {
    val currentChecked by rememberUpdatedState(checked)

    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material

    if (m3eEnabled) {
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
                shapes = ListItemDefaults.shapes().copy(
                    shape = defaultShape,
                    pressedShape = RoundedCornerShape(24.dp),
                    selectedShape = RoundedCornerShape(24.dp),
                    hoveredShape = RoundedCornerShape(24.dp),
                ),
                onClick = { onCheckedChange(!currentChecked) },
                trailingContent = {
                    Switch(
                        checked = currentChecked,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.padding(end = if (header) 8.dp else 0.dp),
                        enabled = enabled
                    )
                },
                supportingContent = description?.let {
                    {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(top = if (header) 2.dp else 4.dp, bottom = if (header) 8.dp else 4.dp)
                                .padding(horizontal = if (header) 8.dp else 0.dp),
                            color = if (header && enabled) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
                        )
                    }
                },
                content = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        modifier = Modifier
                            .padding(
                                top = if (header) 8.dp else 4.dp,
                                bottom = if (header) 2.dp else 4.dp
                            )
                            .padding(horizontal = if (header) 8.dp else 0.dp),
                        color = if (header && enabled) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
                    )
                },
                enabled = enabled,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 64.dp),
                colors = if (header) ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ListItemDefaults.segmentedColors()
            )
            if (index+1 != count) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    } else {
        val appleMetrics = LocalAppleDesignMetrics.current
        val isPressed = remember { mutableStateOf(false) }
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        shape = RoundedCornerShape(appleMetrics.cardCornerRadius),
                        color = if (isPressed.value) Color(0xFFE0E0E0) else Color.Transparent
                    )
                    .heightIn(min = appleMetrics.listRowMinHeight)
                    .padding(horizontal = appleMetrics.cardHorizontalInset)
                    .padding(
                        vertical = if (description != null) {
                            appleMetrics.stackedRowVerticalPadding
                        } else {
                            0.dp
                        }
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed.value = true
                                tryAwaitRelease()
                                isPressed.value = false
                            }
                        )
                    }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        if (enabled) {
                            scope.launch { haptics.performHapticFeedback(if (!currentChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff) }
                            onCheckedChange(!currentChecked)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        // 4dp let the description run right up to the switch; the
                        // extra room makes it wrap before it gets there.
                        .padding(end = 16.dp)
                ) {
                    Text(
                        // bodyMedium, not labelMedium: same size, but labelMedium
                        // inherits Medium weight and made these read bolder than
                        // the list rows beside them.
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )

                    if (description != null) {
                        // No spacer: the two styles' line leading already puts ~6dp
                        // between the ink, which is what iOS shows.
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondaryLabel
                        )
                    }
                }

                StyledSwitch(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = {
                        if (enabled) {
                            onCheckedChange(it)
                        }
                    }
                )
            }
            if (index+1 != count) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0x40888888),
                    modifier = Modifier
                        .padding(start = 12.dp,end = 12.dp)
                )
            }
        }
    }
}

@Preview(name = "List", group = "Apple")
@Composable
fun StyledToggleAppleListPreview() {
    val checked = remember { mutableStateOf(false) }
    LibrePodsTheme(m3eEnabled = false) {
        StyledList {
            StyledToggle(
                label = "Apple Styled List",
                description = "This is an example description for the styled toggle.",
                checked = checked.value,
                onCheckedChange = { checked.value = !checked.value }
            )
        }
    }
}

@Preview(name = "Normal", group = "Apple")
@Composable
fun StyledToggleApplePreview() {
    val checked = remember { mutableStateOf(false) }
    LibrePodsTheme(m3eEnabled = false) {
        StyledToggle(
            label = "Apple",
            description = "This is an example description for the styled toggle.",
            checked = checked.value,
            onCheckedChange = { checked.value = !checked.value }
        )
    }
}

@Preview(name = "List", group = "Apple")
@Composable
fun StyledToggleM3EListPreview() {
    val checked = remember { mutableStateOf(false) }
    LibrePodsTheme(m3eEnabled = true) {
        StyledList {
            StyledToggle(
                label = "Apple Styled List",
//                description = "This is an example description for the styled toggle.",
                checked = checked.value,
                onCheckedChange = { checked.value = !checked.value }
            )
        }
    }
}

@Preview(name = "Normal", group = "Material")
@Composable
fun StyledToggleM3EPreview() {
    val checked = remember { mutableStateOf(false) }
    LibrePodsTheme(m3eEnabled = true) {
        StyledToggle(
            label = "Material",
            description = "This is an example description for the styled toggle.",
            checked = checked.value,
            onCheckedChange = { checked.value = !checked.value }
        )
    }
}
