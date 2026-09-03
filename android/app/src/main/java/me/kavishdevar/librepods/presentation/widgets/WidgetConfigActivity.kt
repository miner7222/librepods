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
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.MaterialButtonStyle
import me.kavishdevar.librepods.presentation.components.StyledButton
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.components.StyledSlider
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import kotlin.math.roundToInt

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        val preferences = getSharedPreferences(WidgetThemePreferences.SETTINGS_NAME, MODE_PRIVATE)
        val isBatteryWidget = isBatteryWidget()

        setContent {
            LibrePodsTheme(
                m3eEnabled = preferences.getBoolean("m3e_enabled", true)
            ) {
                WidgetConfigScreen(
                    selectedTheme = WidgetThemePreferences.get(preferences, appWidgetId),
                    initialOpacity = WidgetThemePreferences.getOpacity(this, appWidgetId),
                    isBatteryWidget = isBatteryWidget,
                    initialShowPhoneBattery =
                        WidgetThemePreferences.getShowPhoneBattery(preferences, appWidgetId),
                    initialRememberBattery =
                        preferences.getBoolean("remember_battery_when_disconnected", false),
                    onDone = { theme, opacity, showPhoneBattery, rememberBattery ->
                        val editor = preferences.edit()
                            .putString(
                                WidgetThemePreferences.preferenceKey(appWidgetId),
                                theme.preferenceValue
                            )
                            .putInt(
                                WidgetThemePreferences.opacityPreferenceKey(appWidgetId),
                                opacity.coerceIn(0, 100)
                            )
                        if (isBatteryWidget) {
                            editor.putBoolean(
                                WidgetThemePreferences.phoneBatteryPreferenceKey(appWidgetId),
                                showPhoneBattery
                            )
                            editor.putBoolean(
                                "remember_battery_when_disconnected",
                                rememberBattery
                            )
                        }
                        editor.apply()
                        refreshWidget()
                        finishSuccessfully()
                    },
                )
            }
        }
    }

    /** These two settings only mean anything to the battery widgets. */
    private fun isBatteryWidget(): Boolean {
        val provider = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider
            ?.className
            ?: return false
        return provider == BatteryWidget::class.java.name ||
            provider == BatteryGridWidget::class.java.name
    }

    private fun refreshWidget() {
        val provider = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider
            ?: return
        sendBroadcast(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setComponent(provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        )
    }

    private fun finishSuccessfully() {
        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )
        finish()
    }
}

@Composable
private fun WidgetConfigScreen(
    selectedTheme: WidgetTheme,
    initialOpacity: Int,
    isBatteryWidget: Boolean,
    initialShowPhoneBattery: Boolean,
    initialRememberBattery: Boolean,
    onDone: (WidgetTheme, Int, Boolean, Boolean) -> Unit
) {
    var theme by rememberSaveable { mutableStateOf(selectedTheme) }
    var opacity by rememberSaveable { mutableFloatStateOf(initialOpacity.toFloat()) }
    var showPhoneBattery by rememberSaveable { mutableStateOf(initialShowPhoneBattery) }
    var rememberBattery by rememberSaveable { mutableStateOf(initialRememberBattery) }
    val materialDesign = LocalDesignSystem.current == DesignSystem.Material
    val topPadding = if (materialDesign) {
        16.dp
    } else {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
            LocalAppleDesignMetrics.current.navigationBarHeight
    }
    val bottomPadding = if (materialDesign) {
        16.dp
    } else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp
    }

    StyledScaffold(
        title = stringResource(R.string.widget_appearance),
        actionButtons = listOf { backdrop ->
            StyledButton(
                onClick = {
                    onDone(
                        theme,
                        opacity.roundToInt(),
                        showPhoneBattery,
                        rememberBattery
                    )
                },
                backdrop = backdrop,
                materialButtonStyle = MaterialButtonStyle.Normal
            ) {
                Text(
                    text = stringResource(R.string.widget_done),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalAppleDesignMetrics.current.cardHorizontalInset)
        ) {
            Spacer(modifier = Modifier.height(topPadding))
            StyledList(firstInColumn = true) {
                StyledListItem(
                    name = stringResource(R.string.widget_theme_system),
                    selected = theme == WidgetTheme.SYSTEM,
                    onClick = {
                        theme = WidgetTheme.SYSTEM
                    }
                )
                StyledListItem(
                    name = stringResource(R.string.widget_theme_light),
                    selected = theme == WidgetTheme.LIGHT,
                    onClick = {
                        theme = WidgetTheme.LIGHT
                    }
                )
                StyledListItem(
                    name = stringResource(R.string.widget_theme_dark),
                    selected = theme == WidgetTheme.DARK,
                    onClick = {
                        theme = WidgetTheme.DARK
                    }
                )
            }
            if (isBatteryWidget) {
                Spacer(modifier = Modifier.height(if (materialDesign) 16.dp else 0.dp))
                StyledToggle(
                    label = stringResource(R.string.show_phone_battery_in_widget),
                    description = stringResource(R.string.show_phone_battery_in_widget_description),
                    checked = showPhoneBattery,
                    onCheckedChange = {
                        showPhoneBattery = it
                    }
                )
                StyledToggle(
                    label = stringResource(R.string.remember_battery_when_disconnected),
                    description = stringResource(R.string.remember_battery_when_disconnected_description),
                    checked = rememberBattery,
                    onCheckedChange = {
                        rememberBattery = it
                    }
                )
            }
            Spacer(modifier = Modifier.height(if (materialDesign) 16.dp else 0.dp))
            StyledSlider(
                label = stringResource(R.string.widget_background_opacity, opacity.roundToInt()),
                value = opacity,
                onValueChange = {
                    opacity = it
                },
                valueRange = 0f..100f,
                independent = true
            )
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}
