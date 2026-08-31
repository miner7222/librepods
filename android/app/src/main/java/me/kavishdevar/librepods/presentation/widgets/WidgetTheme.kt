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
private val DARK_BUTTON_BACKGROUND = 0xFF2C2C2E.toInt()
private val DARK_CHECKED_BUTTON_BACKGROUND = 0xFF90A8F6.toInt()
private val LIGHT_BUTTON_BACKGROUND = 0xFFF2F2F7.toInt()
private val LIGHT_CHECKED_BUTTON_BACKGROUND = 0xFF90A8F6.toInt()

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

private data class NoiseControlWidgetThemeResources(
    val buttonShapeStart: Int,
    val buttonShapeMiddle: Int,
    val buttonShapeEnd: Int,
    val checkedButtonShapeStart: Int,
    val checkedButtonShapeMiddle: Int,
    val checkedButtonShapeEnd: Int,
    val buttonColor: Int,
    val checkedButtonColor: Int
)

internal fun RemoteViews.applyNoiseControlWidgetTheme(
    isDarkTheme: Boolean,
    opacity: Int,
    selectedMode: Int? = null,
    allowOffMode: Boolean = true
) {
    val primaryColor = if (isDarkTheme) Color.WHITE else Color.BLACK
    val resources = if (isDarkTheme) {
        NoiseControlWidgetThemeResources(
            buttonShapeStart = R.drawable.widget_button_shape_start,
            buttonShapeMiddle = R.drawable.widget_button_shape_middle,
            buttonShapeEnd = R.drawable.widget_button_shape_end,
            checkedButtonShapeStart = R.drawable.widget_button_checked_shape_start,
            checkedButtonShapeMiddle = R.drawable.widget_button_checked_shape_middle,
            checkedButtonShapeEnd = R.drawable.widget_button_checked_shape_end,
            buttonColor = DARK_BUTTON_BACKGROUND,
            checkedButtonColor = DARK_CHECKED_BUTTON_BACKGROUND
        )
    } else {
        NoiseControlWidgetThemeResources(
            buttonShapeStart = R.drawable.widget_button_shape_start_light,
            buttonShapeMiddle = R.drawable.widget_button_shape_middle_light,
            buttonShapeEnd = R.drawable.widget_button_shape_end_light,
            checkedButtonShapeStart = R.drawable.widget_button_checked_shape_start_light,
            checkedButtonShapeMiddle = R.drawable.widget_button_checked_shape_middle_light,
            checkedButtonShapeEnd = R.drawable.widget_button_checked_shape_end_light,
            buttonColor = LIGHT_BUTTON_BACKGROUND,
            checkedButtonColor = LIGHT_CHECKED_BUTTON_BACKGROUND
        )
    }

    setInt(
        R.id.noise_control_widget,
        "setBackgroundResource",
        if (isDarkTheme) {
            R.drawable.noise_control_widget_background
        } else {
            R.drawable.noise_control_widget_background_light
        }
    )
    setColorStateList(
        R.id.noise_control_widget,
        "setBackgroundTintList",
        colorWithOpacity(
            if (isDarkTheme) DARK_WIDGET_BACKGROUND else LIGHT_WIDGET_BACKGROUND,
            opacity
        )
    )
    intArrayOf(
        R.id.widget_off_label,
        R.id.widget_transparency_label,
        R.id.widget_adaptive_label,
        R.id.widget_anc_label
    ).forEach { viewId -> setTextColor(viewId, primaryColor) }
    intArrayOf(
        R.id.widget_off_icon,
        R.id.widget_transparency_icon,
        R.id.widget_adaptive_icon,
        R.id.widget_anc_icon
    ).forEach { viewId -> setInt(viewId, "setColorFilter", primaryColor) }
    val buttonThemes = arrayOf(
        Triple(
            R.id.widget_off_button,
            if (selectedMode == 1) {
                resources.checkedButtonShapeStart
            } else {
                resources.buttonShapeStart
            },
            selectedMode == 1
        ),
        Triple(
            R.id.widget_transparency_button,
            if (selectedMode == 3) {
                if (allowOffMode) {
                    resources.checkedButtonShapeMiddle
                } else {
                    resources.checkedButtonShapeStart
                }
            } else {
                if (allowOffMode) resources.buttonShapeMiddle else resources.buttonShapeStart
            },
            selectedMode == 3
        ),
        Triple(
            R.id.widget_adaptive_button,
            if (selectedMode == 4) {
                resources.checkedButtonShapeMiddle
            } else {
                resources.buttonShapeMiddle
            },
            selectedMode == 4
        ),
        Triple(
            R.id.widget_anc_button,
            if (selectedMode == 2) {
                resources.checkedButtonShapeEnd
            } else {
                resources.buttonShapeEnd
            },
            selectedMode == 2
        )
    )
    buttonThemes.forEach { (viewId, _, checked) ->
        // The shape comes from each layout - the wide row uses start/middle/end
        // pills, the 2x2 uses quarters of a rounded square - and selection is
        // now purely a tint, so the drawable is left as the layout declared it.
        // The tiles sit on top of an already translucent container, so giving
        // them the user's opacity too stacks two layers and makes this widget
        // read as far more opaque than the battery one at the same setting.
        // They get a thin fixed alpha instead, enough to segment the row.
        // iOS fills only the selected segment; the rest carry no fill at all, so
        // the widget composites to exactly the container's opacity the way the
        // battery widget does instead of stacking a second layer on top.
        setColorStateList(
            viewId,
            "setBackgroundTintList",
            if (checked) {
                ColorStateList.valueOf(resources.checkedButtonColor)
            } else {
                ColorStateList.valueOf(Color.TRANSPARENT)
            }
        )
    }
}
