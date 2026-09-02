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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.services.ServiceManager

class NoiseControlGridWidget : AppWidgetProvider() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetThemePreferences.remove(context, appWidgetIds)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        ServiceManager.getService()?.let { service ->
            service.updateNoiseControlGridWidget(appWidgetIds)
            return
        }

        appWidgetIds.forEach { appWidgetId ->
            val isDarkTheme = WidgetThemePreferences.isDark(context, appWidgetId)
            val opacity = WidgetThemePreferences.getOpacity(context, appWidgetId)
            appWidgetManager.updateAppWidget(
                appWidgetId,
                appWidgetManager.sizedRemoteViewsFor(context, appWidgetId, 110, 110) { dimensions ->
                    populateNoiseControlWidgetFallback(
                        context,
                        R.layout.noise_control_widget_grid,
                        isDarkTheme,
                        opacity,
                        NoiseControlGridWidget::class.java,
                        gridItemSize(dimensions)
                    )
                }
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        handleNoiseControlWidgetIntent(context, intent, "NoiseControlGridWidget")
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }
}
