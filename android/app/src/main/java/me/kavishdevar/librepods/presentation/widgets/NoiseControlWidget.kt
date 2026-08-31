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
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.services.ServiceManager
import kotlin.io.encoding.ExperimentalEncodingApi

class NoiseControlWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        ServiceManager.getService()?.let { service ->
            service.updateNoiseControlWidget(appWidgetIds)
            return
        }

        appWidgetIds.forEach { appWidgetId ->
            val isDarkTheme = WidgetThemePreferences.isDark(context, appWidgetId)
            val opacity = WidgetThemePreferences.getOpacity(context, appWidgetId)
            appWidgetManager.updateAppWidget(
                appWidgetId,
                RemoteViews(
                    mapOf(
                        SizeF(180f, 40f) to populateNoiseControlWidgetFallback(
                            context,
                            R.layout.noise_control_widget,
                            isDarkTheme,
                            opacity,
                            NoiseControlWidget::class.java
                        ),
                        SizeF(250f, 40f) to populateNoiseControlWidgetFallback(
                            context,
                            R.layout.noise_control_widget_wide,
                            isDarkTheme,
                            opacity,
                            NoiseControlWidget::class.java
                        )
                    )
                )
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        handleNoiseControlWidgetIntent(context, intent, "NoiseControlWidget")
    }
}

internal fun populateNoiseControlWidgetFallback(
    context: Context,
    layoutId: Int,
    isDarkTheme: Boolean,
    opacity: Int,
    providerClass: Class<out AppWidgetProvider>
): RemoteViews {
    return RemoteViews(context.packageName, layoutId).also { views ->
        views.applyNoiseControlWidgetTheme(isDarkTheme, opacity)
        intArrayOf(1, 3, 4, 2).zip(
            intArrayOf(
                R.id.widget_off_button,
                R.id.widget_transparency_button,
                R.id.widget_adaptive_button,
                R.id.widget_anc_button
            )
        ).forEachIndexed { requestCode, (mode, viewId) ->
            val intent = Intent(context, providerClass).apply {
                action = "ACTION_SET_ANC_MODE"
                putExtra("ANC_MODE", mode)
            }
            views.setOnClickPendingIntent(
                viewId,
                PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
    }
}

internal fun handleNoiseControlWidgetIntent(context: Context, intent: Intent, logTag: String) {
    if (intent.action != "ACTION_SET_ANC_MODE") return

    val mode = intent.getIntExtra("ANC_MODE", 1)
    Log.d(logTag, "Setting ANC mode to $mode")
    val service = ServiceManager.getService()
    if (service == null) {
        Log.w(logTag, "Service unavailable")
        return
    }

    service.aacpManager.sendControlCommand(
        AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value,
        mode.toByte()
    )
}
