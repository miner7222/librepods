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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.ATTHandles
import me.kavishdevar.librepods.data.AirPodsPro3
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.components.AudioSettings
import me.kavishdevar.librepods.presentation.components.ConnectionSettings
import me.kavishdevar.librepods.presentation.components.ReportStyledScaffoldScrollState
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun AudioAndRoutingScreen(
    viewModel: AirPodsViewModel,
    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit,
    navigateToMicrophoneSettings: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val capabilities = state.capabilities
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

        val model = state.instance?.model ?: AirPodsPro3()
        val adaptiveVolumeCapability =
            model.capabilities.contains(Capability.ADAPTIVE_VOLUME)
        val conversationalAwarenessCapability =
            model.capabilities.contains(Capability.CONVERSATION_AWARENESS)
        val loudSoundReductionCapability =
            model.capabilities.contains(Capability.LOUD_SOUND_REDUCTION)
        val adaptiveAudioCapability =
            model.capabilities.contains(Capability.ADAPTIVE_AUDIO)

        val adaptiveVolumeChecked =
            state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG]?.getOrNull(
                0
            ) == 0x01.toByte()
        val conversationalAwarenessChecked =
            state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG]?.getOrNull(
                0
            ) == 0x01.toByte()

        AudioSettings(
            adaptiveVolumeCapability = adaptiveVolumeCapability,
            conversationalAwarenessCapability = conversationalAwarenessCapability,
            loudSoundReductionCapability = loudSoundReductionCapability,
            adaptiveAudioCapability = adaptiveAudioCapability,
            customEqCapability = true,
            adaptiveVolumeChecked = adaptiveVolumeChecked,
            onAdaptiveVolumeCheckedChange = { checked ->
                viewModel.setControlCommandBoolean(
                    AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG,
                    checked
                )
            },
            conversationalAwarenessChecked = conversationalAwarenessChecked && state.isPremium,
            onConversationalAwarenessCheckedChange = { checked ->
                viewModel.setControlCommandBoolean(
                    AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG,
                    checked
                )
            },
            loudSoundReductionChecked = state.loudSoundReductionEnabled,
            onLoudSoundReductionCheckedChange = { checked ->
                viewModel.setATTCharacteristicValue(
                    ATTHandles.LOUD_SOUND_REDUCTION,
                    byteArrayOf(if (checked) 0x01.toByte() else 0x00.toByte())
                )
            },
            navigateToAdaptiveStrength = navigateToAdaptiveStrength,
            navigateToEqualizer = navigateToEqualizer,
            vendorIdHook = state.vendorIdHook,
            isPremium = state.isPremium
        )

        Spacer(modifier = Modifier.height(16.dp))
        ConnectionSettings(
            automaticEarDetectionEnabled = state.automaticEarDetectionEnabled,
            onAutomaticEarDetectionChanged = viewModel::setAutomaticEarDetectionEnabled,
            automaticConnectionEnabled = state.automaticConnectionEnabled,
            onAutomaticConnectionChanged = viewModel::setAutomaticConnectionEnabled
        )

        Spacer(modifier = Modifier.height(16.dp))
        val microphoneModeId = AACPManager.Companion.ControlCommandIdentifiers.MIC_MODE
        val selectedModeText =
            when (state.controlStates[microphoneModeId]?.getOrNull(0) ?: 0x00.toByte()) {
                0x00.toByte() -> stringResource(R.string.microphone_automatic)
                0x01.toByte() -> stringResource(R.string.microphone_always_right)
                0x02.toByte() -> stringResource(R.string.microphone_always_left)
                else -> stringResource(R.string.microphone_automatic)
            }
        StyledListItem(
            name = stringResource(R.string.microphone),
            description = selectedModeText,
            onClick = navigateToMicrophoneSettings
        )

        if (capabilities.contains(Capability.SLEEP_DETECTION)) {
            Spacer(modifier = Modifier.height(16.dp))
            val id = AACPManager.Companion.ControlCommandIdentifiers.SLEEP_DETECTION_CONFIG
            StyledToggle(
                label = stringResource(R.string.sleep_detection),
                checked = state.controlStates[id]?.getOrNull(0) == 0x01.toByte(),
                onCheckedChange = { viewModel.setControlCommandBoolean(id, it) },
                enabled = state.isPremium
            )
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}
