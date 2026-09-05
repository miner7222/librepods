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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where a screen's own content begins. Material's scaffold has already inset it, so
 * there is nothing to clear; Apple's navigation bar floats over the content it
 * scrolls behind, so the screen has to clear the bar and the status bar itself.
 *
 * Every screen was working this out for itself, which is three theme branches each
 * before any of them says anything about how it looks.
 *
 * @param columnInset for a screen whose content opens as a column of cards, which
 *   Apple starts a further inset below the bar.
 * @param materialInset for the rare screen that wants a little air under Material's
 *   own inset.
 */
@Composable
fun screenTopPadding(
    columnInset: Boolean = false,
    materialInset: Dp = 0.dp
): Dp {
    if (LocalDesignSystem.current == DesignSystem.Material) return materialInset

    val metrics = LocalAppleDesignMetrics.current
    return WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
        metrics.navigationBarHeight +
        if (columnInset) metrics.cardColumnTopInset else 0.dp
}

/** The room a screen leaves under its last card, above the navigation bar. */
@Composable
fun screenBottomPadding(): Dp =
    if (LocalDesignSystem.current == DesignSystem.Material) 0.dp
    else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp
