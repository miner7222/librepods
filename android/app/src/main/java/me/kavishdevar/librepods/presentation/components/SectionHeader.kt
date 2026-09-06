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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalSectionMetrics
import me.kavishdevar.librepods.presentation.theme.SectionMetrics
import me.kavishdevar.librepods.presentation.theme.sectionHeader

/**
 * The line of text above a group of cards.
 *
 * Apple sets it in its own tinted grey on the grouped background, indented to the
 * card edge; M3 sets it in the accent on nothing at all, indented to the page
 * margin. Five places were drawing it - three of them from the same seventeen
 * lines, character for character, and two more that only ever ran the M3 half and
 * so had that half written out again.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val appleMetrics = LocalAppleDesignMetrics.current
    Box(
        modifier = modifier
            .background(
                if (m3eEnabled) Color.Transparent
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .padding(horizontal = if (m3eEnabled) 16.dp else appleMetrics.cardHorizontalInset)
            .padding(
                top = 4.dp,
                bottom = LocalSectionMetrics.current.sectionHeaderBottomGap
            )
    ) {
        Text(
            text = title,
            color =
                if (m3eEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.sectionHeader,
            style =
                if (m3eEnabled) MaterialTheme.typography.labelLargeEmphasized
                else appleMetrics.sectionHeaderStyle
        )
    }
}

/**
 * The gap a section opens above itself, which depends on what it is and where it
 * sits: a header needs more room than a bare card, and either needs more again
 * when it is the first thing on the screen.
 *
 * @param whenNeither what to leave above a section that has no header and does not
 *   open the column. Cards want the gap between two of them; a slider with no
 *   label carries no header at all and is placed by its caller, so it wants none.
 */
fun sectionTopGap(
    metrics: SectionMetrics,
    hasHeader: Boolean,
    firstInColumn: Boolean,
    whenNeither: Dp = metrics.cardGap
): Dp = when {
    hasHeader && firstInColumn -> metrics.sectionHeaderColumnTopInset
    hasHeader -> metrics.sectionHeaderTopGap
    firstInColumn -> metrics.cardColumnTopInset
    else -> whenNeither
}

/** The rounded rectangle a group of rows sits in. */
@Composable
fun sectionCardShape(): Shape =
    RoundedCornerShape(
        if (LocalDesignSystem.current == DesignSystem.Material) 16.dp
        else LocalAppleDesignMetrics.current.cardCornerRadius
    )
