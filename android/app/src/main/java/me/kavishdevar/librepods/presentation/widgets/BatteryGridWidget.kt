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

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.material3.ExperimentalMaterial3Api
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.services.ServiceManager
import kotlin.io.encoding.ExperimentalEncodingApi

class BatteryGridWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        ServiceManager.getService()?.let { service ->
            service.updateBatteryGridWidget(appWidgetIds)
            return
        }

        appWidgetIds.forEach { appWidgetId ->
            val isDarkTheme = WidgetThemePreferences.isDark(context, appWidgetId)
            val opacity = WidgetThemePreferences.getOpacity(context, appWidgetId)
            appWidgetManager.updateAppWidget(
                appWidgetId,
                populateFallback(context, isDarkTheme, opacity)
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun populateFallback(
        context: Context,
        isDarkTheme: Boolean,
        opacity: Int
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.battery_widget_grid).also { views ->
            views.applyBatteryWidgetTheme(isDarkTheme, opacity)
            val openActivityIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.battery_widget, openActivityIntent)
        }
    }
}
