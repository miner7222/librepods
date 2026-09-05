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

package me.kavishdevar.librepods.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The vertical rhythm of a settings column, in whichever measure the current theme
 * keeps. A section asks for the gap its role calls for and gets it, so a screen is
 * written once and both themes space it themselves - which is the point: the themes
 * are meant to differ in how things look, not in where they sit.
 *
 * Material used to have its screens post the gaps by hand instead, and every section
 * added without its spacer, or given one on top of a component that already carried
 * its own, came out at the wrong distance from its neighbours.
 */
data class SectionMetrics(
    /** Between one section and the next. */
    val cardGap: Dp,
    /** Above a section that opens with a header. */
    val sectionHeaderTopGap: Dp,
    /** Above a section that opens with a header and starts the column. */
    val sectionHeaderColumnTopInset: Dp,
    /** Between a header and the card it introduces. */
    val sectionHeaderBottomGap: Dp,
    /** Above a section that starts the column without a header. */
    val cardColumnTopInset: Dp,
)

val AppleSectionMetrics = SectionMetrics(
    cardGap = AppleDesignMetrics.cardGap,
    sectionHeaderTopGap = AppleDesignMetrics.sectionHeaderTopGap,
    sectionHeaderColumnTopInset = AppleDesignMetrics.sectionHeaderColumnTopInset,
    sectionHeaderBottomGap = AppleDesignMetrics.sectionHeaderBottomGap,
    cardColumnTopInset = AppleDesignMetrics.cardColumnTopInset,
)

/**
 * The 16dp its screens had been posting by hand. Opening the column asks for the
 * same: a section can be the first card without being the first thing on the screen
 * - the battery screen leads with a paragraph - and nothing would then separate the
 * two.
 */
val MaterialSectionMetrics = SectionMetrics(
    cardGap = 16.dp,
    sectionHeaderTopGap = 16.dp,
    sectionHeaderColumnTopInset = 16.dp,
    sectionHeaderBottomGap = 12.dp,
    cardColumnTopInset = 16.dp,
)

val LocalSectionMetrics = compositionLocalOf { AppleSectionMetrics }
