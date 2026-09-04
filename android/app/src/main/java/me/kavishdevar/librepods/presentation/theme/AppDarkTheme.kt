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

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Whether the app is currently drawing dark, honouring the appearance the user
 * chose over the system's. Every entry point has to agree on this: an activity
 * that leaves it to [LibrePodsTheme]'s old default kept following the system, so
 * its screens stayed in the phone's appearance while the rest of the app moved.
 * Previews have no preference store, so they follow the system as before.
 */
@Composable
fun rememberAppDarkTheme(): Boolean {
    val systemDark = isSystemInDarkTheme()
    if (LocalInspectionMode.current) return systemDark

    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var appTheme by remember(preferences) {
        mutableStateOf(AppTheme.from(preferences.getString(AppTheme.PREFERENCE_KEY, null)))
    }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { store, key ->
            if (key == AppTheme.PREFERENCE_KEY) {
                appTheme = AppTheme.from(store.getString(key, null))
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return when (appTheme) {
        AppTheme.System -> systemDark
        AppTheme.Light -> false
        AppTheme.Dark -> true
    }
}
