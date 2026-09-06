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

import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The shapes one row of a Material settings card wears.
 *
 * M3's own `segmentedShapes` rounds the top of the first row and the bottom of the
 * last, leaving the corners between rows at the small item shape - and hands a card
 * of one row straight back unchanged, which is that small shape on all four corners
 * and reads as a square. A card of one row is ordinary here (a slider, a lone
 * switch), and it is still a card, so it takes the same `CornerLarge` container the
 * ends of a longer card take.
 */
@Composable
fun segmentedCardShapes(index: Int, count: Int): ListItemShapes =
    if (count == 1) {
        ListItemDefaults.shapes().copy(shape = MaterialTheme.shapes.large)
    } else {
        ListItemDefaults.segmentedShapes(index, count)
    }
