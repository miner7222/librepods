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

package me.kavishdevar.librepods.presentation.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.components.ReportStyledScaffoldScrollState
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun BatterySettingsScreen(
    viewModel: AirPodsViewModel,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    ReportStyledScaffoldScrollState(scrollState, onScrollStateChanged)
    val backdrop = rememberLayerBackdrop()

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val appleMetrics = LocalAppleDesignMetrics.current
    val topPadding =
        if (m3eEnabled) 0.dp else WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp
    val bottomPadding =
        if (m3eEnabled) 0.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp

    Column(
        modifier = Modifier
            .layerBackdrop(backdrop)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(scrollState)
            .padding(horizontal = appleMetrics.cardHorizontalInset)
    ) {
        Spacer(modifier = Modifier.height(topPadding))

        Text(
            text = stringResource(R.string.battery_intro),
            style = appleMetrics.sectionFooterStyle,
            color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        StyledToggle(
            label = stringResource(R.string.charge_notifications),
            description = stringResource(R.string.charge_notifications_description),
            checked = state.chargeNotifications,
            onCheckedChange = viewModel::setChargeNotifications
        )

        if (state.capabilities.contains(Capability.OPTIMIZED_CHARGE_LIMIT)) {
            Spacer(modifier = Modifier.height(16.dp))
            StyledToggle(
                label = stringResource(R.string.optimized_charging),
                description = stringResource(R.string.optimized_charging_description),
                checked = state.dynamicEndOfCharge,
                onCheckedChange = viewModel::setDynamicEndOfCharge
            )
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}
