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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.ReportStyledScaffoldScrollState
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun ConnectToThisDeviceRoute(
    viewModel: AirPodsViewModel,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val topPadding = if (m3eEnabled) 0.dp else WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
        LocalAppleDesignMetrics.current.navigationBarHeight
    val bottomPadding = if (m3eEnabled) 0.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        ConnectToThisDeviceScreen(
            automatic = state.automaticConnectionEnabled,
            topPadding = topPadding,
            bottomPadding = bottomPadding,
            // The view model turns the request down and says why when the vendor ID
            // hook is off, so a refused tap simply leaves the tick where it was.
            onAutomaticChanged = viewModel::setAutomaticConnectionEnabled,
            onScrollStateChanged = onScrollStateChanged
        )
    }
}

@Composable
fun ConnectToThisDeviceScreen(
    automatic: Boolean,
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp,
    onAutomaticChanged: (Boolean) -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val scrollState = rememberScrollState()
    ReportStyledScaffoldScrollState(scrollState, onScrollStateChanged)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(scrollState)
            .padding(top = if (m3eEnabled) 8.dp else 0.dp)
            .padding(horizontal = LocalAppleDesignMetrics.current.cardHorizontalInset)
    ) {
        Spacer(modifier = Modifier.height(topPadding))

        StyledList(
            firstInColumn = true,
            description = stringResource(R.string.connect_to_this_device_description)
        ) {
            StyledListItem(
                name = stringResource(R.string.connect_automatically),
                selected = automatic,
                onClick = { onAutomaticChanged(true) }
            )

            StyledListItem(
                name = stringResource(R.string.connect_when_last_connected),
                selected = !automatic,
                onClick = { onAutomaticChanged(false) }
            )
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}
