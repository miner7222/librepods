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

/** Which appearance the app uses, independent of the system's. */
enum class AppTheme(val preferenceValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        const val PREFERENCE_KEY = "app_theme"

        fun from(value: String?): AppTheme =
            entries.firstOrNull { it.preferenceValue == value } ?: System
    }
}
