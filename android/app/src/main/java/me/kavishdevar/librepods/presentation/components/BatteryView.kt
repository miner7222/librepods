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

package me.kavishdevar.librepods.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.OverlayRingLayout
import me.kavishdevar.librepods.data.BatteryStatus
import kotlin.io.encoding.ExperimentalEncodingApi
import me.kavishdevar.librepods.presentation.theme.LocalIsDarkTheme

/** Stills are two transparent layers on the shared 1050 × 354 artwork canvas. */
@Composable
fun BatteryView(
    batteryList: List<Battery>,
    budsRes: Int,
    caseRes: Int,
    ringLayout: OverlayRingLayout = OverlayRingLayout()
) {
    val left = batteryList.find { it.component == BatteryComponent.LEFT }
    val right = batteryList.find { it.component == BatteryComponent.RIGHT }
    val case = batteryList.find { it.component == BatteryComponent.CASE }
    val leftLevel = left?.level ?: 0
    val rightLevel = right?.level ?: 0
    val caseLevel = case?.level ?: 0
    val combined = left?.status == right?.status && (leftLevel - rightLevel) in -3..3

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 353.dp).fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().aspectRatio(1050f / 354f)) {
                Image(
                    bitmap = ImageBitmap.imageResource(budsRes),
                    contentDescription = stringResource(R.string.buds),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize()
                )
                Image(
                    bitmap = ImageBitmap.imageResource(caseRes),
                    contentDescription = stringResource(R.string.case_alt),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize()
                )
            }
            BoxWithConstraints(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                contentAlignment = AbsoluteAlignment.TopLeft
            ) {
                // Positions are physical image coordinates; keep them in LTR
                // even when surrounding labels use a right-to-left language.
                val canvasWidth = maxWidth
                @Composable
                fun At(center: Float, content: @Composable () -> Unit) {
                    Box(
                        Modifier.absoluteOffset(x = canvasWidth * center - 36.dp).width(72.dp),
                        contentAlignment = Alignment.TopCenter
                    ) { content() }
                }
                if (combined) {
                    At(ringLayout.budPair) {
                        BatteryIndicator(leftLevel.coerceAtMost(rightLevel), left?.status ?: BatteryStatus.NOT_CHARGING)
                    }
                } else {
                    if (leftLevel > 0 || left?.status != BatteryStatus.DISCONNECTED) {
                        At(ringLayout.leftBud) {
                            BatteryIndicator(leftLevel, left?.status ?: BatteryStatus.NOT_CHARGING, R.drawable.sf_l_circle_fill)
                        }
                    }
                    if (rightLevel > 0 || right?.status != BatteryStatus.DISCONNECTED) {
                        At(ringLayout.rightBud) {
                            BatteryIndicator(rightLevel, right?.status ?: BatteryStatus.NOT_CHARGING, R.drawable.sf_r_circle_fill)
                        }
                    }
                }
                if (caseLevel > 0 || case?.status != BatteryStatus.DISCONNECTED) {
                    At(ringLayout.chargingCase) {
                        BatteryIndicator(caseLevel, case?.status ?: BatteryStatus.NOT_CHARGING,
                            prefix = if (!combined) R.drawable.sf_chargingcase_wireless_fill else 0)
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BatteryViewPreview() {
    val fakeBattery = listOf(
        Battery(BatteryComponent.LEFT, 85, BatteryStatus.CHARGING),
        Battery(BatteryComponent.RIGHT, 40, BatteryStatus.OPTIMIZED_CHARGING),
        Battery(BatteryComponent.CASE, 60, BatteryStatus.NOT_CHARGING)
    )

    val bg = if (LocalIsDarkTheme.current) Color.Black else Color(0xFFF2F2F7)

    Box(
        modifier = Modifier
            .background(bg)
            .padding(16.dp)
    ) {
        BatteryView(
            batteryList = fakeBattery,
            budsRes = R.drawable.airpods_pro_2_buds,
            caseRes = R.drawable.airpods_pro_2_case
        )
    }
}
