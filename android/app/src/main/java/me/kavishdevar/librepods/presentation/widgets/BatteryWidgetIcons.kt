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

import androidx.annotation.DrawableRes
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPods
import me.kavishdevar.librepods.data.AirPods2
import me.kavishdevar.librepods.data.AirPods3
import me.kavishdevar.librepods.data.AirPods4
import me.kavishdevar.librepods.data.AirPods4ANC
import me.kavishdevar.librepods.data.AirPodsBase
import me.kavishdevar.librepods.data.AirPodsPro1
import me.kavishdevar.librepods.data.AirPodsPro2Lightning
import me.kavishdevar.librepods.data.AirPodsPro2USBC
import me.kavishdevar.librepods.data.AirPodsPro3

internal data class BatteryWidgetIcons(
    @param:DrawableRes val buds: Int,
    @param:DrawableRes val leftBud: Int,
    @param:DrawableRes val rightBud: Int,
    @param:DrawableRes val chargingCase: Int
) {
    @DrawableRes
    fun forDevice(device: BatteryWidgetDevice): Int = when (device) {
        BatteryWidgetDevice.PHONE -> R.drawable.sf_iphone_gen3
        BatteryWidgetDevice.BUDS -> buds
        BatteryWidgetDevice.LEFT_BUD -> leftBud
        BatteryWidgetDevice.RIGHT_BUD -> rightBud
        BatteryWidgetDevice.CASE -> chargingCase
        BatteryWidgetDevice.EMPTY -> buds
    }
}

internal fun batteryWidgetIcons(model: AirPodsBase?): BatteryWidgetIcons = when (model) {
    is AirPods, is AirPods2 -> BatteryWidgetIcons(
        R.drawable.sf_airpods,
        R.drawable.sf_airpod_left,
        R.drawable.sf_airpod_right,
        R.drawable.sf_airpods_chargingcase_wireless_fill
    )
    is AirPods3 -> BatteryWidgetIcons(
        R.drawable.sf_airpods_gen3,
        R.drawable.sf_airpod_gen3_left,
        R.drawable.sf_airpod_gen3_right,
        R.drawable.sf_airpods_gen3_chargingcase_wireless_fill
    )
    is AirPods4, is AirPods4ANC -> BatteryWidgetIcons(
        R.drawable.sf_airpods_gen4,
        R.drawable.sf_airpods_gen4_left,
        R.drawable.sf_airpods_gen4_right,
        R.drawable.sf_airpods_gen4_chargingcase_wireless_fill
    )
    is AirPodsPro1 -> BatteryWidgetIcons(
        R.drawable.sf_airpods_pro_gen1,
        R.drawable.sf_airpods_pro_gen1_left,
        R.drawable.sf_airpods_pro_gen1_right,
        R.drawable.sf_airpods_pro_gen1_chargingcase_wireless_fill
    )
    is AirPodsPro3 -> BatteryWidgetIcons(
        R.drawable.sf_airpods_pro_gen3,
        R.drawable.sf_airpods_pro_gen3_left,
        R.drawable.sf_airpods_pro_gen3_right,
        R.drawable.sf_airpods_pro_gen3_chargingcase_wireless_fill
    )
    is AirPodsPro2Lightning, is AirPodsPro2USBC, null -> BatteryWidgetIcons(
        R.drawable.sf_airpods_pro,
        R.drawable.sf_airpods_pro_left,
        R.drawable.sf_airpods_pro_right,
        R.drawable.sf_airpods_pro_chargingcase_wireless_fill
    )
    else -> BatteryWidgetIcons(
        R.drawable.sf_airpods_pro,
        R.drawable.sf_airpods_pro_left,
        R.drawable.sf_airpods_pro_right,
        R.drawable.sf_airpods_pro_chargingcase_wireless_fill
    )
}
