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
import android.content.res.Configuration

/**
 * The overlays are their own windows built from XML, so they pick their colours
 * through resource qualifiers rather than the app's composition. Qualifiers only
 * ever read the system's night mode, which leaves the connect sheet in the phone's
 * appearance while the rest of the app has followed the setting. Inflating against
 * a configuration that carries the chosen mode puts them back in step.
 */
fun Context.withAppNightMode(): Context {
    val preferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
    val night = when (AppTheme.from(preferences.getString(AppTheme.PREFERENCE_KEY, null))) {
        AppTheme.System -> return this
        AppTheme.Light -> Configuration.UI_MODE_NIGHT_NO
        AppTheme.Dark -> Configuration.UI_MODE_NIGHT_YES
    }
    val configuration = Configuration(resources.configuration).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
    }
    return createConfigurationContext(configuration)
}
