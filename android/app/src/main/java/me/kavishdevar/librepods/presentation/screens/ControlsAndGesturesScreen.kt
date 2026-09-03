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

import android.content.Context.MODE_PRIVATE
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.components.CallControlSettings
import me.kavishdevar.librepods.presentation.components.PressAndHoldSettings
import me.kavishdevar.librepods.presentation.components.ReportStyledScaffoldScrollState
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun ControlsAndGesturesScreen(
    viewModel: AirPodsViewModel,
    navigateToLeftLongPress: () -> Unit,
    navigateToRightLongPress: () -> Unit,
    navigateToCallControlScreen: (action: String) -> Unit,
    navigateToHeadTracking: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val capabilities = state.capabilities
    val sharedPreferences = LocalContext.current.getSharedPreferences("settings", MODE_PRIVATE)
    val scrollState = rememberScrollState()
    ReportStyledScaffoldScrollState(scrollState, onScrollStateChanged)
    val backdrop = rememberLayerBackdrop()

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val appleMetrics = LocalAppleDesignMetrics.current
    val topPadding = if (m3eEnabled) 0.dp else
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
            appleMetrics.navigationBarHeight
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

        if (capabilities.contains(Capability.STEM_CONFIG)) {
            PressAndHoldSettings(
                leftAction = state.leftAction,
                rightAction = state.rightAction,
                navigateToLeftLongPress = navigateToLeftLongPress,
                navigateToRightLongPress = navigateToRightLongPress,
                firstInColumn = true
            )
            Spacer(modifier = Modifier.height(if (m3eEnabled) 16.dp else 0.dp))
        }

        val bytes =
            state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CALL_MANAGEMENT_CONFIG]?.take(
                2
            )?.toByteArray() ?: byteArrayOf(0x00, 0x00)
        val flipped = try {
            bytes[1] == 0x02.toByte()
        } catch (_: Exception) {
            false
        }
        CallControlSettings(
            flipped = flipped,
            navigateToCallControlScreen = navigateToCallControlScreen,
            firstInColumn = !capabilities.contains(Capability.STEM_CONFIG)
        )

        if (capabilities.contains(Capability.HEAD_GESTURES)) {
            Spacer(modifier = Modifier.height(if (m3eEnabled) 16.dp else 0.dp))
            StyledListItem(
                name = stringResource(R.string.head_gestures),
                description = if (sharedPreferences.getBoolean(
                        "head_gestures", false
                    )
                ) stringResource(R.string.on) else stringResource(R.string.off),
                onClick = navigateToHeadTracking
            )
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}
