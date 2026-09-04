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

package me.kavishdevar.librepods.presentation.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPodsModels
import me.kavishdevar.librepods.presentation.MaterialIcons
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.widgets.batteryWidgetIcons
import me.kavishdevar.librepods.presentation.theme.LocalIsDarkTheme

@Composable
internal fun AppleDisconnectedContent(
    canReconnect: Boolean,
    reconnecting: Boolean,
    showTroubleshooting: Boolean,
    onReconnect: () -> Unit,
    onTroubleshooting: () -> Unit,
    lastConnectedModelNumber: String? = null,
    modifier: Modifier = Modifier,
    messageModifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val lastConnectedModel = lastConnectedModelNumber?.let(AirPodsModels::getModelByModelNumber)
    val deviceIcon = if (lastConnectedModel != null) {
        painterResource(batteryWidgetIcons(lastConnectedModel).buds)
    } else {
        rememberVectorPainter(MaterialIcons.bluetooth)
    }
    // A darker blue keeps white labels readable on the filled capsule.
    val buttonBlue = Color(0xFF0066DD)
    val linkBlue = if (LocalIsDarkTheme.current) MaterialTheme.colorScheme.primary else buttonBlue
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = messageModifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = deviceIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.airpods_not_connected),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.airpods_not_connected_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }

        if (canReconnect) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onReconnect()
                },
                enabled = !reconnecting,
                modifier = Modifier
                    .width(220.dp)
                    .heightIn(min = 52.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBlue,
                    contentColor = Color.White,
                    disabledContainerColor = buttonBlue,
                    disabledContentColor = Color.White,
                ),
                elevation = null,
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f)),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            ) {
                if (reconnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp).clearAndSetSemantics {},
                        color = Color.White,
                        strokeWidth = 2.dp,
                        trackColor = Color.Transparent,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(
                        if (reconnecting) R.string.reconnecting else R.string.reconnect_to_last_device
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (showTroubleshooting) {
            Spacer(Modifier.height(if (canReconnect) 8.dp else 20.dp))
            TextButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onTroubleshooting()
                },
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = linkBlue),
            ) {
                Text(
                    text = stringResource(R.string.troubleshooting),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun AppleDisconnectedSettingsButton(backdrop: LayerBackdrop, onClick: () -> Unit) {
    StyledIconButton(
        icon = R.drawable.sf_gear,
        contentDescription = stringResource(R.string.settings),
        backdrop = backdrop,
        onClick = onClick,
    )
}

@Preview(name = "Apple disconnected", locale = "ko", widthDp = 400, heightDp = 700)
@Preview(name = "Apple disconnected dark", locale = "ko", widthDp = 400, heightDp = 700, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Apple disconnected large text", locale = "ko", widthDp = 320, heightDp = 480, fontScale = 2f)
@Composable
private fun AppleDisconnectedPreview() {
    AppleDisconnectedPreviewContent(reconnecting = false)
}

@Preview(name = "Apple reconnecting", locale = "ko", widthDp = 400, heightDp = 700)
@Composable
private fun AppleReconnectingPreview() {
    AppleDisconnectedPreviewContent(reconnecting = true)
}

@Composable
private fun AppleDisconnectedPreviewContent(reconnecting: Boolean) {
    LibrePodsTheme(m3eEnabled = false) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppleDisconnectedContent(
                    canReconnect = true,
                    reconnecting = reconnecting,
                    showTroubleshooting = true,
                    onReconnect = {},
                    onTroubleshooting = {},
                )
            }
        }
    }
}
