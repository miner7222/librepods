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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.StemPressType
import me.kavishdevar.librepods.data.StemAction
import me.kavishdevar.librepods.presentation.components.ReportStyledScaffoldScrollState
import me.kavishdevar.librepods.presentation.components.StyledButton
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.stemActionLabel
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.screenBottomPadding
import me.kavishdevar.librepods.presentation.theme.screenTopPadding
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

/**
 * Picks what a double or triple press on one AirPod does. [press] is a [StemPressType]
 * name and [bud] is "Left" or "Right", as the long press screen is opened.
 */
@Composable
fun StemPressScreen(
    viewModel: AirPodsViewModel,
    press: String,
    bud: String,
    navigateToPurchase: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val prefKey = StemPressType.entries.find { it.name == press }?.let {
        StemAction.multiPressPrefKey(it, left = bud.equals("left", ignoreCase = true))
    }
    val selected = prefKey?.let {
        state.pressActions[it] ?: StemAction.multiPressFallbacks.getValue(it)
    }

    val topPadding = screenTopPadding(materialInset = 8.dp)
    val bottomPadding = screenBottomPadding()

    val scrollState = rememberScrollState()
    ReportStyledScaffoldScrollState(scrollState, onScrollStateChanged)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(scrollState)
            .padding(horizontal = LocalAppleDesignMetrics.current.cardHorizontalInset)
    ) {
        Spacer(modifier = Modifier.height(topPadding))

        if (prefKey != null) {
            StyledList(
                firstInColumn = true,
                description = stringResource(R.string.stem_press_listening_mode_description)
            ) {
                StemAction.multiPressChoices.forEach { choice ->
                    StyledListItem(
                        name = stemActionLabel(choice),
                        selected = selected == choice,
                        onClick = { viewModel.setPressAction(prefKey, choice) },
                        // The assistant is a premium action wherever it is picked.
                        enabled = choice != StemAction.DIGITAL_ASSISTANT || state.isPremium
                    )
                }
            }

            if (!state.isPremium) {
                Spacer(modifier = Modifier.height(24.dp))
                StyledButton(
                    onClick = navigateToPurchase,
                    backdrop = rememberLayerBackdrop(),
                    modifier = Modifier.fillMaxWidth(),
                    maxScale = 0.05f,
                    surfaceColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        stringResource(R.string.unlock_advanced_features),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}
