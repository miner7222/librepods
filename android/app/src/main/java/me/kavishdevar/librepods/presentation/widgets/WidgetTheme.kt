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

package me.kavishdevar.librepods.presentation.widgets

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.widget.RemoteViews
import me.kavishdevar.librepods.R

enum class WidgetTheme(val preferenceValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPreferenceValue(value: String?): WidgetTheme =
            entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}

object WidgetThemePreferences {
    const val SETTINGS_NAME = "settings"
    const val DEFAULT_OPACITY = 50
    private const val THEME_KEY_PREFIX = "widget_theme_"
    private const val OPACITY_KEY_PREFIX = "widget_opacity_"

    fun preferenceKey(appWidgetId: Int): String = "$THEME_KEY_PREFIX$appWidgetId"

    fun opacityPreferenceKey(appWidgetId: Int): String = "$OPACITY_KEY_PREFIX$appWidgetId"

    fun get(preferences: SharedPreferences, appWidgetId: Int): WidgetTheme =
        WidgetTheme.fromPreferenceValue(
            preferences.getString(preferenceKey(appWidgetId), WidgetTheme.SYSTEM.preferenceValue)
        )

    fun set(
        preferences: SharedPreferences,
        appWidgetId: Int,
        theme: WidgetTheme
    ) {
        preferences.edit()
            .putString(preferenceKey(appWidgetId), theme.preferenceValue)
            .apply()
    }

    fun getOpacity(preferences: SharedPreferences, appWidgetId: Int): Int =
        preferences.getInt(opacityPreferenceKey(appWidgetId), DEFAULT_OPACITY).coerceIn(0, 100)

    fun getOpacity(context: Context, appWidgetId: Int): Int =
        getOpacity(
            context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE),
            appWidgetId
        )

    fun setOpacity(
        preferences: SharedPreferences,
        appWidgetId: Int,
        opacity: Int
    ) {
        preferences.edit()
            .putInt(opacityPreferenceKey(appWidgetId), opacity.coerceIn(0, 100))
            .apply()
    }

    fun isDark(context: Context, appWidgetId: Int): Boolean {
        val preferences = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        return when (get(preferences, appWidgetId)) {
            WidgetTheme.SYSTEM ->
                (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            WidgetTheme.LIGHT -> false
            WidgetTheme.DARK -> true
        }
    }
}

private val DARK_WIDGET_BACKGROUND = 0xFF1C1C1E.toInt()
private val LIGHT_WIDGET_BACKGROUND = 0xFFFFFFFF.toInt()

private fun argbWithOpacity(color: Int, opacity: Int): Int {
    val alpha = opacity.coerceIn(0, 100) * 255 / 100
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}

private fun colorWithOpacity(color: Int, opacity: Int): ColorStateList {
    val alpha = opacity.coerceIn(0, 100) * 255 / 100
    return ColorStateList.valueOf(
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    )
}

internal fun RemoteViews.applyBatteryWidgetTheme(isDarkTheme: Boolean, opacity: Int) {
    val primaryColor = if (isDarkTheme) Color.WHITE else Color.BLACK
    val backgroundColor = if (isDarkTheme) DARK_WIDGET_BACKGROUND else LIGHT_WIDGET_BACKGROUND
    setInt(
        R.id.battery_widget_surface,
        "setBackgroundResource",
        if (isDarkTheme) R.drawable.widget_background else R.drawable.widget_background_light
    )
    setColorStateList(
        R.id.battery_widget_surface,
        "setBackgroundTintList",
        colorWithOpacity(
            if (isDarkTheme) DARK_WIDGET_BACKGROUND else LIGHT_WIDGET_BACKGROUND,
            opacity
        )
    )
    intArrayOf(
        R.id.phone_battery_widget,
        R.id.left_battery_widget,
        R.id.right_battery_widget,
        R.id.case_battery_widget
    ).forEach { viewId -> setTextColor(viewId, primaryColor) }
    intArrayOf(
        R.id.phone_battery_icon,
        R.id.left_battery_icon,
        R.id.right_battery_icon,
        R.id.case_battery_icon,
        R.id.phone_charging_icon,
        R.id.left_charging_icon,
        R.id.right_charging_icon,
        R.id.case_charging_icon
    ).forEach { viewId -> setInt(viewId, "setColorFilter", primaryColor) }
    intArrayOf(
        R.id.phone_charging_icon_outline,
        R.id.left_charging_icon_outline,
        R.id.right_charging_icon_outline,
        R.id.case_charging_icon_outline
    ).forEach { viewId ->
        // The outline stands in for the surface behind the ring, so it has to
        // carry the surface's alpha too - at reduced opacity a solid outline
        // reads as a hard blob over a see-through background.
        setInt(viewId, "setColorFilter", argbWithOpacity(backgroundColor, opacity))
    }
}
